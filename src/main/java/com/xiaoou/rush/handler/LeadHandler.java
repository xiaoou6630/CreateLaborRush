package com.xiaoou.rush.handler;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
import com.xiaoou.rush.util.WorkerHelper;
import com.xiaoou.rush.util.WorkerTypeDetector;
import com.yyn.labor.util.WorkerUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateLaborRush.MODID)
public class LeadHandler {

    private static final int WHIP_COOLDOWN_TICKS = 100; // 5 seconds

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        var player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        CreateLaborRush.LOGGER.info("[LeadHandler] AttackEntityEvent fired, hand={}, target={}",
            stack.getItem(), event.getTarget());
        if (stack.getItem() != Items.LEAD) return;

        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living)) return;
        if (!WorkerUtil.isWorkerEntity(target) && !WorkerTypeDetector.isWorker(living)) {
            CreateLaborRush.LOGGER.info("[LeadHandler] target {} is not a worker", target);
            return;
        }
        CreateLaborRush.LOGGER.info("[LeadHandler] whipping worker {}", living.getStringUUID());

        Level level = target.level();
        if (level.isClientSide) return;

        int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
        int channelingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.CHANNELING, stack);

        // Shift + Left click: Lightning Five-Whip
        if (player.isShiftKeyDown()) {
            triggerLightningWhip(player, level, fireAspectLevel, channelingLevel);
            return;
        }

        // Normal single-target behavior
        applyWorkToWorker(living, level, fireAspectLevel, channelingLevel, player);
    }

    private static void applyWorkToWorker(LivingEntity living, Level level, int fireAspectLevel, int channelingLevel, Player player) {
        // 起义叛军（不在座位上）：只触发闪电镇压，不加工（避免给叛军重新挂 Work 效果）
        if (RebellionSystem.isRebelEntity(level, living)) {
            if (channelingLevel > 0) {
                LightningEffectHandler.triggerLightningEffect(level, living);
                if (player instanceof ServerPlayer sp) {
                    AchievementHandler.grantAchievement(sp, AchievementHandler.LIGHTNING_SUPPRESSOR);
                }
            }
            return;
        }

        int amplifier;
        int batchSize;
        boolean isSupercharged;
        SimpleParticleType particleType = null;
        int particleCount = 0;

        if (channelingLevel > 0) {
            amplifier = 2;
            batchSize = 64;
            isSupercharged = true;
            LightningEffectHandler.triggerLightningEffect(level, living);
            // 成就：天罚
            if (player instanceof ServerPlayer sp) {
                AchievementHandler.grantAchievement(sp, AchievementHandler.LIGHTNING_WHIP);
            }
        } else if (fireAspectLevel >= 2) {
            amplifier = 2;
            batchSize = 64;
            isSupercharged = true;
            particleType = ParticleTypes.SOUL_FIRE_FLAME;
            particleCount = 30;
            // 成就：地狱火
            if (player instanceof ServerPlayer sp) {
                AchievementHandler.grantAchievement(sp, AchievementHandler.FIRE_ASPECT_2);
            }
        } else if (fireAspectLevel == 1) {
            amplifier = 1;
            batchSize = 32;
            isSupercharged = true;
            particleType = ParticleTypes.FLAME;
            particleCount = 20;
            // 成就：火焰使者
            if (player instanceof ServerPlayer sp) {
                AchievementHandler.grantAchievement(sp, AchievementHandler.FIRE_ASPECT_1);
            }
        } else {
            amplifier = 0;
            batchSize = 0;
            isSupercharged = false;
        }

        living.getPersistentData().putInt("laborrush.batchSize", batchSize);
        living.getPersistentData().putBoolean("laborrush.supercharged", isSupercharged);
        living.getPersistentData().putInt("laborrush.amplifier", amplifier);

        living.addEffect(new MobEffectInstance(
            ModEffects.WORK_EFFECT.get(),
            90 * 20,
            amplifier,
            false,
            true,
            false
        ));

        // 成就：第一鞭 & 周扒皮
        if (player instanceof ServerPlayer sp) {
            var playerData = player.getPersistentData();
            // 第一次抽打（grantAchievement 内部幂等，已达成会自动跳过）
            AchievementHandler.grantAchievement(sp, AchievementHandler.FIRST_WHIP);

            // 同一工人抽打次数
            String whipKey = "laborrush.whipCount_" + living.getStringUUID();
            int whipCount = playerData.getInt(whipKey) + 1;
            playerData.putInt(whipKey, whipCount);
            if (whipCount >= 10) {
                AchievementHandler.grantAchievement(sp, AchievementHandler.WHIP_10_TIMES);
            }
        }

        if (particleType != null) {
            spawnWorkParticles(living, particleType, particleCount);
        }
    }

    private static void triggerLightningWhip(Player player, Level level, int fireAspectLevel, int channelingLevel) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Check cooldown
        var playerData = player.getPersistentData();
        long lastWhip = playerData.getLong("laborrush.lastWhipTime");
        if (level.getGameTime() - lastWhip < WHIP_COOLDOWN_TICKS) return;
        playerData.putLong("laborrush.lastWhipTime", level.getGameTime());

        // Find all workers within 5 blocks
        AABB area = new AABB(player.blockPosition()).inflate(5);
        boolean foundAny = false;
        int hitCount = 0;
        for (LivingEntity living : WorkerHelper.collectWorkers(level, area)) {
            foundAny = true;
            hitCount++;
            applyWorkToWorker(living, level, fireAspectLevel, channelingLevel, player);
            // Always spawn lightning for visual effect
            if (channelingLevel > 0 || serverLevel.random.nextFloat() < 0.3f) {
                LightningEffectHandler.triggerLightningEffect(level, living);
            }
        }

        // 起义叛军（不在座位上，座位循环扫不到）：同样吃引雷五连鞭，只劈不加工
        boolean hitRebel = false;
        for (LivingEntity rebel : RebellionSystem.getActiveRebels(level)) {
            if (!rebel.isAlive() || rebel.isPassenger()) continue;
            if (rebel.distanceToSqr(player) > 5 * 5) continue;
            foundAny = true;
            hitCount++;
            hitRebel = true;
            if (channelingLevel > 0) {
                LightningEffectHandler.triggerLightningEffect(level, rebel);
            }
        }

        // 闪电五连鞭命中起义叛军 → 直接镇压起义（无需劈死叛军）
        if (hitRebel && channelingLevel > 0 && level instanceof ServerLevel sl) {
            RebellionSystem.suppressActiveRebellion(sl);
        }

        // 成就：闪电五连鞭 — 一次击中5+工人
        if (hitCount >= 5 && player instanceof ServerPlayer sp) {
            AchievementHandler.grantAchievement(sp, AchievementHandler.LIGHTNING_5_WHIP);
        }

        if (foundAny) {
            // Play thunder sound
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
        }
    }

    private static void spawnWorkParticles(LivingEntity living, SimpleParticleType particleType, int count) {
        if (!(living.level() instanceof ServerLevel serverLevel)) return;
        var pos = living.position();
        for (int i = 0; i < count; i++) {
            double xOff = (living.getRandom().nextDouble() - 0.5) * 1.2;
            double zOff = (living.getRandom().nextDouble() - 0.5) * 1.2;
            double yOff = living.getRandom().nextDouble() * 1.5 + 0.2;
            com.xiaoou.rush.util.ParticleBudget.send(
                serverLevel,
                particleType,
                pos.x + xOff,
                pos.y + yOff,
                pos.z + zOff,
                1,
                0.0, 0.0, 0.0, 0.1
            );
        }
    }
}