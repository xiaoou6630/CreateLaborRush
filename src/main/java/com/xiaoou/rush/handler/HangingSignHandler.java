package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.network.HangingSignEditPayload;
import com.xiaoou.rush.network.SignBoardSyncPayload;
import com.xiaoou.rush.util.HangingSignData;
import com.xiaoou.rush.util.WorkerTypeDetector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 「挂牌」功能的交互逻辑：物品就是原版悬挂告示牌（{@link ItemTags#HANGING_SIGNS}），
 * 不新增物品，文字一律存在物品数据组件里。
 *
 * <ul>
 *   <li>手持牌子 + 潜行 + 右键空气 → 换到胸甲栏（原胸甲回到手上）</li>
 *   <li>手持牌子 + 不潜行 + 右键空气 → 由 {@code HangingSignClientHandler} 在客户端弹写字界面</li>
 *   <li>手持牌子 + 右键工人（村民） → 挂到工人身上，消耗一块牌子</li>
 *   <li>潜行 + 空手 + 右键已挂牌的工人 → 把牌子取回手上</li>
 * </ul>
 *
 * <p>只有涉及容器/实体数据的部分在服务端执行；客户端的写字界面走另一个只注册在
 * 客户端的监听器，两边条件写法保持一致。
 */
// 【已停用】挂牌功能暂时不接入游戏：写字渲染还有没解决的问题，先把入口注解摘掉，
// 事件方法本身原样保留。要恢复就把下面这行注解加回来（并保留客户端入口的 AddLayers 注册）。
// @EventBusSubscriber(modid = CreateLaborRush.MODID)
public class HangingSignHandler {

    /** 判定"面前是空气"的射线长度（格），与玩家触及距离同量级即可 */
    private static final int AIR_PICK_DISTANCE = 20;

    /**
     * 右键空气：潜行时把牌子换到胸甲栏。
     *
     * <p>换容器只在服务端做——客户端的容器内容由服务端同步下来，两端都改会打架。
     */
    @SubscribeEvent
    public static void onRightClickAir(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(ItemTags.HANGING_SIGNS)) {
            return;
        }
        Player player = event.getEntity();
        if (!isAimingAir(player)) {
            return;
        }
        if (!player.isShiftKeyDown()) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        swapToChest(player, event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * 右键工人：把牌子挂上去；潜行空手则是把牌子取回来。
     *
     * <p>挂/取本身只在服务端做；但"拿牌子点工人"这一支必须在客户端也取消事件，
     * 否则客户端会照常弹出村民交易界面。
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        if (!(target instanceof Villager villager) || !WorkerTypeDetector.isWorker(villager)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();

        if (player.isShiftKeyDown()) {
            // 取回要求空手。潜行 + 空手对村民本来没有原版行为，所以不必在客户端取消
            if (!held.isEmpty() || player.level().isClientSide) {
                return;
            }
            takeBack(player, villager, event.getHand());
        } else if (held.is(ItemTags.HANGING_SIGNS)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (player.level().isClientSide) {
                return;
            }
            hangOn(player, villager, held);
        }
    }

    /**
     * 处理写字界面回传的文字：写进指定槽位那件牌子。
     */
    public static void applyEdit(ServerPlayer player, HangingSignEditPayload payload) {
        ItemStack target = payload.chestSlot()
            ? player.getItemBySlot(EquipmentSlot.CHEST)
            : player.getMainHandItem();
        // 校验：目标槽位确实还放着悬挂告示牌，否则丢弃这个包
        if (!target.is(ItemTags.HANGING_SIGNS)) {
            return;
        }
        String[] lines = new String[HangingSignData.LINES];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = i < payload.lines().size() ? payload.lines().get(i) : "";
        }
        HangingSignData.writeLines(target, lines);
    }

    /** 面前是不是空气（没瞄着方块也没瞄着实体） */
    private static boolean isAimingAir(Player player) {
        return player.pick(AIR_PICK_DISTANCE, 0.0F, false).getType() == HitResult.Type.MISS;
    }

    private static void swapToChest(Player player, InteractionHand hand) {
        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack handItem = player.getItemInHand(hand);
        player.setItemSlot(EquipmentSlot.CHEST, handItem.copy());
        player.setItemInHand(hand, chestItem);
    }

    private static void hangOn(Player player, Villager villager, ItemStack held) {
        // 一个工人身上只挂一块牌子，已挂就不再消耗手里的
        if (HangingSignData.hasWornSign(villager)) {
            return;
        }
        HangingSignData.hangOn(villager, held);
        held.shrink(1);
        // 村民身上的数据不会自动同步，推一份给正在看着它的客户端
        SignBoardSyncPayload.sendToTracking(villager, HangingSignData.readWornLines(villager));
    }

    private static void takeBack(Player player, Villager villager, InteractionHand hand) {
        ItemStack sign = HangingSignData.takeOff(villager);
        if (!sign.isEmpty()) {
            player.setItemInHand(hand, sign);
            SignBoardSyncPayload.sendToTracking(villager, null);
        }
    }
}
