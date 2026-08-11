package com.xiaoou.rush.util;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.xiaoou.rush.ModEffects;
import com.xiaoou.rush.handler.RebellionSystem;
import com.yyn.labor.util.WorkerUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class WorkerHelper {

    /**
     * 收集区域内所有工人：
     * 1. 座位上的工人（村民/女仆/千年村庄等，按 create_labor 的识别）
     * 2. 站着的村民及其子类（性能升级生成的 LaborEntity 不坐座位，直接用 Villager 子类扫，
     *    不扩大对其他 mod 实体的识别）
     * 起义叛军已排除（叛军不再算作工作工人）
     */
    public static List<LivingEntity> collectWorkers(Level level, AABB area) {
        List<LivingEntity> workers = new ArrayList<>();
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, area)) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living && WorkerUtil.isWorkerEntity(passenger)) {
                    workers.add(living);
                }
            }
        }
        for (Villager villager : level.getEntitiesOfClass(Villager.class, area)) {
            if (!villager.isAlive() || villager.isPassenger()) continue;
            if (RebellionSystem.isRebelEntity(level, villager)) continue;
            workers.add(villager);
        }
        return workers;
    }

    /**
     * 给范围内的工人附加 Work 效果
     * @return 受影响的工人数量
     */
    public static int applyWorkToNearbyWorkers(Level level, AABB area) {
        if (level.isClientSide) return 0;

        int count = 0;
        for (LivingEntity living : collectWorkers(level, area)) {
            living.addEffect(new MobEffectInstance(ModEffects.WORK_EFFECT, 1800, 0, false, true, false));
            count++;
        }
        return count;
    }
}
