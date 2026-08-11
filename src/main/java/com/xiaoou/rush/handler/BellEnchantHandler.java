package com.xiaoou.rush.handler;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.redstone.deskBell.DeskBellBlock;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
import com.xiaoou.rush.util.WorkerHelper;
import com.yyn.labor.util.WorkerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = CreateLaborRush.MODID)
public class BellEnchantHandler {

    // 记录"放置在地上的附魔钟"（MC 中 BlockItem 放置后附魔数据会丢失，原版钟无 BlockEntity）
    // key: "dimension:pos" -> {fireAspectLevel, channelingLevel}
    // 持久化到世界数据（PlacedBellData），重启游戏不丢失
    private static final Map<String, int[]> PLACED_ENCHANTED_BELLS = new HashMap<>();

    public static String keyOf(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /**
     * 钟或呼唤铃（Create desk bell）都算"钟"
     */
    public static boolean isBellBlock(Block block) {
        return block instanceof BellBlock || block instanceof DeskBellBlock;
    }

    /**
     * 构建附魔钟/附魔呼唤铃物品（供破坏掉落/创造中键复制复用；原版钟无 BlockEntity，必须手动还原附魔）
     */
    public static ItemStack buildEnchantedStack(Level level, Block block, int fireAspect, int channeling) {
        ItemStack bell = new ItemStack(block.asItem());
        var fireHolder = level.holder(Enchantments.FIRE_ASPECT);
        if (fireAspect > 0 && fireHolder.isPresent()) {
            bell.enchant(fireHolder.get(), fireAspect);
        }
        var channelHolder = level.holder(Enchantments.CHANNELING);
        if (channeling > 0 && channelHolder.isPresent()) {
            bell.enchant(channelHolder.get(), channeling);
        }
        return bell;
    }

    /**
     * 构建附魔钟（供破坏掉落/创造中键复制复用；原版钟无 BlockEntity，必须手动还原附魔）
     */
    public static ItemStack buildEnchantedBell(ServerLevel level, int fireAspect, int channeling) {
        return buildEnchantedStack(level, net.minecraft.world.level.block.Blocks.BELL, fireAspect, channeling);
    }

    private static void putPlacedBell(ServerLevel level, String key, int fireAspect, int channeling) {
        PLACED_ENCHANTED_BELLS.put(key, new int[]{fireAspect, channeling});
        PlacedBellData.get(level).put(key, new int[]{fireAspect, channeling});
    }

    private static boolean removePlacedBell(ServerLevel level, String key) {
        boolean changed = PLACED_ENCHANTED_BELLS.remove(key) != null;
        if (PlacedBellData.get(level).remove(key)) changed = true;
        return changed;
    }

    private static int[] getPlacedBell(ServerLevel level, String key) {
        int[] cached = PLACED_ENCHANTED_BELLS.get(key);
        if (cached != null) return cached;
        return PlacedBellData.get(level).get(key);
    }

    private static int enchantLevel(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        var holder = level.holder(enchantment);
        if (holder.isEmpty()) return 0;
        return stack.getEnchantments().getLevel(holder.get());
    }

    /**
     * 放置附魔钟时记录附魔信息（供之后空手敲响地上的钟使用）
     */
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!isBellBlock(event.getPlacedBlock().getBlock())) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        Level level = serverLevel;
        String key = keyOf(level.dimension(), event.getPos());
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        var stack = living.getMainHandItem();

        int fireAspect = enchantLevel(level, stack, Enchantments.FIRE_ASPECT);
        int channeling = enchantLevel(level, stack, Enchantments.CHANNELING);

        if (fireAspect == 0 && channeling == 0) {
            // 放置的是普通钟，清除该位置旧的附魔记录（仅实际清除时才广播，防刷屏）
            if (removePlacedBell(serverLevel, key)) {
                com.xiaoou.rush.network.BellSyncPayload.broadcastToAll(serverLevel.players());
            }
            return;
        }

