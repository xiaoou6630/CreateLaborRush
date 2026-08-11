package com.xiaoou.rush.handler;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.xiaoou.rush.Config;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
import com.xiaoou.rush.util.WorkerHelper;
import com.xiaoou.rush.util.WorkerTypeDetector;
import com.yyn.labor.util.WorkerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = CreateLaborRush.MODID)
public class RebellionSystem {

    private static final Map<ResourceKey<Level>, RebellionData> REBELLIONS = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<StrikeZone>> STRIKE_ZONES = new HashMap<>();

    private static final int CONTAGION_INTERVAL_TICKS = 200;
    private static final int CONTAGION_RADIUS = 3;
    private static final double CONTAGION_CHANCE = 0.3;
    private static final int MAX_REBELS = 10;
    private static final int STRIKE_DURATION_TICKS = 12000;

    /**
     * 罢工谈判的绿宝石需求：5 + 起义人数 × 2（10人=25，2人=9），上限 32
     */
    public static int getPeaceDemand(RebellionData data) {
        return Math.min(32, 5 + data.rebels.size() * 2);
    }

    /**
     * 右键起义工人进行谈判：支付绿宝石和平结束罢工
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!Config.REBELLION_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        var data = REBELLIONS.get(level.dimension());
        if (data == null || !data.active) return;
        if (!data.rebels.contains(event.getTarget())) return;
        // 只有手持绿宝石右键才触发谈判：手持其他物品（魂符/工具等）放行给其他 mod 处理
        if (!player.getMainHandItem().is(net.minecraft.world.item.Items.EMERALD)) return;

        int demand = getPeaceDemand(data);
        var inventory = player.getInventory();
        int have = inventory.countItem(net.minecraft.world.item.Items.EMERALD);
        if (have < demand) {
            player.sendSystemMessage(Component.translatable(
                "chat.createlaborrush.peace.need", demand - have
            ));
            return;
        }
        // 从背包逐格扣除绿宝石
        int toRemove = demand;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (toRemove <= 0) break;
            var stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(net.minecraft.world.item.Items.EMERALD)) continue;
            int count = Math.min(toRemove, stack.getCount());
            stack.shrink(count);
            toRemove -= count;
            if (stack.isEmpty()) {
                inventory.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        player.sendSystemMessage(Component.translatable(
            "chat.createlaborrush.peace.success", demand
        ));
        endRebellion(serverLevel, data, false, true);
    }

    private RebellionSystem() {
    }

    private static class RebellionData {
        final ResourceKey<Level> dimension;
        final List<LivingEntity> rebels = new ArrayList<>();
        LivingEntity leader;
        ServerBossEvent bossEvent;
        int remainingTicks;
        int contagionTimer;
        int amplifier = -1;
        BlockPos origin;
        boolean active = false;
        int initialRebelCount = 0;
        int destroyedBlockCount = 0;
        // 起义开始时保存每个叛军的原 WORK_EFFECT，结束后原样恢复（不重新发放新 buff）
        final Map<UUID, MobEffectInstance> savedWorkEffects = new HashMap<>();
        // 起义开始时每个叛军骑乘的原座位，结束后自动坐回去
        final Map<UUID, Entity> savedSeats = new HashMap<>();
        // 拆家组每个叛军当前的破坏目标方块
        final Map<UUID, BlockPos> demolitionTargets = new HashMap<>();
        // 拆家进度（0.0~1.0）：按方块硬度累积（空手挖掘速度），到 1.0 才破坏
        final Map<UUID, Float> demolitionProgress = new HashMap<>();
        // 记录每个工人UUID的起义次数，用于"反抗军领袖"成就
        final Map<UUID, Integer> workerRebellionCount = new HashMap<>();

        RebellionData(ResourceKey<Level> dimension, BlockPos origin) {
            this.dimension = dimension;
            this.origin = origin;
        }
    }

    private static class StrikeZone {
        final BlockPos center;
        final int radius;
        int remainingTicks;

        StrikeZone(BlockPos center, int radius, int remainingTicks) {
            this.center = center;
            this.radius = radius;
            this.remainingTicks = remainingTicks;
        }

        boolean contains(BlockPos pos) {
            return center.distSqr(pos) <= radius * radius;
        }
    }

    private static final int REBELLION_CHECK_INTERVAL_TICKS = 200; // 每10秒自动判定一次
    private static final Map<ResourceKey<Level>, Integer> CHECK_TIMERS = new HashMap<>();

    /**
     * 周期性自动判定：工作中（有 WORK_EFFECT）的工人，每隔一段时间按等级概率起义
     */
    private static void tryStartRebellion(ServerLevel level) {
        ResourceKey<Level> dimensionKey = level.dimension();

        List<LivingEntity> workingWorkers = new ArrayList<>();
        Set<LivingEntity> seenSeats = new HashSet<>();
        int maxAmplifier = -1;
        BlockPos center = null;

        for (ServerPlayer player : level.players()) {
            AABB playerArea = player.getBoundingBox().inflate(64);
            for (LivingEntity living : WorkerHelper.collectWorkers(level, playerArea)) {
                if (!seenSeats.add(living)) continue;
                var effect = living.getEffect(ModEffects.WORK_EFFECT.get());
                if (effect == null) continue;
                int amp = effect.getAmplifier();
                if (amp > maxAmplifier) maxAmplifier = amp;
                workingWorkers.add(living);
                if (center == null) center = living.blockPosition();
            }
        }

        if (workingWorkers.isEmpty()) return;
        if (center == null) return;

        if (isInStrikeZone(level, center)) return;

        // 等级越高概率越高：0级=1x，1级=2x，2级=3x
        double chance = Math.min(1.0, Config.REBELLION_CHANCE.get() * (1 + maxAmplifier));
        double roll = level.random.nextDouble();
        CreateLaborRush.LOGGER.info("[Rebellion] auto-check: {} working workers, maxAmplifier={}, chance={}, roll={}",
            workingWorkers.size(), maxAmplifier, chance, roll);

        if (roll < chance) {
            CreateLaborRush.LOGGER.info("[Rebellion] STARTING REBELLION at {} with {} workers!",
                center, workingWorkers.size());
            AABB area = new AABB(center).inflate(Config.REBELLION_RADIUS.get());
            startRebellion(level, area, maxAmplifier);
        }
    }

