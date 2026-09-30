package com.xiaoou.rush.network;

import com.xiaoou.rush.util.ClientSignBoardCache;
import com.xiaoou.rush.util.SignBoardData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：某个实体身上的牌子文字变了（挂上 / 摘下 / 玩家刚追踪到它）。
 *
 * <p>村民的牌子数据存在实体持久化数据里，Forge 不会自动同步，所以必须自己推一份，
 * 否则客户端根本画不出来。
 */
public class SignBoardSyncPacket {

    private final int entityId;
    /** null 表示牌子被摘掉了 */
    private final String[] lines;

    public SignBoardSyncPacket(int entityId, String[] lines) {
        this.entityId = entityId;
        this.lines = lines;
    }

    public static void encode(SignBoardSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.lines != null);
        if (msg.lines != null) {
            for (int i = 0; i < SignBoardData.LINES; i++) {
                buf.writeUtf(msg.lines[i], 384);
            }
        }
    }

    public static SignBoardSyncPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        if (!buf.readBoolean()) {
            return new SignBoardSyncPacket(entityId, null);
        }
        String[] lines = new String[SignBoardData.LINES];
        for (int i = 0; i < SignBoardData.LINES; i++) {
            lines[i] = buf.readUtf(384);
        }
        return new SignBoardSyncPacket(entityId, lines);
    }

    public static void handle(SignBoardSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientSignBoardCache.set(msg.entityId, msg.lines));
        ctx.get().setPacketHandled(true);
    }
}
