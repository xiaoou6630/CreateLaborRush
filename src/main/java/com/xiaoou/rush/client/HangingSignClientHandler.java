package com.xiaoou.rush.client;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 「挂牌」的客户端交互：手持悬挂告示牌、不潜行、右键空气时弹出写字界面。
 *
 * <p>与服务端那一半（{@link com.xiaoou.rush.handler.HangingSignHandler}）判定条件保持一致，
 * 只是这边负责开界面、那边负责改数据：客户端改物品数据不会被同步，写字结果必须由
 * 界面发网络包交给服务端落盘。
 */
// 【已停用】挂牌功能暂时不接入游戏，入口注解已摘掉（见 handler.HangingSignHandler 的说明）。
// @EventBusSubscriber(modid = CreateLaborRush.MODID, value = Dist.CLIENT)
public class HangingSignClientHandler {

    /** 与服务端一致的"面前是空气"判定长度 */
    private static final int AIR_PICK_DISTANCE = 20;

    @SubscribeEvent
    public static void onRightClickAir(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!(player instanceof LocalPlayer)) {
            return;
        }
        // 潜行那一支是"换到胸甲栏"，由服务端处理
        if (player.isShiftKeyDown()) {
            return;
        }
        if (player.pick(AIR_PICK_DISTANCE, 0.0F, false).getType() != HitResult.Type.MISS) {
            return;
        }

        // 优先编辑手上那块；手上没拿牌子就看胸甲栏里那块（潜行穿上去的）
        ItemStack source = event.getItemStack();
        boolean chestSlot = false;
        if (!source.is(ItemTags.HANGING_SIGNS)) {
            source = player.getItemBySlot(EquipmentSlot.CHEST);
            chestSlot = true;
            if (!source.is(ItemTags.HANGING_SIGNS)) {
                return;
            }
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        // 用原版告示牌编辑界面写字：先造一块灌好现有文字的离屏假 BE，再交给界面编辑
        Minecraft.getInstance().setScreen(
            new HangingSignEditScreen(HangingSignEditScreen.createDetachedSign(source), chestSlot));
    }
}
