package com.xiaoou.rush.network;

import com.xiaoou.rush.util.SignBoardData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端写完牌子后发上来的 4 行文字。
 *
 * <p>只改服务端的物品：客户端那点改动不会自动同步，交给服务端写进 NBT 后
 * 由原版的槽位同步推回客户端。
 */
public class SignTextPacket {

    /** 文字写到哪一格 */
    public enum Target {
        /** 手上拿着的那件（写字界面的常规入口） */
        MAIN_HAND,
        /** 胸甲槽（潜行右键空气把牌子换上去之后） */
        CHEST
    }

    private final Target target;
    private final String[] lines;

    public SignTextPacket(Target target, String[] lines) {
        this.target = target;
        this.lines = lines;
    }

    public static void encode(SignTextPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.target);
        for (int i = 0; i < SignBoardData.LINES; i++) {
            buf.writeUtf(msg.lines[i], 384);
        }
    }

    public static SignTextPacket decode(FriendlyByteBuf buf) {
        Target target = buf.readEnum(Target.class);
        String[] lines = new String[SignBoardData.LINES];
        for (int i = 0; i < SignBoardData.LINES; i++) {
            lines[i] = buf.readUtf(384);
        }
        return new SignTextPacket(target, lines);
    }

    public static void handle(SignTextPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> apply(player, msg));
        }
        context.setPacketHandled(true);
    }

    private static void apply(ServerPlayer player, SignTextPacket msg) {
        ItemStack stack = msg.target == Target.CHEST
            ? player.getItemBySlot(EquipmentSlot.CHEST)
            : player.getMainHandItem();
        // 校验：那一格现在确实得是悬挂告示牌，否则玩家就是在用改过的客户端乱发
        if (!stack.is(ItemTags.HANGING_SIGNS)) {
            return;
        }
        SignBoardData.writeToItem(stack, msg.lines);
    }
}
