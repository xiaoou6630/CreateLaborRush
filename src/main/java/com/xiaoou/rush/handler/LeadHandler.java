package com.xiaoou.rush.handler;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
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
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = CreateLaborRush.MODID)
public class LeadHandler {

    private static final int WHIP_COOLDOWN_TICKS = 100; // 5 seconds

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        var player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() != Items.LEAD) return;

        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living)) return;
        if (!WorkerUtil.isWorkerEntity(target) && !WorkerTypeDetector.isWorker(living))
            return;

        var level = target.level();
        if (level.isClientSide) return;

        // NeoForge 1.21.1: 使用 holder 获取附魔等级
        int fireAspectLevel = 0;
        var faHolder = target.registryAccess().holder(Enchantments.FIRE_ASPECT);
        if (faHolder.isPresent()) {
            fireAspectLevel = stack.getEnchantments().getLevel(faHolder.get());
        }

        int channelingLevel = 0;
        var chHolder = target.registryAccess().holder(Enchantments.CHANNELING);
        if (chHolder.isPresent()) {
            channelingLevel = stack.getEnchantments().getLevel(chHolder.get());
        }

        // Shift + Left click: Lightning Five-Whip
        if (player.isShiftKeyDown()) {
            triggerLightningWhip(player, level, fireAspectLevel, channelingLevel);
            return;
        }

        // Normal single-target behavior
        applyWorkToWorker(living, level, fireAspectLevel, channelingLevel, player);
    }

    private static void applyWorkToWorker(LivingEntity living, Level level, int fireAspectLevel, int channelingLevel, Player player) {
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
            ModEffects.WORK_EFFECT,
            90 * 20,
            amplifier,
            false,
            true,
            false
        ));

        // 成就：第一鞭 & 周扒皮
        if (player instanceof ServerPlayer sp) {
            // 第一次抽打
            var playerData = player.getPersistentData();
            if (!playerData.getBoolean("laborrush.hasFirstWhip")) {
                playerData.putBoolean("laborrush.hasFirstWhip", true);
                AchievementHandler.grantAchievement(sp, AchievementHandler.FIRST_WHIP);
            }

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
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, area)) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living && WorkerUtil.isWorkerEntity(passenger)) {
                    foundAny = true;
                    hitCount++;
                    applyWorkToWorker(living, level, fireAspectLevel, channelingLevel, player);
                    // Always spawn lightning for visual effect
                    if (channelingLevel > 0 || serverLevel.random.nextFloat() < 0.3f) {
                        LightningEffectHandler.triggerLightningEffect(level, living);
                    }
                }
            }
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
            serverLevel.sendParticles(
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