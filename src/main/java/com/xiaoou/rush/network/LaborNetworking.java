package com.xiaoou.rush.network;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.handler.PlacedBellData;
import com.xiaoou.rush.util.ClientBellCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 附魔钟记录同步：创造模式中键拾取在客户端本地处理（不发服务端包），
 * 因此把 PlacedBellData 全量同步给客户端，客户端 mixin 拾取时还原附魔。
 */
public class LaborNetworking {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new net.minecraft.resources.ResourceLocation(CreateLaborRush.MODID, "main"),
        () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, BellSyncPacket.class,
            BellSyncPacket::encode, BellSyncPacket::decode, BellSyncPacket::handle);
    }

    public static void broadcastFullSync(ServerLevel level) {
        BellSyncPacket packet = new BellSyncPacket(PlacedBellData.get(level).all());
        for (ServerPlayer player : level.players()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    @Mod.EventBusSubscriber(modid = CreateLaborRush.MODID)
    public static class Events {
        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new BellSyncPacket(PlacedBellData.get(level).all()));
        }
    }

    public static class BellSyncPacket {
        private final Map<String, int[]> bells;

        public BellSyncPacket(Map<String, int[]> bells) {
            this.bells = bells;
        }

        public static void encode(BellSyncPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.bells.size());
            for (Map.Entry<String, int[]> entry : msg.bells.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeVarInt(entry.getValue().length);
                for (int v : entry.getValue()) buf.writeVarInt(v);
            }
        }

        public static BellSyncPacket decode(FriendlyByteBuf buf) {
            Map<String, int[]> map = new HashMap<>();
            int n = buf.readInt();
            for (int i = 0; i < n; i++) {
                String key = buf.readUtf();
                int len = buf.readVarInt();
                int[] arr = new int[len];
                for (int j = 0; j < len; j++) arr[j] = buf.readVarInt();
                map.put(key, arr);
            }
            return new BellSyncPacket(map);
        }

        public static void handle(BellSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> ClientBellCache.set(msg.bells));
            ctx.get().setPacketHandled(true);
        }
    }
}
