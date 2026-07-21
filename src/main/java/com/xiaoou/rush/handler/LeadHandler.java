package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Handles left-clicking a worker with a Lead to apply the WORK_EFFECT.
 * Supports villagers, players, and Touhou Little Maid entities (optional integration).
 */
@EventBusSubscriber(modid = CreateLaborRush.MODID)
public class LeadHandler {

    /** Cached maid class reference (lazy-loaded, optional dependency). */
    private static Class<?> maidClass;
    private static boolean maidChecked = false;

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        var player = event.getEntity();
        if (player.getMainHandItem().getItem() != Items.LEAD && player.getOffhandItem().getItem() != Items.LEAD)
            return;

        Entity target = event.getTarget();
        if (!(target instanceof Villager) && !isMaidEntity(target) && !(target instanceof net.minecraft.world.entity.player.Player))
            return;

        if (!target.level().isClientSide) {
            var opt = BuiltInRegistries.MOB_EFFECT.getHolder(ModEffects.WORK_EFFECT.getKey());
            if (opt.isPresent()) {
                ((LivingEntity) target).addEffect(new MobEffectInstance(opt.get(), 1800, 0, false, true, false));
            }
        }
    }

    /**
     * Check if an entity is a Touhou Little Maid, using reflection to avoid hard dependency.
     * Tries two known class names for different mod versions.
     */
    private static boolean isMaidEntity(Entity entity) {
        if (!maidChecked) {
            try {
                maidClass = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid");
            } catch (ClassNotFoundException e1) {
                try {
                    maidClass = Class.forName("touhou_little_maid.entity.passive.MaidEntity");
                } catch (ClassNotFoundException e2) {
                    maidClass = null;
                }
            }
            maidChecked = true;
        }
        return maidClass != null && maidClass.isInstance(entity);
    }
}
