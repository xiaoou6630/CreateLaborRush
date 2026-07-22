package com.xiaoou.rush.util;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.xiaoou.rush.ModEffects;
import com.yyn.labor.util.WorkerUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class WorkerHelper {

    public static void applyWorkToNearbyWorkers(Level level, AABB area) {
        if (level.isClientSide) return;

        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, area)) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living && WorkerUtil.isWorkerEntity(passenger)) {
                    // ✅ 直接使用 DeferredHolder
                    living.addEffect(new MobEffectInstance(ModEffects.WORK_EFFECT, 1800, 0, false, true, false));
                }
            }
        }
    }
}