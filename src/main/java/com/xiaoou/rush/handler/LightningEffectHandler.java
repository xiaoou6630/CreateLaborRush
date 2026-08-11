package com.xiaoou.rush.handler;

import com.xiaoou.rush.util.ParticleBudget;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Random;

public class LightningEffectHandler {

    private static final Random RANDOM = new Random();

    /**
     * 触发引雷特效序列（真实闪电 + 粒子组合）
     * 特效序列：第0/2/4/6/8 tick 分别触发不同效果
     * 防滥用：预算不足/同一目标 1 秒内已劈过时静默跳过（防连点粒子风暴）
     */
    public static void triggerLightningEffect(Level level, LivingEntity target) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        long tick = serverLevel.getServer().getTickCount();

        // 全服同 tick 序列上限（防多玩家同 tick 风暴）
        if (!ParticleBudget.tryTrigger(tick)) return;
        // 同一目标冷却（防连点反复劈同一目标）
        if (!ParticleBudget.canTriggerTarget(tick, target.getUUID())) return;
        // 真实闪电实体预算（实体堆积是主要性能杀手）
        boolean hasBolt = ParticleBudget.tryBolt(tick);

        // ===== 真实闪电劈下（纯视觉，不造成伤害） =====
        if (hasBolt) {
            net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true);
                serverLevel.addFreshEntity(bolt);
            }
        }

        // ===== 第0 tick：蓝色光柱 =====
        spawnBlueLightColumn(serverLevel, target);

        // ===== 第2 tick：闪电炸裂 =====
        serverLevel.getServer().tell(new TickTask(
            serverLevel.getServer().getTickCount() + 2, () -> {
                if (!target.isAlive()) return;
                spawnLightningBurst(serverLevel, target);
            }
        ));

        // ===== 第4 tick：冲击波扩散 =====
        serverLevel.getServer().tell(new TickTask(
            serverLevel.getServer().getTickCount() + 4, () -> {
                if (!target.isAlive()) return;
                spawnShockwave(serverLevel, target);
            }
        ));

        // ===== 第6 tick：电流缠绕（持续20tick） =====
        serverLevel.getServer().tell(new TickTask(
            serverLevel.getServer().getTickCount() + 6, () -> {
                if (!target.isAlive()) return;
                startCurrentArc(serverLevel, target, 20);
            }
        ));

        // ===== 第8 tick：地面余波（持续40tick） =====
        serverLevel.getServer().tell(new TickTask(
            serverLevel.getServer().getTickCount() + 8, () -> {
                if (!target.isAlive()) return;
                startGroundRipple(serverLevel, target, 40);
            }
        ));

        // ===== 工人受击特效 =====
        // 闪烁红色
        target.hurtTime = 10;
        target.hurtDuration = 10;
        // 头顶弹出"⚡"文字
        Component lightningText = Component.literal("\u26A1");
        target.setCustomName(lightningText);
        target.setCustomNameVisible(true);
        // 0.5秒后清除文字
        serverLevel.getServer().tell(new TickTask(
            serverLevel.getServer().getTickCount() + 10, () -> {
                if (target.isAlive()) {
                    target.setCustomName(null);
                    target.setCustomNameVisible(false);
                }
            }
        ));

        // ===== 音效 =====
        serverLevel.playSound(null, target.blockPosition(),
            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
        serverLevel.playSound(null, target.blockPosition(),
            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.0F, 1.0F);

        // ===== 成就：雷电法王 — 用引雷击中起义中的叛军 =====
        if (RebellionSystem.isRebelEntity(serverLevel, target)) {
            var nearest = serverLevel.getNearestPlayer(target, 32);
            if (nearest instanceof net.minecraft.server.level.ServerPlayer sp) {
                AchievementHandler.grantAchievement(sp, AchievementHandler.LIGHTNING_SUPPRESSOR);
            }
        }

        // ===== 屏幕震动（1.20.1不支持ClientboundCameraShakePacket，跳过） =====
        // 粒子特效和音效已足够
    }

    /**
     * 第0 tick：目标头顶蓝色光柱（END_ROD 垂直下落，从 y+3 到 y）
     */
    private static void spawnBlueLightColumn(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();
        for (double h = 3.0; h >= 0; h -= 0.25) {
            ParticleBudget.send(level, ParticleTypes.END_ROD,
                x, y + h, z,
                1, 0.05, 0.0, 0.05, 0.01);
        }
    }

    /**
     * 第2 tick：闪电炸裂（200个 ELECTRIC_SPARK 球形扩散）
     */
    private static void spawnLightningBurst(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double y = target.getY() + 1.0;
        double z = target.getZ();
        for (int i = 0; i < 200; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double phi = RANDOM.nextDouble() * Math.PI;
            double r = 0.5 + RANDOM.nextDouble() * 2.0;
            double dx = r * Math.sin(phi) * Math.cos(theta);
            double dy = r * Math.cos(phi);
            double dz = r * Math.sin(phi) * Math.sin(theta);
            ParticleBudget.send(level, ParticleTypes.ELECTRIC_SPARK,
                x + dx, y + dy, z + dz,
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * 第4 tick：冲击波扩散（白色电弧环，半径 0.5→1.5→3.0 逐圈淡出）
     */
    private static void spawnShockwave(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double y = target.getY() + 0.5;
        double z = target.getZ();
        double[] radii = {0.5, 1.5, 3.0};
        int[] counts = {30, 50, 80};
        for (int ring = 0; ring < radii.length; ring++) {
            double radius = radii[ring];
            int count = counts[ring];
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                double px = x + radius * Math.cos(angle);
                double pz = z + radius * Math.sin(angle);
                ParticleBudget.send(level, ParticleTypes.ELECTRIC_SPARK,
                    px, y, pz,
                    1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /**
     * 第6 tick：电流缠绕（ELECTRIC_SPARK 在目标身上跳跃，持续 1 秒）
     */
    private static void startCurrentArc(ServerLevel level, LivingEntity target, int remainingTicks) {
        if (remainingTicks <= 0 || !target.isAlive()) return;
        double x = target.getX();
        double y = target.getY() + 0.5;
        double z = target.getZ();
        for (int i = 0; i < 5; i++) {
            double dx = (RANDOM.nextDouble() - 0.5) * 1.5;
            double dz = (RANDOM.nextDouble() - 0.5) * 1.5;
            double dy = RANDOM.nextDouble() * 1.5;
            ParticleBudget.send(level, ParticleTypes.ELECTRIC_SPARK,
                x + dx, y + dy, z + dz,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        level.getServer().tell(new TickTask(
            level.getServer().getTickCount() + 1, () -> {
                startCurrentArc(level, target, remainingTicks - 1);
            }
        ));
    }

    /**
     * 第8 tick：地面余波（ELECTRIC_SPARK + FLAME 混合，在目标脚下持续 2 秒）
     */
    private static void startGroundRipple(ServerLevel level, LivingEntity target, int remainingTicks) {
        if (remainingTicks <= 0 || !target.isAlive()) return;
        double x = target.getX();
        double y = target.getY() + 0.1;
        double z = target.getZ();
        for (int i = 0; i < 3; i++) {
            double dx = (RANDOM.nextDouble() - 0.5) * 2.0;
            double dz = (RANDOM.nextDouble() - 0.5) * 2.0;
            ParticleBudget.send(level, ParticleTypes.ELECTRIC_SPARK,
                x + dx, y, z + dz,
                1, 0.0, 0.0, 0.0, 0.0);
            ParticleBudget.send(level, ParticleTypes.FLAME,
                x + dx, y, z + dz,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        level.getServer().tell(new TickTask(
            level.getServer().getTickCount() + 1, () -> {
                startGroundRipple(level, target, remainingTicks - 1);
            }
        ));
    }
}
