package com.xiaoou.rush.network;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.handler.PlacedBellData;
import com.xiaoou.rush.util.ClientBellCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/**
 * 附魔钟记录同步：创造模式中键拾取在客户端本地处理（不发服务端包），
 * 因此把 PlacedBellData 全量同步给客户端，客户端 mixin 拾取时还原附魔。
 */
public record BellSyncPayload(Map<String, int[]> bells) implements CustomPacketPayload {

    public static final Type<BellSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "bell_sync"));

    public static final StreamCodec<FriendlyByteBuf, BellSyncPayload> STREAM_CODEC = StreamCodec.of(
        BellSyncPayload::encode, BellSyncPayload::decode);

    private static void encode(FriendlyByteBuf buf, BellSyncPayload payload) {
        buf.writeVarInt(payload.bells.size());
        for (Map.Entry<String, int[]> entry : payload.bells.entrySet()) {
            buf.writeUtf(entry.getKey());
            int[] arr = entry.getValue();
            buf.writeVarInt(arr.length);
            for (int v : arr) buf.writeVarInt(v);
        }
    }

    private static BellSyncPayload decode(FriendlyByteBuf buf) {
        Map<String, int[]> map = new HashMap<>();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            String key = buf.readUtf();
            int len = buf.readVarInt();
            int[] arr = new int[len];
            for (int j = 0; j < len; j++) arr[j] = buf.readVarInt();
            map.put(key, arr);
        }
        return new BellSyncPayload(map);
    }

    public static void handle(BellSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientBellCache.set(payload.bells));
    }

    public static void broadcastToPlayers(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BellSyncPayload(PlacedBellData.get(player.serverLevel()).all()));
    }

    public static void broadcastToAll(Iterable<ServerPlayer> players) {
        BellSyncPayload payload = null;
        for (ServerPlayer p : players) {
            if (payload == null) payload = new BellSyncPayload(PlacedBellData.get(p.serverLevel()).all());
            PacketDistributor.sendToPlayer(p, payload);
        }
    }

    @EventBusSubscriber(modid = CreateLaborRush.MODID)
    public static class Events {
        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            broadcastToPlayers(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
