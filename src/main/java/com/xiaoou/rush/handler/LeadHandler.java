package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
import com.yyn.labor.util.WorkerUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = CreateLaborRush.MODID)
public class LeadHandler {

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        var player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() != Items.LEAD) return;

        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living) || !WorkerUtil.isWorkerEntity(target))
            return;

        if (target.level().isClientSide) return;

        // ✅ 检查火焰附加附魔等级（NeoForge 1.21 正确写法）
        int fireAspectLevel = 0;
        var holder = target.registryAccess()
            .holder(Enchantments.FIRE_ASPECT);
        if (holder.isPresent()) {
            fireAspectLevel = stack.getEnchantments().getLevel(holder.get());
        }

        int batchSize = 0;
        boolean isSupercharged = false;

        if (fireAspectLevel >= 2) {
            batchSize = 64;
            isSupercharged = true;
            spawnWorkParticles(living, ParticleTypes.SOUL_FIRE_FLAME, 30);
        } else if (fireAspectLevel == 1) {
            batchSize = 32;
            isSupercharged = true;
            spawnWorkParticles(living, ParticleTypes.FLAME, 20);
        }

        living.getPersistentData().putInt("laborrush.batchSize", batchSize);
        living.getPersistentData().putBoolean("laborrush.supercharged", isSupercharged);
        living.getPersistentData().putInt("laborrush.fireAspectLevel", fireAspectLevel);

        living.addEffect(new MobEffectInstance(
            ModEffects.WORK_EFFECT,
            90 * 20,
            0,
            false,
            true,
            false
        ));
    }

    /**
     * ✅ 修复：使用 ParticleOptions 而不是 ParticleType
     */
    private static void spawnWorkParticles(LivingEntity living, net.minecraft.core.particles.SimpleParticleType particleType, int count) {
        if (!(living.level() instanceof ServerLevel serverLevel)) return;
        var pos = living.position();
        for (int i = 0; i < count; i++) {
            double xOff = (living.getRandom().nextDouble() - 0.5) * 1.2;
            double zOff = (living.getRandom().nextDouble() - 0.5) * 1.2;
            double yOff = living.getRandom().nextDouble() * 1.5 + 0.2;
            // ✅ 使用 sendParticles(ParticleOptions, ...) 正确重载
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