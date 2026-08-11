package com.xiaoou.rush.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 特效防滥用预算（粒子效果本身不变，只在超过预算时截断）：
 * - 全服每 tick 粒子总数上限（防粒子风暴刷服/卡客户端）
 * - 全服每 tick 特效序列触发上限（防多玩家同 tick 风暴）
 * - 全服每 tick 真实闪电实体上限（闪电实体是性能杀手，防实体堆积）
 * - 每个目标 20 tick 冷却（防连点反复劈同一目标）
 */
public class ParticleBudget {

    private static final int MAX_PARTICLES_PER_TICK = 500;
    private static final int MAX_TRIGGERS_PER_TICK = 12;
    private static final int MAX_BOLTS_PER_TICK = 6;
    private static final long TARGET_COOLDOWN_TICKS = 20;

    private static long budgetTick = -1;
    private static int particlesThisTick = 0;
    private static int triggersThisTick = 0;
    private static int boltsThisTick = 0;

    private static final Map<UUID, Long> LAST_TARGET_TRIGGER = new HashMap<>();

    private static void rollBudget(long tick) {
        if (tick != budgetTick) {
            budgetTick = tick;
            particlesThisTick = 0;
            triggersThisTick = 0;
            boltsThisTick = 0;
        }
    }

    public static boolean tryParticles(long tick, int count) {
        rollBudget(tick);
        if (count <= 0) return false;
        if (particlesThisTick + count > MAX_PARTICLES_PER_TICK) return false;
        particlesThisTick += count;
        return true;
    }

    public static boolean tryTrigger(long tick) {
        rollBudget(tick);
        if (triggersThisTick >= MAX_TRIGGERS_PER_TICK) return false;
        triggersThisTick++;
        return true;
    }

    public static boolean tryBolt(long tick) {
        rollBudget(tick);
        if (boltsThisTick >= MAX_BOLTS_PER_TICK) return false;
        boltsThisTick++;
        return true;
    }

    public static boolean canTriggerTarget(long tick, UUID target) {
        Long last = LAST_TARGET_TRIGGER.get(target);
        if (last != null && tick - last < TARGET_COOLDOWN_TICKS) return false;
        LAST_TARGET_TRIGGER.put(target, tick);
        if (LAST_TARGET_TRIGGER.size() > 2048) {
            LAST_TARGET_TRIGGER.entrySet().removeIf(e -> tick - e.getValue() > TARGET_COOLDOWN_TICKS * 2);
        }
        return true;
    }

    /** 带预算的粒子发送（预算不足时静默丢弃，正常玩法下预算远用不完） */
    public static void send(ServerLevel level, ParticleOptions type,
                            double x, double y, double z,
                            int count, double dx, double dy, double dz, double speed) {
        if (!tryParticles(level.getServer().getTickCount(), count)) return;
        level.sendParticles(type, x, y, z, count, dx, dy, dz, speed);
    }
}
