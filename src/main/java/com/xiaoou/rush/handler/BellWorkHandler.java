package com.xiaoou.rush.handler;

import com.simibubi.create.content.redstone.deskBell.DeskBellBlock;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.util.WorkerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CreateLaborRush.MODID)
public class BellWorkHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!(block instanceof BellBlock || block instanceof DeskBellBlock))
            return;

        if (event.getLevel().isClientSide) return;

        // 如果玩家手持附魔钟，由 BellEnchantHandler 处理
        var stack = event.getEntity().getMainHandItem();
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BellBlock) {
            int fireAspect = 0;
            var faHolder = event.getLevel().holder(Enchantments.FIRE_ASPECT);
            if (faHolder.isPresent()) {
                fireAspect = stack.getEnchantments().getLevel(faHolder.get());
            }
            int channeling = 0;
            var chHolder = event.getLevel().holder(Enchantments.CHANNELING);
            if (chHolder.isPresent()) {
                channeling = stack.getEnchantments().getLevel(chHolder.get());
            }
            if (fireAspect > 0 || channeling > 0) {
                return;
            }
        }

        // 普通钟声：Work I (amp 0)
        AABB area = new AABB(event.getPos()).inflate(16);
        int affected = WorkerHelper.applyWorkToNearbyWorkers(event.getLevel(), area);

        // 成就：可持续发展 — 钟声一次给5+工人效果
        if (affected >= 5 && event.getEntity() instanceof ServerPlayer sp) {
            AchievementHandler.grantAchievement(sp, AchievementHandler.SUSTAINABLE);
        }
    }
}