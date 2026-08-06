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

    /**
     * 给范围内的工人附加 Work 效果
     * @return 受影响的工人数量
     */
    public static int applyWorkToNearbyWorkers(Level level, AABB area) {
        if (level.isClientSide) return 0;

        int count = 0;
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, area)) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living && WorkerUtil.isWorkerEntity(passenger)) {
                    living.addEffect(new MobEffectInstance(ModEffects.WORK_EFFECT, 1800, 0, false, true, false));
                    count++;
                }
            }
        }
        return count;
    }
}