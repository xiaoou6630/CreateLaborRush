package com.xiaoou.rush.network;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.handler.HangingSignHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 「挂牌写字」的客户端 → 服务端包：把写好的 4 行文字回传给服务端。
 *
 * <p>写字界面用的是原版告示牌编辑界面，但它编辑的只是一块离屏假 {@code SignBlockEntity}，
 * 原版那条保存路线（{@code ServerboundSignUpdatePacket}）指向的是真实方块坐标、对挂牌不适用，
 * 所以必须靠这个包把结果送回服务端，由服务端校验后写进物品数据组件。
 *
 * @param lines     4 行文字（顺序即从上到下）
 * @param chestSlot 目标槽位：{@code true} = 胸甲栏，{@code false} = 主手
 */
public record HangingSignEditPayload(List<String> lines, boolean chestSlot) implements CustomPacketPayload {

    public static final Type<HangingSignEditPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "hanging_sign_edit"));

    public static final StreamCodec<FriendlyByteBuf, HangingSignEditPayload> STREAM_CODEC = StreamCodec.of(
        HangingSignEditPayload::encode, HangingSignEditPayload::decode);

    private static void encode(FriendlyByteBuf buf, HangingSignEditPayload payload) {
        buf.writeVarInt(payload.lines.size());
        for (String line : payload.lines) {
            buf.writeUtf(line);
        }
        buf.writeBoolean(payload.chestSlot);
    }

    private static HangingSignEditPayload decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lines.add(buf.readUtf());
        }
        return new HangingSignEditPayload(List.copyOf(lines), buf.readBoolean());
    }

    public static void handleToServer(HangingSignEditPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                HangingSignHandler.applyEdit(player, payload);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
