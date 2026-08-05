package com.xiaoou.rush.handler;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
import com.yyn.labor.util.WorkerUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
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
        if (stack.getItem() != Items.LEAD) return;

        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living) || !WorkerUtil.isWorkerEntity(target))
            return;

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
        applyWorkToWorker(living, level, fireAspectLevel, channelingLevel);
    }

    private static void applyWorkToWorker(LivingEntity living, Level level, int fireAspectLevel, int channelingLevel) {
        int amplifier;
        int batchSize;
        boolean isSupercharged;
        SimpleParticleType particleType = null;
        int particleCount = 0;

        if (channelingLevel > 0) {
            amplifier = 2;
            batchSize = 64;
            isSupercharged = true;
            spawnLightningAt(living, level);
        } else if (fireAspectLevel >= 2) {
            amplifier = 2;
            batchSize = 64;
            isSupercharged = true;
            particleType = ParticleTypes.SOUL_FIRE_FLAME;
            particleCount = 30;
        } else if (fireAspectLevel == 1) {
            amplifier = 1;
            batchSize = 32;
            isSupercharged = true;
            particleType = ParticleTypes.FLAME;
            particleCount = 20;
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
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, area)) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living && WorkerUtil.isWorkerEntity(passenger)) {
                    foundAny = true;
                    applyWorkToWorker(living, level, fireAspectLevel, channelingLevel);
                    // Always spawn lightning for visual effect
                    if (channelingLevel > 0 || serverLevel.random.nextFloat() < 0.3f) {
                        spawnLightningAt(living, level);
                    }
                }
            }
        }

        if (foundAny) {
            // Play thunder sound
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
        }
    }

    private static void spawnLightningAt(LivingEntity living, Level level) {
        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        lightning.setPos(living.getX(), living.getY(), living.getZ());
        lightning.setVisualOnly(true);
        level.addFreshEntity(lightning);
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