    private static void startRebellion(Level level, AABB area, int amplifier) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ResourceKey<Level> dimensionKey = level.dimension();
        BlockPos center = new BlockPos(
            (int) area.getCenter().x,
            (int) area.getCenter().y,
            (int) area.getCenter().z
        );

        List<LivingEntity> candidates = WorkerHelper.collectWorkers(level, area);

        if (candidates.isEmpty()) return;

        int rebelCount = Math.min(candidates.size(), MAX_REBELS);
        java.util.Collections.shuffle(candidates);
        List<LivingEntity> selectedRebels = candidates.subList(0, rebelCount);

        RebellionData data = new RebellionData(dimensionKey, center);
        data.active = true;
        data.amplifier = amplifier;
        data.remainingTicks = Config.REBELLION_DURATION.get() * 20;
        data.contagionTimer = 0;
        data.initialRebelCount = rebelCount;
        data.rebels.addAll(selectedRebels);

        // 叛军跳下座位（记录原座位，罢工结束自动坐回去）
        for (LivingEntity rebel : selectedRebels) {
            Entity vehicle = rebel.getVehicle();
            if (vehicle != null) {
                data.savedSeats.put(rebel.getUUID(), vehicle);
            }
            rebel.stopRiding();
        }

        data.leader = selectedRebels.get(level.random.nextInt(selectedRebels.size()));

