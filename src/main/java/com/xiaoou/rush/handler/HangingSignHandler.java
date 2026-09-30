package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.network.LaborNetworking;
import com.xiaoou.rush.network.SignBoardSyncPacket;
import com.xiaoou.rush.util.SignBoardData;
import com.xiaoou.rush.util.WorkerTypeDetector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * 「挂牌」的服务端交互：把原版悬挂告示牌挂到身上 / 挂到工人身上 / 取回来。
 *
 * <p>本类只管服务端真正改数据的那部分；客户端那半（打开写字界面）在
 * {@code client.SignEditClientEvents} 里，免得这里引用到客户端专属类。
 *
 * <p>事件一律<b>只在服务端取消</b>：客户端取消 {@code RightClickItem} / {@code EntityInteract}
 * 有可能连带把 Serverbound 包一起吞掉，服务端就什么都收不到了。
 *
 * <p>挂到工人身上时额外补发一份 {@link SignBoardSyncPacket}：实体持久化数据不会同步给客户端，
 * 不推的话客户端画不出这块牌子。
 */
// 【已停用】挂牌功能暂时不接入游戏：写字渲染还有没解决的问题，先把入口注解摘掉，
// 事件方法本身原样保留。要恢复就把下面这行注解加回来。
// @Mod.EventBusSubscriber(modid = CreateLaborRush.MODID)
public class HangingSignHandler {

    /** 判定「右键的是空气」用的距离，和玩家交互距离同量级 */
    private static final double REACH = 20.0D;

    /**
     * 潜行 + 手持悬挂告示牌右键空气 → 把牌子塞进胸甲槽，原来那件回到手上。
     */
    @SubscribeEvent
    public static void onRightClickAir(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;
        if (!event.getItemStack().is(ItemTags.HANGING_SIGNS)) return;
        // 必须确实对着空气，否则会跟原版放牌子/开门之类的操作打架
        if (player.pick(REACH, 0.0F, false).getType() != HitResult.Type.MISS) return;
        if (player.level().isClientSide) return;

        ItemStack held = player.getMainHandItem();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        player.setItemSlot(EquipmentSlot.CHEST, held);
        player.setItemInHand(InteractionHand.MAIN_HAND, chest);
        event.setCanceled(true);
    }

    /**
     * 右键工人：手持牌子且对方身上没有牌子 → 挂上去并消耗手上的牌子；
     * 潜行右键（空手即可）且对方身上有牌子 → 摘下来还到手上。
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!(event.getTarget() instanceof LivingEntity target) || !WorkerTypeDetector.isWorker(target)) return;

        boolean takeBack = player.isShiftKeyDown() && SignBoardData.hasSign(target);
        boolean hang = !player.isShiftKeyDown()
            && event.getItemStack().is(ItemTags.HANGING_SIGNS)
            && !SignBoardData.hasSign(target);
        if (!takeBack && !hang) return;
        if (player.level().isClientSide) return;

        if (takeBack) {
            ItemStack taken = SignBoardData.takeFrom(target);
            syncToTracking(target, null);
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, taken);
            } else if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
        } else {
            SignBoardData.hangOn(target, event.getItemStack());
            syncToTracking(target, SignBoardData.readFromEntity(target));
            event.getItemStack().shrink(1);
        }
        // 取消，免得村民顺手把交易界面打开
        event.setCanceled(true);
    }

    /**
     * 玩家刚开始追踪某个实体时补发一份牌子数据。
     *
     * <p>实体持久化数据不会自动同步给客户端，新进视距的玩家、以及跨维度后重建的实体
     * 都得靠这一手把牌子补回去。
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof LivingEntity target) || !SignBoardData.hasSign(target)) return;
        String[] lines = SignBoardData.readFromEntity(target);
        if (lines == null) return;
        LaborNetworking.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()),
            new SignBoardSyncPacket(target.getId(), lines));
    }

    /** 把改动推给所有能看到这个实体的玩家（lines 为 null 表示摘掉） */
    private static void syncToTracking(LivingEntity target, String[] lines) {
        LaborNetworking.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> target),
            new SignBoardSyncPacket(target.getId(), lines));
    }
}
