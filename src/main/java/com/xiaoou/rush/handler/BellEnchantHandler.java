package com.xiaoou.rush.handler;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.redstone.deskBell.DeskBellBlock;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
import com.yyn.labor.util.WorkerUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CreateLaborRush.MODID)
public class BellEnchantHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 检查点击的方块是否为钟/DeskBell
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!(block instanceof BellBlock || block instanceof DeskBellBlock))
            return;

        if (event.getLevel().isClientSide) return;

        var player = event.getEntity();
        var stack = player.getMainHandItem();

        // 检查主手物品是否为附魔的钟
        if (!(stack.getItem() instanceof BlockItem blockItem))
            return;
        if (!(blockItem.getBlock() instanceof BellBlock))
            return;

        // NeoForge 1.21.1: 使用 holder 获取附魔等级
        int fireAspectLevel = 0;
        var faHolder = event.getLevel().holder(Enchantments.FIRE_ASPECT);
        if (faHolder.isPresent()) {
            fireAspectLevel = stack.getEnchantments().getLevel(faHolder.get());
        }

        int channelingLevel = 0;
        var chHolder = event.getLevel().holder(Enchantments.CHANNELING);
        if (chHolder.isPresent()) {
            channelingLevel = stack.getEnchantments().getLevel(chHolder.get());
        }

        // 无附魔则不处理（由 BellWorkHandler 处理普通钟声）
        if (fireAspectLevel == 0 && channelingLevel == 0)
            return;

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
        } else if (fireAspectLevel == 1) {
            amplifier = 1;
            batchSize = 32;
        } else {
            amplifier = 0;
            batchSize = 0;
        }

        // 给 16 格半径内的所有工人施加效果
        var level = event.getLevel();
        AABB area = new AABB(event.getPos()).inflate(16);

        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, area)) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living && WorkerUtil.isWorkerEntity(passenger)) {
                    living.getPersistentData().putInt("laborrush.batchSize", batchSize);
                    living.getPersistentData().putBoolean("laborrush.supercharged", true);
                    living.getPersistentData().putInt("laborrush.amplifier", amplifier);

                    living.addEffect(new MobEffectInstance(ModEffects.WORK_EFFECT, 90 * 20, amplifier, false, true, false));

                    // 如果有引雷，触发引雷特效
                    if (hasChanneling) {
                        LightningEffectHandler.triggerLightningEffect(level, living);
                    }
                }
            }
        }

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
}