        putPlacedBell(serverLevel, key, fireAspect, channeling);
        com.xiaoou.rush.network.BellSyncPayload.broadcastToAll(serverLevel.players());
        CreateLaborRush.LOGGER.info("Placed enchanted bell at {}: fireAspect={}, channeling={}",
            event.getPos(), fireAspect, channeling);
    }

    /**
     * 破坏钟时：若该位置是附魔钟，清除记录并掉落附魔钟
     * （原版钟无 BlockEntity，附魔在放置时丢失，破坏后掉落物也是普通钟；这里还原附魔）
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        BlockState state = serverLevel.getBlockState(event.getPos());
        if (!isBellBlock(state.getBlock())) return;

        String key = keyOf(serverLevel.dimension(), event.getPos());
        int[] placed = getPlacedBell(serverLevel, key);
        if (removePlacedBell(serverLevel, key)) {
            com.xiaoou.rush.network.BellSyncPayload.broadcastToAll(serverLevel.players());
        }

        if (placed != null && (placed[0] > 0 || placed[1] > 0)) {
            // 取消原版破坏（防止掉落普通钟），改为掉落附魔钟
            event.setCanceled(true);
            ItemStack bell = buildEnchantedStack(serverLevel, state.getBlock(), placed[0], placed[1]);
            serverLevel.destroyBlock(event.getPos(), false);
            net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(serverLevel,
                event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, bell);
            serverLevel.addFreshEntity(item);
            CreateLaborRush.LOGGER.info("Dropped enchanted bell at {}: fireAspect={}, channeling={}",
                event.getPos(), placed[0], placed[1]);
        }
    }
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 检查点击的方块是否为钟/DeskBell
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!(block instanceof BellBlock || block instanceof DeskBellBlock))
            return;

        if (event.getLevel().isClientSide) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        var player = event.getEntity();
        var stack = player.getMainHandItem();
        var level = event.getLevel();

        // 附魔来源一：主手手持附魔钟/附魔呼唤铃
        boolean holdingBell = stack.getItem() instanceof BlockItem blockItem && isBellBlock(blockItem.getBlock());
        int fireAspectLevel = holdingBell ? enchantLevel(level, stack, Enchantments.FIRE_ASPECT) : 0;
        int channelingLevel = holdingBell ? enchantLevel(level, stack, Enchantments.CHANNELING) : 0;

        // 附魔来源二：放置在地上的附魔钟（空手敲响，持久化存储，重启不丢）
        int[] placed = getPlacedBell(serverLevel, keyOf(level.dimension(), event.getPos()));
        if (placed != null) {
            fireAspectLevel = Math.max(fireAspectLevel, placed[0]);
            channelingLevel = Math.max(channelingLevel, placed[1]);
        }

        // 无附魔则不处理（由 BellWorkHandler 处理普通钟声）
        if (fireAspectLevel == 0 && channelingLevel == 0)
            return;

        // 不取消事件：原版敲钟流程照常执行（摆动动画 + 钟声自然保留），这里只叠加效果。
        // 手持钟物品右键钟时原版是"放置失败"（钟已占位），无动画属原版行为，不影响空手敲

        // 根据附魔决定 Work 等级和批量大小
        int amplifier;
        int batchSize;
        boolean hasChanneling = false;

        if (channelingLevel > 0) {
            amplifier = 2;
            batchSize = 64;
            hasChanneling = true;
        } else if (fireAspectLevel >= 2) {
            amplifier = 2;
            batchSize = 64;
        } else {
            // 火焰附加 I 或其他任意附魔（效率/耐久等）也按附魔钟处理
            amplifier = 1;
            batchSize = 32;
        }

        // 给 16 格半径内的所有工人施加效果
        AABB area = new AABB(event.getPos()).inflate(16);

        for (LivingEntity living : WorkerHelper.collectWorkers(level, area)) {
            living.getPersistentData().putInt("laborrush.batchSize", batchSize);
            living.getPersistentData().putBoolean("laborrush.supercharged", true);
            living.getPersistentData().putInt("laborrush.amplifier", amplifier);

            living.addEffect(new MobEffectInstance(ModEffects.WORK_EFFECT, 90 * 20, amplifier, false, true, false));

            // 火焰附加：点燃工人 + 火焰粒子环绕（火焰I 2秒 / 火焰II 4秒）
            if (fireAspectLevel > 0) {
                living.setRemainingFireTicks(fireAspectLevel >= 2 ? 80 : 40);
                if (level instanceof ServerLevel slLevel) {
                    startFlameWreath(slLevel, living, fireAspectLevel >= 2 ? 80 : 60);
                }
            }

            // 引雷：触发引雷特效（蓝色光柱/闪电炸裂/冲击波/电流缠绕/地面余波）
            if (hasChanneling) {
                LightningEffectHandler.triggerLightningEffect(level, living);
            }
        }

        // 起义期间：引雷钟同时劈向 16 格内的起义叛军（叛军不在座位上，座位循环劈不到；
        // 雷劈中起义叛军可达成"雷电法王"成就）
        if (hasChanneling) {
            for (LivingEntity rebel : RebellionSystem.getActiveRebels(level)) {
                if (rebel.isAlive() && rebel.distanceToSqr(event.getPos().getCenter()) <= 16 * 16) {
                    LightningEffectHandler.triggerLightningEffect(level, rebel);
                }
            }
        }

        // 火焰附加：敲响时钟身炸出一团火焰
        if (fireAspectLevel > 0 && level instanceof ServerLevel sl2) {
            com.xiaoou.rush.util.ParticleBudget.send(sl2, ParticleTypes.FLAME,
                event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5,
                40, 1.0, 0.5, 1.0, 0.05);
            level.playSound(null, event.getPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.6F);
        }

        CreateLaborRush.LOGGER.info("Enchanted bell rung by {} at {}: fireAspect={}, channeling={}, amplifier={}, batch={}",
            player.getName().getString(), event.getPos(), fireAspectLevel, channelingLevel, amplifier, batchSize);

        // 成就
        if (player instanceof ServerPlayer sp) {
            // 成就：交响乐 — 第一次给钟附魔
            var playerData = player.getPersistentData();
            if (!playerData.getBoolean("laborrush.hasFirstBellEnchant")) {
                playerData.putBoolean("laborrush.hasFirstBellEnchant", true);
                AchievementHandler.grantAchievement(sp, AchievementHandler.FIRST_BELL_ENCHANT);
            }

            // 成就：天籁之音 — 引雷钟敲响
            if (hasChanneling) {
                AchievementHandler.grantAchievement(sp, AchievementHandler.LIGHTNING_BELL);
            }
        }
    }

    /**
     * 火焰粒子环绕工人（持续 remainingTicks 个 tick）
     */
    private static void startFlameWreath(ServerLevel level, LivingEntity target, int remainingTicks) {
        if (remainingTicks <= 0 || !target.isAlive()) return;
        double x = target.getX();
        double y = target.getY() + 1.0;
        double z = target.getZ();
        for (int i = 0; i < 4; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dx = 0.6 * Math.cos(angle);
            double dz = 0.6 * Math.sin(angle);
            com.xiaoou.rush.util.ParticleBudget.send(level, ParticleTypes.FLAME, x + dx, y, z + dz, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.getServer().tell(new net.minecraft.server.TickTask(
            level.getServer().getTickCount() + 1,
            () -> startFlameWreath(level, target, remainingTicks - 1)
        ));
    }
}