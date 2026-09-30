package com.xiaoou.rush.handler;

import com.xiaoou.rush.Config;
import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * 配置的权限控制与同步（服务端部分）。
 *
 * <p>配置以服务端的值为权威：客户端改配置时把全量值推给服务端（见 client.ConfigSyncClient），
 * 服务端校验管理员权限后才落盘；非管理员会被拒绝并收到聊天提示。
 *
 * <p>没有"服务端 → 客户端"的下发方向：客户端的 toml 永远只由玩家自己编辑。NeoForge 与
 * Forge 的配置文件都开了 nightconfig 的 autosave，对配置项调用 {@code set()} 会立即写盘，
 * 所以客户端一旦接收服务端值就必然污染本地文件，只能不发。
 */
public final class ConfigSyncHandler {

    /** 最近一次已知的本地配置快照，用于去重：避免同一次改动被重复推给服务端 */
    private static Config.Values lastKnown;

    private ConfigSyncHandler() {}

    /** 服务端把一份配置落盘（不含权限校验） */
    public static void applyLocalAndSave(Config.Values values) {
        lastKnown = Config.applyInMemory(values);
        Config.save();
    }

    /** 去重：返回 true 表示本地配置确实发生了新变化 */
    public static boolean markChanged() {
        Config.Values now = Config.current();
        if (now.equals(lastKnown)) return false;
        lastKnown = now;
        return true;
    }

    /** 服务端：处理客户端发来的改配置请求 */
    public static void onClientRequest(ServerPlayer player, Config.Values values) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (!canEditConfig(server, player)) {
            player.sendSystemMessage(Component.translatable("chat.createlaborrush.config.no_permission"));
            return;
        }

        applyLocalAndSave(values);
        player.sendSystemMessage(Component.translatable("chat.createlaborrush.config.updated"));
    }

    /**
     * 管理员判定：拥有 OP 权限（等级 2），或者是单人/局域网世界的房主
     * （房主在未开作弊时权限等级可能不到 2，但配置理应归他管）。
     */
    private static boolean canEditConfig(MinecraftServer server, ServerPlayer player) {
        return player.hasPermissions(2) || server.isSingleplayerOwner(player.getGameProfile());
    }

    @EventBusSubscriber(modid = CreateLaborRush.MODID)
    public static class Events {

        /** 首次加载时记录基线，避免后续把"未变化"误判成改动 */
        @SubscribeEvent
        public static void onConfigLoading(ModConfigEvent.Loading event) {
            if (event.getConfig().getSpec() != Config.SPEC) return;
            lastKnown = Config.current();
        }
    }
}