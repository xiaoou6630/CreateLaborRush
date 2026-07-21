package com.xiaoou.rush.handler;

import com.simibubi.create.content.redstone.deskBell.DeskBellBlock;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.util.WorkerHelper;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * When a player right-clicks a Bell or DeskBell,
 * applies the WORK_EFFECT to all seated workers within a 16-block radius.
 */
@EventBusSubscriber(modid = CreateLaborRush.MODID)
public class BellWorkHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!(block instanceof BellBlock || block instanceof DeskBellBlock))
            return;

        if (event.getLevel().isClientSide) return;

        AABB area = new AABB(event.getPos()).inflate(16);
        WorkerHelper.applyWorkToNearbyWorkers(event.getLevel(), area);
    }
}