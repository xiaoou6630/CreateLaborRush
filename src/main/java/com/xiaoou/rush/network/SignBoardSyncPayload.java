package com.xiaoou.rush.network;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.util.ClientSignBoardCache;
import com.xiaoou.rush.util.HangingSignData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：某个实体身上的牌子文字变了（挂上 / 摘下 / 玩家刚开始追踪到它）。
 *
 * <p>村民的牌子数据存在实体持久化数据里，NeoForge 不会自动同步，所以必须自己推一份，
 * 否则客户端根本画不出来。
 *
 * @param lines 4 行文字；空列表表示牌子被摘掉了
 */
public record SignBoardSyncPayload(int entityId, List<String> lines) implements CustomPacketPayload {

    public static final Type<SignBoardSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "sign_board_sync"));

    public static final StreamCodec<FriendlyByteBuf, SignBoardSyncPayload> STREAM_CODEC = StreamCodec.of(
        SignBoardSyncPayload::encode, SignBoardSyncPayload::decode);

    private static void encode(FriendlyByteBuf buf, SignBoardSyncPayload payload) {
        buf.writeVarInt(payload.entityId);
        buf.writeVarInt(payload.lines.size());
        for (String line : payload.lines) {
            buf.writeUtf(line, 384);
        }
    }

    private static SignBoardSyncPayload decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int size = buf.readVarInt();
        List<String> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lines.add(buf.readUtf(384));
        }
        return new SignBoardSyncPayload(entityId, List.copyOf(lines));
    }

    public static void handle(SignBoardSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSignBoardCache.set(
            payload.entityId(),
            payload.lines().isEmpty() ? null : payload.lines().toArray(new String[0])));
    }

    /** 把某个实体当前的牌子状态推给所有正在追踪它的玩家 */
    public static void sendToTracking(Entity target, String[] lines) {
        List<String> encoded = lines == null
            ? List.of()
            : List.of(lines[0], lines[1], lines[2], lines[3]);
        PacketDistributor.sendToPlayersTrackingEntity(target,
            new SignBoardSyncPayload(target.getId(), encoded));
    }

    /** 只推给一个玩家（玩家刚进入追踪范围时用） */
    public static void sendTo(ServerPlayer player, Entity target, String[] lines) {
        List<String> encoded = lines == null
            ? List.of()
            : List.of(lines[0], lines[1], lines[2], lines[3]);
        PacketDistributor.sendToPlayer(player,
            new SignBoardSyncPayload(target.getId(), encoded));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 玩家刚开始追踪某个村民时补发一次：新进视距、跨维度重建实体都能覆盖到。
     */
    @EventBusSubscriber(modid = CreateLaborRush.MODID)
    public static class Events {
        @SubscribeEvent
        public static void onStartTracking(PlayerEvent.StartTracking event) {
            if (!(event.getTarget() instanceof Villager villager)) {
                return;
            }
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            String[] lines = HangingSignData.readWornLines(villager);
            if (lines != null) {
                sendTo(player, villager, lines);
            }
        }
    }
}
