package com.xiaoou.rush.network;

import com.xiaoou.rush.Config;
import com.xiaoou.rush.handler.ConfigSyncHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 配置全量同步包，双向使用：
 * <ul>
 *   <li>客户端 → 服务端：请求应用一份新配置（服务端会校验管理员权限）</li>
 *   <li>服务端 → 客户端：下发权威配置值（加入服务器时，或管理员改动之后）</li>
 * </ul>
 */
public class ConfigSyncPacket {

    private final Config.Values values;

    public ConfigSyncPacket(Config.Values values) {
        this.values = values;
    }

    public Config.Values values() {
        return values;
    }

    public static void encode(ConfigSyncPacket msg, FriendlyByteBuf buf) {
        Config.Values v = msg.values;
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

    public static ConfigSyncPacket decode(FriendlyByteBuf buf) {
        return new ConfigSyncPacket(new Config.Values(
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readBoolean(), buf.readVarInt(), buf.readDouble(),
            buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
            buf.readVarInt(), buf.readDouble()
        ));
    }

    public static void handle(ConfigSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> ConfigSyncHandler.onClientRequest(context.getSender(), msg.values));
        } else {
            context.enqueueWork(() -> ConfigSyncHandler.onServerBroadcast(msg.values));
        }
        context.setPacketHandled(true);
    }
}