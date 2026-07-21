package com.xiaoou.rush.util;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.xiaoou.rush.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Utility methods for applying the WORK_EFFECT to nearby seated entities
 * (villagers, players, Touhou Little Maids).
 */
public class WorkerHelper {

    /** Cached maid class reference (lazy-loaded, optional dependency). */
    private static Class<?> maidClass;
    private static boolean maidChecked = false;

    /**
     * Apply the WORK_EFFECT to all eligible passengers on nearby SeatEntities
     * within the given AABB.
     */
    public static void applyWorkToNearbyWorkers(Level level, AABB area) {
        if (level.isClientSide) return;

        var opt = BuiltInRegistries.MOB_EFFECT.getHolder(ModEffects.WORK_EFFECT.getKey());
        if (opt.isEmpty()) return;

        var effect = new MobEffectInstance(opt.get(), 1800, 0, false, true, false);

        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, area)) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living
                    && (passenger instanceof Villager || isMaidEntity(passenger) || passenger instanceof net.minecraft.world.entity.player.Player)) {
                    living.addEffect(effect);
                }
            }
        }
    }

    /**
     * Check if an entity is a Touhou Little Maid, using reflection to avoid hard dependency.
     * Tries two known class names for different mod versions.
     */
    public static boolean isMaidEntity(Entity entity) {
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