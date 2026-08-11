package com.xiaoou.rush.handler;

import com.simibubi.create.content.redstone.deskBell.DeskBellBlock;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.util.WorkerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateLaborRush.MODID)
public class BellWorkHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!(block instanceof BellBlock || block instanceof DeskBellBlock))
            return;

        if (event.getLevel().isClientSide) return;

        // 如果玩家手持附魔钟/附魔呼唤铃，由 BellEnchantHandler 处理
        var stack = event.getEntity().getMainHandItem();
        boolean holdingBell = stack.getItem() instanceof BlockItem blockItem
            && (blockItem.getBlock() instanceof BellBlock || blockItem.getBlock() instanceof DeskBellBlock);
        if (holdingBell) {
            int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
            int channeling = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.CHANNELING, stack);
            if (fireAspect > 0 || channeling > 0) {
                return;
            }
        }

        // 手持钟物品右键钟方块时，原版行为是尝试"放置"钟（无法敲钟）。
        // 取消放置，改为"手持钟敲钟"：播放钟声并施加效果
        // 不取消事件：原版敲钟流程照常执行（摆动动画 + 钟声自然保留），这里只叠加效果。
        // 手持钟物品右键钟时原版是"放置失败"（钟已占位），无动画属原版行为，不影响空手敲

        // 普通钟声：Work I (amp 0)
        AABB area = new AABB(event.getPos()).inflate(16);
        int affected = WorkerHelper.applyWorkToNearbyWorkers(event.getLevel(), area);

        // 成就：可持续发展 — 钟声一次给5+工人效果
        if (affected >= 5 && event.getEntity() instanceof ServerPlayer sp) {
            AchievementHandler.grantAchievement(sp, AchievementHandler.SUSTAINABLE);
        }
    }
}