        Component bossName = Component.translatable(
            "ui.createlaborrush.rebellion.boss",
            data.leader.getDisplayName()
        );
        data.bossEvent = new ServerBossEvent(
            bossName,
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
        );
        data.bossEvent.setProgress(1.0f);

        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level().dimension() == dimensionKey) {
                data.bossEvent.addPlayer(player);
            }
        }

        REBELLIONS.put(dimensionKey, data);

        applyRebellionEffects(serverLevel, data);
        playRebellionEffects(serverLevel, center);

        Component message = Component.translatable(
            "chat.createlaborrush.rebellion.start",
            rebelCount
        );
        serverLevel.getServer().getPlayerList().broadcastSystemMessage(message, false);

        // 发送起义口号
        ChatMessageHandler.sendRebellionChat(serverLevel, center);
        // 发送首领口号
        ChatMessageHandler.sendLeaderChat(serverLevel, center);

        // 首领提出罢工要求：绿宝石谈判
        serverLevel.getServer().getPlayerList().broadcastSystemMessage(Component.translatable(
            "chat.createlaborrush.peace.offer", getPeaceDemand(data)
        ), false);

        // 对女仆起义工人执行武装和气泡
        Player nearestPlayer = serverLevel.getNearestPlayer(center.getX(), center.getY(), center.getZ(), 64, false);
        for (LivingEntity rebel : data.rebels) {
            if (WorkerTypeDetector.isMaid(rebel)) {
                LivingEntity owner = WorkerTypeDetector.getMaidOwner(rebel);
                String ownerName = owner != null ? owner.getName().getString() : null;
                MaidRebellionHandler.showRebellionBubble(rebel, ownerName);
                if (nearestPlayer != null) {
                    MaidRebellionHandler.armAndAttackMaid(rebel, nearestPlayer, level);
                }
            }
        }

        // ===== 成就授予 =====
        // 成就：第一次起义 — 触发工人起义
        if (nearestPlayer instanceof ServerPlayer sp) {
            AchievementHandler.grantAchievement(sp, AchievementHandler.FIRST_REBELLION);
        } else {
            // 如果没有最近的玩家，给所有附近的玩家
            for (ServerPlayer player : serverLevel.getPlayers(p -> p.distanceToSqr(center.getX(), center.getY(), center.getZ()) < 64 * 64)) {
                AchievementHandler.grantAchievement(player, AchievementHandler.FIRST_REBELLION);
                break;
            }
        }

        // 成就：群起而攻之 — 触发3人以上起义
        if (rebelCount >= 3) {
            if (nearestPlayer instanceof ServerPlayer sp) {
                AchievementHandler.grantAchievement(sp, AchievementHandler.REBELLION_3_PEOPLE);
            } else {
                for (ServerPlayer player : serverLevel.getPlayers(p -> p.distanceToSqr(center.getX(), center.getY(), center.getZ()) < 64 * 64)) {
                    AchievementHandler.grantAchievement(player, AchievementHandler.REBELLION_3_PEOPLE);
                    break;
                }
            }
        }

        // 追踪每个工人的起义次数（用于"反抗军领袖"成就）
        for (LivingEntity rebel : data.rebels) {
            UUID uuid = rebel.getUUID();
            int count = data.workerRebellionCount.getOrDefault(uuid, 0) + 1;
            data.workerRebellionCount.put(uuid, count);
            // 如果该工人之前已经起义过，累加次数
            var rebelData = rebel.getPersistentData();
            int totalRebellions = rebelData.getInt("laborrush.totalRebellions") + 1;
            rebelData.putInt("laborrush.totalRebellions", totalRebellions);
        }
    }

    private static void applyRebellionEffects(ServerLevel level, RebellionData data) {
        int count = data.rebels.size();
        int strengthLevel;
        if (count <= 2) strengthLevel = 0;
        else if (count <= 4) strengthLevel = 1;
        else if (count <= 6) strengthLevel = 2;
        else if (count <= 8) strengthLevel = 3;
        else strengthLevel = 4;

        int duration = data.remainingTicks;

        for (LivingEntity rebel : data.rebels) {
            // 保存原 WORK_EFFECT（罢工结束原样恢复，等级/剩余时长都不变）
            data.savedWorkEffects.put(rebel.getUUID(), rebel.getEffect(ModEffects.WORK_EFFECT.get()));
            rebel.removeEffect(ModEffects.WORK_EFFECT.get());

            rebel.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, duration, strengthLevel, false, true, true
            ));
            // 不加速度效果：叛军保持基础移速（≈玩家步行），玩家跑步冲刺能甩开，
            // 追不上时叛军会停下来拆家，方便玩家围观
            rebel.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true
            ));
        }

        LivingEntity leader = data.leader;
        if (leader != null) {
            if (!WorkerTypeDetector.isMaid(leader)) {
                // 村民首领：头顶 👑 名字；女仆首领不改名（用气泡表现，避免顶名）
                String name = leader.getDisplayName().getString();
                leader.setCustomName(Component.literal("\uD83D\uDC51 " + name));
                leader.setCustomNameVisible(true);
            }
            leader.setGlowingTag(true);
            leader.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION, duration, 1, false, true, true
            ));
        }
    }

    private static void playRebellionEffects(ServerLevel level, BlockPos center) {
        level.sendParticles(
            ParticleTypes.EXPLOSION,
            center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
            5, 1.0, 1.0, 1.0, 0.1
        );
        level.sendParticles(
            ParticleTypes.ANGRY_VILLAGER,
            center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5,
            10, 2.0, 1.0, 2.0, 0.1
        );
        level.sendParticles(
            ParticleTypes.FLAME,
            center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
            20, 2.0, 1.0, 2.0, 0.05
        );
        level.playSound(null, center, SoundEvents.RAID_HORN.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.REBELLION_ENABLED.get()) return;

        var server = event.getServer();
        if (server == null) return;

        // 自动起义判定：每 10 秒检查一次各维度
        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dim = level.dimension();
            int timer = CHECK_TIMERS.getOrDefault(dim, 0) + 1;
            if (timer < REBELLION_CHECK_INTERVAL_TICKS) {
                CHECK_TIMERS.put(dim, timer);
                continue;
            }
            CHECK_TIMERS.put(dim, 0);

            var existing = REBELLIONS.get(dim);
            if (existing == null || !existing.active) {
                tryStartRebellion(level);
            }
        }

        for (var entry : new HashMap<>(REBELLIONS).entrySet()) {
            var dimensionKey = entry.getKey();
            var data = entry.getValue();
            if (!data.active) continue;

            var level = server.getLevel(dimensionKey);
            if (level == null) continue;

            tickRebellion(level, data);
        }

        for (var entry : new HashMap<>(STRIKE_ZONES).entrySet()) {
            var dimensionKey = entry.getKey();
            var zones = entry.getValue();
            zones.removeIf(zone -> {
                zone.remainingTicks--;
                return zone.remainingTicks <= 0;
            });
            if (zones.isEmpty()) {
                STRIKE_ZONES.remove(dimensionKey);
            }
        }
    }

    private static void tickRebellion(ServerLevel level, RebellionData data) {
        data.rebels.removeIf(rebel -> !rebel.isAlive());

        if (data.rebels.isEmpty()) {
            endRebellion(level, data, true, false);
            return;
        }

        // 每 tick 强制叛军保持在地面（防止被座位方块吸回/骑乘状态残留）
        for (LivingEntity rebel : data.rebels) {
            if (rebel.getVehicle() != null) {
                rebel.stopRiding();
            }
        }

        data.remainingTicks--;
        if (data.remainingTicks <= 0) {
            endRebellion(level, data, false, false);
            return;
        }

        if (data.bossEvent != null) {
            float progress = (float) data.remainingTicks / (Config.REBELLION_DURATION.get() * 20);
            data.bossEvent.setProgress(progress);
        }

        if (data.leader == null || !data.leader.isAlive()) {
            if (!data.rebels.isEmpty()) {
                data.leader = data.rebels.get(level.random.nextInt(data.rebels.size()));
                if (!WorkerTypeDetector.isMaid(data.leader)) {
                    String name = data.leader.getDisplayName().getString();
                    data.leader.setCustomName(Component.literal("\uD83D\uDC51 " + name));
                    data.leader.setCustomNameVisible(true);
                }
                data.leader.setGlowingTag(true);
            }
        }

        data.contagionTimer++;
        if (data.contagionTimer >= CONTAGION_INTERVAL_TICKS) {
            data.contagionTimer = 0;
            tryContagion(level, data);
        }

        processRebelAI(level, data);
    }

    private static void tryContagion(ServerLevel level, RebellionData data) {
        if (data.rebels.size() >= MAX_REBELS) return;

        Set<LivingEntity> newRebels = new HashSet<>();

        for (LivingEntity rebel : data.rebels) {
            AABB searchArea = rebel.getBoundingBox().inflate(CONTAGION_RADIUS);

            for (LivingEntity living : WorkerHelper.collectWorkers(level, searchArea)) {
                if (data.rebels.contains(living)) continue;
                if (living.hasEffect(ModEffects.WORK_EFFECT.get())
                    && level.random.nextDouble() < CONTAGION_CHANCE) {
                    newRebels.add(living);
                }
            }
        }

        for (LivingEntity newRebel : newRebels) {
            if (data.rebels.size() >= MAX_REBELS) break;
            data.rebels.add(newRebel);
            // 记录原座位（罢工结束自动坐回去）
            Entity vehicle = newRebel.getVehicle();
            if (vehicle != null) {
                data.savedSeats.put(newRebel.getUUID(), vehicle);
            }
            // 保存原 WORK_EFFECT（罢工结束原样恢复）
            data.savedWorkEffects.put(newRebel.getUUID(), newRebel.getEffect(ModEffects.WORK_EFFECT.get()));
            newRebel.removeEffect(ModEffects.WORK_EFFECT.get());
            // 传染叛军立即解除骑乘（防止坐回座位卡住）
            newRebel.stopRiding();

            int count = data.rebels.size();
            int strengthLevel;
            if (count <= 2) strengthLevel = 0;
            else if (count <= 4) strengthLevel = 1;
            else if (count <= 6) strengthLevel = 2;
            else if (count <= 8) strengthLevel = 3;
            else strengthLevel = 4;

            for (LivingEntity rebel : data.rebels) {
                rebel.removeEffect(MobEffects.DAMAGE_BOOST);
                rebel.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST, data.remainingTicks, strengthLevel, false, true, true
                ));
            }
        }

        if (!newRebels.isEmpty()) {
            ChatMessageHandler.sendContagionChat(level, data.origin);
        }
    }

    /**
     * 导航工具：只有当前没有进行中的路径（或被其他 AI 打断）时才重新寻路。
     * 每 tick 无条件 moveTo 会反复重置寻路，导致实体原地打转走不动。
     */
    private static void navigateTo(Mob mob, double x, double y, double z, double speed) {
        var nav = mob.getNavigation();
        if (!nav.isInProgress() || nav.isDone()) {
            nav.moveTo(x, y, z, speed);
        }
    }

    private static void processRebelAI(ServerLevel level, RebellionData data) {
        for (LivingEntity rebel : data.rebels) {
            if (!rebel.isAlive()) continue;
            boolean isMaid = WorkerTypeDetector.isMaid(rebel);

            Player nearestPlayer = level.getNearestPlayer(rebel, 32);
            double distSq = nearestPlayer != null ? rebel.distanceToSqr(nearestPlayer) : Double.MAX_VALUE;
            boolean hasNearbyPlayer = nearestPlayer != null && distSq <= 16 * 16;

            if (hasNearbyPlayer) {
                if (rebel instanceof Mob m) {
                    if (isMaid) {
                        // 女仆：强制只打玩家（setTarget + 覆盖 ATTACK_TARGET 脑记忆，防她自己换目标）
                        if (m.getTarget() != nearestPlayer) {
                            MaidRebellionHandler.forceAttackTarget(m, nearestPlayer);
                        }
                    } else {
                        m.setTarget(nearestPlayer);
                    }
                    // 追击玩家（有进行中的路径就不打断）
                    if (distSq > 3.5 * 3.5) {
                        navigateTo(m, nearestPlayer.getX(), nearestPlayer.getY(), nearestPlayer.getZ(), 1.0);
                    }
                    // 近身攻击：每 20 tick 一次
                    if (distSq <= 3.5 * 3.5 && rebel.tickCount % 20 == 0) {
                        if (isMaid) {
                            // 女仆自驱动攻击（doHurtTarget 走女仆的攻击属性/挥击动画）
                            m.doHurtTarget(nearestPlayer);
                        } else {
                            // 村民没有 attack_damage 属性，直接造成伤害
                            float damage = 2.0f + data.amplifier;
                            nearestPlayer.hurt(level.damageSources().mobAttack(rebel), damage);
                        }
                    }
                }
                // 顺手拆家：每 10 tick 按 destroyIntensity 概率找 6 格内方块拆（会走过去）
                if (Config.REBELLION_CAN_DESTROY.get()
                        && rebel.tickCount % 10 == 0
                        && level.random.nextDouble() < Config.REBELLION_DESTROY_INTENSITY.get()) {
                    tryDestroyBlock(level, rebel, data, 6, true);
                }
            } else {
                // 无玩家或玩家很远：清空目标（女仆还要擦除脑记忆，防她乱打别人），专心找方块拆
                if (rebel instanceof Mob m) {
                    if (isMaid) {
                        MaidRebellionHandler.clearAttackTarget(m);
                    } else {
                        m.setTarget(null);
                    }
                }
                if (Config.REBELLION_CAN_DESTROY.get()) {
                    tryDestroyBlock(level, rebel, data, 12, true);
                }
            }
        }
    }

    private static void tryDestroyBlock(ServerLevel level, LivingEntity rebel, RebellionData data,
                                        int maxSearchRadius, boolean walkToTarget) {
        BlockPos rebelPos = rebel.blockPosition();
        UUID rebelId = rebel.getUUID();
        BlockPos target = data.demolitionTargets.get(rebelId);

        // 无目标 / 目标已被拆 / 目标太远 → 重新找一个（换目标时重置挖掘进度）
        if (target == null || !target.closerThan(rebelPos, 24)
                || level.getBlockState(target).isAir()) {
            target = findDemolitionTarget(level, rebelPos, maxSearchRadius);
            if (target == null) return;
            data.demolitionTargets.put(rebelId, target);
            data.demolitionProgress.remove(rebelId);
        }

        // 需要走过去且未到达 → 走向目标（无玩家时全神拆家）
        if (walkToTarget && target.distSqr(rebelPos) > 3 * 3) {
            if (rebel instanceof Mob m) {
                navigateTo(m, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
            }
            return;
        }

        // 到达目标 → 按"空手挖掘速度"累积进度：挖掘时间 = 方块硬度（秒），每 tick 推进 1/20
        BlockState state = level.getBlockState(target);
        if (state.isAir()) {
            data.demolitionTargets.remove(rebelId);
            data.demolitionProgress.remove(rebelId);
            return;
        }
        float destroySpeed = Math.max(0.1f, state.getDestroySpeed(level, target));
        float progress = data.demolitionProgress.getOrDefault(rebelId, 0f) + 0.05f / destroySpeed;
        data.demolitionProgress.put(rebelId, progress);

        // 敲击音效（每 4 tick 一下）
        if (rebel.tickCount % 4 == 0) {
            level.playSound(null, target,
                state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.8F, 0.8F);
        }

        if (progress >= 1.0f) {
            level.destroyBlock(target, true);
            level.sendParticles(
                ParticleTypes.FLAME,
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                5, 0.3, 0.3, 0.3, 0.05
            );
            data.destroyedBlockCount++;
            CreateLaborRush.LOGGER.info(
                "Rebel at {} destroyed block at {} (hardness {})",
                rebelPos, target, destroySpeed
            );
            data.demolitionTargets.remove(rebelId);
            data.demolitionProgress.remove(rebelId);
        }
    }

    /**
     * 在叛军周围指定半径内随机找一个可破坏的方块作为目标
     */
    private static BlockPos findDemolitionTarget(ServerLevel level, BlockPos center, int maxRadius) {
        for (int radius = 1; radius <= maxRadius; radius++) {
            int half = radius * 2 + 1;
            for (int i = 0; i < 24; i++) {
                int x = center.getX() + level.random.nextInt(half) - radius;
                int y = center.getY() + level.random.nextInt(5) - 2;
                int z = center.getZ() + level.random.nextInt(half) - radius;
                BlockPos pos = new BlockPos(x, y, z);
                if (pos.equals(center.below())) continue;
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.is(Blocks.BEDROCK)) continue;
                if (state.getBlock() instanceof net.minecraft.world.level.block.BushBlock) continue;
                return pos.immutable();
            }
        }
        return null;
    }

    private static void endRebellion(ServerLevel level, RebellionData data, boolean allKilled, boolean peaceful) {
        data.active = false;

        if (data.bossEvent != null) {
            data.bossEvent.removeAllPlayers();
        }

        BlockPos center = data.origin;

        // 找到最近的玩家授予成就
        Player nearestPlayer = level.getNearestPlayer(center.getX(), center.getY(), center.getZ(), 64, false);

        if (allKilled) {
            for (LivingEntity rebel : data.rebels) {
                if (rebel.isAlive()) {
                    level.sendParticles(
                        ParticleTypes.END_ROD,
                        rebel.getX(), rebel.getY() + 1, rebel.getZ(),
                        10, 0.5, 0.5, 0.5, 0.1
                    );
                    rebel.setGlowingTag(false);
                    rebel.setCustomName(null);
                    rebel.setCustomNameVisible(false);
                }
            }
            level.sendParticles(
                ParticleTypes.END_ROD,
                center.getX(), center.getY() + 1, center.getZ(),
                20, 2.0, 1.0, 2.0, 0.1
            );
            level.playSound(
                null, center, SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 1.0F
            );

            Component message = Component.translatable(
                "chat.createlaborrush.rebellion.suppressed"
            );
            level.getServer().getPlayerList().broadcastSystemMessage(message, false);

            // 成就：镇压者 — 成功镇压起义
            if (nearestPlayer instanceof ServerPlayer sp) {
                AchievementHandler.grantAchievement(sp, AchievementHandler.SUPPRESSOR);
            }

            // 成就：拆迁大队 — 起义工人拆除5+设备
            if (data.destroyedBlockCount >= 5 && nearestPlayer instanceof ServerPlayer sp) {
                AchievementHandler.grantAchievement(sp, AchievementHandler.DEMOLITION_SQUAD);
            }
        } else if (peaceful) {
            // 绿宝石谈判和平解决：庆祝粒子 + 音效
            for (LivingEntity rebel : data.rebels) {
                if (rebel.isAlive()) {
                    level.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        rebel.getX(), rebel.getY() + 1, rebel.getZ(),
                        8, 0.5, 0.5, 0.5, 0.1
                    );
                    rebel.setGlowingTag(false);
                    rebel.setCustomName(null);
                    rebel.setCustomNameVisible(false);
                }
            }
            level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                center.getX(), center.getY() + 1, center.getZ(),
                30, 2.0, 1.0, 2.0, 0.1
            );
            level.playSound(
                null, center, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F
            );

            Component message = Component.translatable(
                "chat.createlaborrush.peace.ended"
            );
            level.getServer().getPlayerList().broadcastSystemMessage(message, false);
        } else {
            for (LivingEntity rebel : data.rebels) {
                if (rebel.isAlive()) {
                    level.sendParticles(
                        ParticleTypes.SMOKE,
                        rebel.getX(), rebel.getY() + 1, rebel.getZ(),
                        5, 0.5, 0.5, 0.5, 0.05
                    );
                    rebel.setGlowingTag(false);
                    rebel.setCustomName(null);
                    rebel.setCustomNameVisible(false);
                }
            }
            level.sendParticles(
                ParticleTypes.SMOKE,
                center.getX(), center.getY() + 1, center.getZ(),
                15, 2.0, 1.0, 2.0, 0.05
            );
            level.playSound(
                null, center, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.HOSTILE, 1.0F, 1.0F
            );

            Component message = Component.translatable(
                "chat.createlaborrush.rebellion.ended"
            );
            level.getServer().getPlayerList().broadcastSystemMessage(message, false);
        }

        // 恢复工人工作状态：清除起义强化效果，并把罢工前保存的 WORK_EFFECT 原样放回
        // （等级与剩余时长不变，不重新发放新 buff）
        for (LivingEntity rebel : data.rebels) {
            if (!rebel.isAlive()) continue;
            rebel.removeEffect(MobEffects.DAMAGE_BOOST);
            rebel.removeEffect(MobEffects.MOVEMENT_SPEED);
            rebel.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            rebel.removeEffect(MobEffects.REGENERATION);
            MobEffectInstance savedWork = data.savedWorkEffects.get(rebel.getUUID());
            if (savedWork != null) {
                rebel.addEffect(savedWork);
            }
            // 自动坐回原座位（座位有乘客 + 有 WORK_EFFECT 就会继续干活）
            Entity seat = data.savedSeats.get(rebel.getUUID());
            if (seat != null && seat.isAlive() && seat.getPassengers().isEmpty()) {
                boolean ok = rebel.startRiding(seat, true);
                CreateLaborRush.LOGGER.info(
                    "Rebellion end: rebel {} remount seat at {} ok={} passenger={}",
                    rebel.getUUID(), seat.blockPosition(), ok, rebel.isPassenger()
                );
            } else if (seat != null && !seat.isAlive()) {
                // create 的座位实体空置后会被自动回收：在原位置重建座位并坐下
                try {
                    SeatBlock.sitDown(level, seat.blockPosition(), rebel);
                    CreateLaborRush.LOGGER.info(
                        "Rebellion end: rebel {} rebuilt seat at {} passenger={}",
                        rebel.getUUID(), seat.blockPosition(), rebel.isPassenger()
                    );
                } catch (Exception e) {
                    CreateLaborRush.LOGGER.debug(
                        "Rebellion end: rebuild seat for rebel {} failed", rebel.getUUID(), e
                    );
                }
            } else {
                CreateLaborRush.LOGGER.info(
                    "Rebellion end: rebel {} no valid seat (alive={} occupied={})",
                    rebel.getUUID(), seat != null && seat.isAlive(), seat != null && !seat.getPassengers().isEmpty()
                );
            }
            if (WorkerTypeDetector.isMaid(rebel)) {
                MaidRebellionHandler.restoreMaid(rebel, level);
            }
        }

        // 罢工冷却：结束后该区域一段时间内不再触发新起义，工人得以安心回岗
        // （镇压=长冷却；自然结束/谈判=短冷却，防止立刻再起义看起来像"永远不回去"）
        List<StrikeZone> zones = STRIKE_ZONES.computeIfAbsent(
            level.dimension(), k -> new ArrayList<>()
        );
        int cooldownTicks = allKilled ? STRIKE_DURATION_TICKS : 1200;
        zones.add(new StrikeZone(
            center, Config.REBELLION_RADIUS.get(), cooldownTicks
        ));

        // 成就：反抗军领袖 — 同一工人起义3次
        if (nearestPlayer instanceof ServerPlayer sp) {
            for (Map.Entry<UUID, Integer> entry : data.workerRebellionCount.entrySet()) {
                if (entry.getValue() >= 3) {
                    // 查找该工人实体
                    for (LivingEntity rebel : data.rebels) {
                        if (rebel.getUUID().equals(entry.getKey())) {
                            int totalRebellions = rebel.getPersistentData().getInt("laborrush.totalRebellions");
                            if (totalRebellions >= 3) {
                                AchievementHandler.grantAchievement(sp, AchievementHandler.REBELLION_LEADER_3);
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }

        REBELLIONS.remove(level.dimension());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!Config.REBELLION_ENABLED.get()) return;
        if (event.getEntity().level().isClientSide) return;

        var entity = event.getEntity();
        var dimensionKey = entity.level().dimension();
        var data = REBELLIONS.get(dimensionKey);

        if (data != null && data.active && data.rebels.contains(entity)) {
            if (entity.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    entity.getX(), entity.getY() + 1, entity.getZ(),
                    5, 0.5, 0.5, 0.5, 0.05
                );
            }

            // 成就：灭村者 — 击杀3+起义工人
            var killer = event.getSource().getEntity();
            if (killer instanceof ServerPlayer sp) {
                var playerData = sp.getPersistentData();
                String killKey = "laborrush.rebelKills_" + dimensionKey.location().toString();
                int kills = playerData.getInt(killKey) + 1;
                playerData.putInt(killKey, kills);
                if (kills >= 3) {
                    AchievementHandler.grantAchievement(sp, AchievementHandler.REBEL_KILLER_3);
                }
            }
        }
    }

    public static boolean isInStrikeZone(Level level, BlockPos pos) {
        var zones = STRIKE_ZONES.get(level.dimension());
        if (zones == null) return false;
        return zones.stream().anyMatch(zone -> zone.contains(pos));
    }

    /**
     * 检查指定位置是否有活跃的起义
     */
    public static boolean hasActiveRebellion(Level level, BlockPos pos) {
        var data = REBELLIONS.get(level.dimension());
        return data != null && data.active && data.origin.distSqr(pos) < Config.REBELLION_RADIUS.get() * Config.REBELLION_RADIUS.get();
    }

    /**
     * 指定实体是否为当前起义中的叛军（用于"雷电法王"等成就判定）
     */
    public static boolean isRebelEntity(Level level, Entity entity) {
        var data = REBELLIONS.get(level.dimension());
        return data != null && data.active && data.rebels.contains(entity);
    }

    /**
     * 获取当前起义中的全部叛军（供引雷钟劈向起义者等使用）
     */
    public static List<LivingEntity> getActiveRebels(Level level) {
        var data = REBELLIONS.get(level.dimension());
        if (data == null || !data.active) return java.util.Collections.emptyList();
        return new ArrayList<>(data.rebels);
    }

    /**
     * 直接镇压当前维度的起义（闪电五连鞭命中叛军时调用，无需劈死叛军）
     *
     * @return 是否成功镇压了起义
     */
    public static boolean suppressActiveRebellion(ServerLevel level) {
        var data = REBELLIONS.get(level.dimension());
        if (data == null || !data.active) return false;
        endRebellion(level, data, true, false);
        return true;
    }
}