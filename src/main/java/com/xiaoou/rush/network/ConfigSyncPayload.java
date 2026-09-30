package com.xiaoou.rush.network;

import com.xiaoou.rush.Config;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.handler.ConfigSyncHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 配置全量同步包，双向注册：
 * <ul>
 *   <li>客户端 → 服务端：请求应用一份新配置（服务端会校验管理员权限）</li>
 *   <li>服务端 → 客户端：下发权威配置值（加入服务器时，或管理员改动之后）</li>
 * </ul>
 */
public record ConfigSyncPayload(Config.Values values) implements CustomPacketPayload {

    public static final Type<ConfigSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
        ConfigSyncPayload::encode, ConfigSyncPayload::decode);

    private static void encode(FriendlyByteBuf buf, ConfigSyncPayload payload) {
        Config.Values v = payload.values;
        buf.writeDouble(v.destroyChance());
        buf.writeDouble(v.destroyRatioMin());
        buf.writeDouble(v.destroyRatioMax());
        buf.writeBoolean(v.rebellionEnabled());
        buf.writeVarInt(v.rebellionTriggerTime());
        buf.writeDouble(v.rebellionChance());
        buf.writeVarInt(v.rebellionDuration());
        buf.writeVarInt(v.rebellionRadius());
        buf.writeBoolean(v.canDestroyDevices());
        buf.writeVarInt(v.destroyCooldown());
        buf.writeDouble(v.destroyIntensity());
    }

    private static ConfigSyncPayload decode(FriendlyByteBuf buf) {
        return new ConfigSyncPayload(new Config.Values(
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readBoolean(), buf.readVarInt(), buf.readDouble(),
            buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
            buf.readVarInt(), buf.readDouble()
        ));
    }

    /** 客户端 → 服务端：请求应用一份新配置（服务端校验管理员权限后落盘） */
    public static void handleToServer(ConfigSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ConfigSyncHandler.onClientRequest(player, payload.values);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}