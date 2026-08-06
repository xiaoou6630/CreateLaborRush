package com.xiaoou.rush.handler;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.redstone.deskBell.DeskBellBlock;
import com.xiaoou.rush.Config;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
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
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
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
    private static final Map<ResourceKey<Level>, Long> LAST_BELL_RING_TICK = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long> FIRST_BELL_RING_TICK = new HashMap<>();

    private static final int CONTAGION_INTERVAL_TICKS = 200;
    private static final int CONTAGION_RADIUS = 3;
    private static final double CONTAGION_CHANCE = 0.3;
    private static final int MAX_REBELS = 10;
    private static final int STRIKE_DURATION_TICKS = 12000;

    private RebellionSystem() {
    }

    private static class RebellionParams {
        final int checkIntervalTicks;
        final double chance;
        final int triggerTimeTicks;

        RebellionParams(int checkIntervalTicks, double chance, int triggerTimeTicks) {
            this.checkIntervalTicks = checkIntervalTicks;
            this.chance = chance;
            this.triggerTimeTicks = triggerTimeTicks;
        }
    }

    private static RebellionParams getParamsForAmplifier(int amplifier) {
        return switch (amplifier) {
            case -1 -> new RebellionParams(2400, 0.02, 12000);
            case 0 -> new RebellionParams(1200, 0.05, 6000);
            case 1 -> new RebellionParams(600, 0.08, 3000);
            case 2 -> new RebellionParams(300, 0.15, 1200);
            default -> new RebellionParams(600, 0.08, 3000);
        };
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

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.REBELLION_ENABLED.get()) return;
        if (event.getLevel().isClientSide) return;

        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!(block instanceof BellBlock || block instanceof DeskBellBlock)) return;

        var level = event.getLevel();
        var dimensionKey = level.dimension();
        long gameTime = level.getGameTime();

        FIRST_BELL_RING_TICK.putIfAbsent(dimensionKey, gameTime);

        AABB area = new AABB(event.getPos()).inflate(Config.REBELLION_RADIUS.get());
        int maxAmplifier = -1;
        int workerCount = 0;

        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, area)) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living && WorkerUtil.isWorkerEntity(passenger)) {
                    workerCount++;
                    var effect = living.getEffect(ModEffects.WORK_EFFECT.get());
                    if (effect != null) {
                        int amp = effect.getAmplifier();
                        if (amp > maxAmplifier) maxAmplifier = amp;
                    }
                }
            }
        }

        if (workerCount == 0) return;

        // 检查是否在罢工区域内（镇压后没人工作）
        if (isInStrikeZone(level, event.getPos())) return;

        int amplifier = maxAmplifier;
        RebellionParams params = getParamsForAmplifier(amplifier);

        long lastCheck = LAST_BELL_RING_TICK.getOrDefault(dimensionKey, 0L);
        if (gameTime - lastCheck < params.checkIntervalTicks) return;

        long firstBell = FIRST_BELL_RING_TICK.getOrDefault(dimensionKey, gameTime);
        if (gameTime - firstBell < params.triggerTimeTicks) return;

        LAST_BELL_RING_TICK.put(dimensionKey, gameTime);

        double roll = level.random.nextDouble();
        if (roll < params.chance) {
            startRebellion(level, area, amplifier);
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

        List<LivingEntity> candidates = new ArrayList<>();
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, area)) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living && WorkerUtil.isWorkerEntity(passenger)) {
                    candidates.add(living);
                }
            }
        }

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
            rebel.removeEffect(ModEffects.WORK_EFFECT.get());

            rebel.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, duration, strengthLevel, false, true, true
            ));
            rebel.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED, duration, 1, false, true, true
            ));
            rebel.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true
            ));
        }

        LivingEntity leader = data.leader;
        if (leader != null) {
            String name = leader.getDisplayName().getString();
            leader.setCustomName(Component.literal("\uD83D\uDC51 " + name));
            leader.setCustomNameVisible(true);
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
            endRebellion(level, data, true);
            return;
        }

        data.remainingTicks--;
        if (data.remainingTicks <= 0) {
            endRebellion(level, data, false);
            return;
        }

        if (data.bossEvent != null) {
            float progress = (float) data.remainingTicks / (Config.REBELLION_DURATION.get() * 20);
            data.bossEvent.setProgress(progress);
        }

        if (data.leader == null || !data.leader.isAlive()) {
            if (!data.rebels.isEmpty()) {
                data.leader = data.rebels.get(level.random.nextInt(data.rebels.size()));
                String name = data.leader.getDisplayName().getString();
                data.leader.setCustomName(Component.literal("\uD83D\uDC51 " + name));
                data.leader.setCustomNameVisible(true);
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

            for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, searchArea)) {
                for (Entity passenger : seat.getPassengers()) {
                    if (!(passenger instanceof LivingEntity living)) continue;
                    if (!WorkerUtil.isWorkerEntity(passenger)) continue;
                    if (data.rebels.contains(living)) continue;
                    if (living.hasEffect(ModEffects.WORK_EFFECT.get())
                        && level.random.nextDouble() < CONTAGION_CHANCE) {
                        newRebels.add(living);
                    }
                }
            }
        }

        for (LivingEntity newRebel : newRebels) {
            if (data.rebels.size() >= MAX_REBELS) break;
            data.rebels.add(newRebel);
            newRebel.removeEffect(ModEffects.WORK_EFFECT.get());

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

    private static void processRebelAI(ServerLevel level, RebellionData data) {
        double attackRatio = Config.REBELLION_ATTACK_RATIO.get();
        int attackCount = (int) Math.ceil(data.rebels.size() * attackRatio);

        for (int i = 0; i < data.rebels.size(); i++) {
            LivingEntity rebel = data.rebels.get(i);
            if (!rebel.isAlive()) continue;

            if (i < attackCount) {
                Player nearestPlayer = level.getNearestPlayer(
                    rebel, Config.REBELLION_RADIUS.get() * 2.0
                );
                if (nearestPlayer != null) {
                    if (rebel instanceof Villager) {
                        rebel.doHurtTarget(nearestPlayer);
                    } else if (rebel instanceof Mob mob) {
                        mob.setTarget(nearestPlayer);
                    }
                }
            } else {
                if (Config.REBELLION_CAN_DESTROY.get()) {
                    tryDestroyBlock(level, rebel, data);
                }
            }
        }
    }

    private static void tryDestroyBlock(ServerLevel level, LivingEntity rebel, RebellionData data) {
        if (rebel.tickCount % (Config.REBELLION_DESTROY_COOLDOWN.get() * 20) != 0) return;

        BlockPos rebelPos = rebel.blockPosition();
        int radius = 3;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos targetPos = rebelPos.offset(x, y, z);
                    BlockState state = level.getBlockState(targetPos);

                    if (state.isAir()) continue;
                    if (state.is(Blocks.BEDROCK)) continue;

                    if (level.random.nextDouble() < 0.05) {
                        level.destroyBlock(targetPos, true);
                        level.sendParticles(
                            ParticleTypes.FLAME,
                            targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                            5, 0.3, 0.3, 0.3, 0.05
                        );
                        data.destroyedBlockCount++;
                        CreateLaborRush.LOGGER.info(
                            "Rebel at {} destroyed block at {}",
                            rebelPos, targetPos
                        );
                        return;
                    }
                }
            }
        }
    }

    private static void endRebellion(ServerLevel level, RebellionData data, boolean allKilled) {
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

            List<StrikeZone> zones = STRIKE_ZONES.computeIfAbsent(
                level.dimension(), k -> new ArrayList<>()
            );
            zones.add(new StrikeZone(
                center, Config.REBELLION_RADIUS.get(), STRIKE_DURATION_TICKS
            ));

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

        // 成就：反抗军领袖 — 同一工人起义3次
        if (nearestPlayer instanceof ServerPlayer sp) {
            for (Map.Entry<UUID, Integer> entry : data.workerRebellionCount.entrySet()) {
                if (entry.getValue() >= 3) {
                    // 检查该工人总起义次数
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
}