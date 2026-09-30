package com.xiaoou.rush.handler;

import com.xiaoou.rush.Config;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.client.ConfigSyncClient;
import com.xiaoou.rush.network.ConfigSyncPacket;
import com.xiaoou.rush.network.LaborNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 配置的权限控制与同步。
 *
 * <p>规则：配置以服务端的值为权威。客户端改配置时把全量值推给服务端，服务端校验管理员权限后
 * 落盘并广播；非管理员会被拒绝，并收到服务端当前的真实值（界面因此会自动回滚到真实值）。
 *
 * <p><b>客户端永不落盘</b>：Forge/NeoForge 的配置文件都开了 nightconfig 的 autosave，
 * 对配置项调用 {@code set()} 就会立即写盘。所以服务端下发的值只存一份 {@link #displayValues}
 * 供界面显示，绝不写进配置项。客户端的 toml 只有一种情况会被写入——没连服务器时，
 * 玩家自己在配置界面里点"完成"。
 */
public final class ConfigSyncHandler {

    /** 最近一次已知的配置值，用于去重：避免同一次改动被重复推给服务端 */
    private static Config.Values lastKnown;

    /**
     * 服务端权威值（或本地刚提交、等待服务端裁决的值），<b>仅用于界面显示</b>。
     * 它不会写进配置项、也不会落盘——客户端的 toml 永远只由玩家自己编辑。
     */
    private static Config.Values displayValues;

    private ConfigSyncHandler() {}

    /**
     * 服务端把一份配置落盘（不含权限校验）。
     * 只有服务端会持久化配置：Forge 的配置文件开了 nightconfig 的 autosave，
     * 一旦对配置项调用 set() 就会写盘，所以客户端那边一律不碰配置项。
     */
    public static void applyLocalAndSave(Config.Values values) {
        lastKnown = Config.applyInMemory(values);
        Config.save();
    }

    /** 服务端：处理客户端发来的改配置请求 */
    public static void onClientRequest(ServerPlayer player, Config.Values values) {
        if (player == null) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (!canEditConfig(server, player)) {
            player.sendSystemMessage(Component.translatable("chat.createlaborrush.config.no_permission"));
            // 拒绝后把服务端权威值发回去，客户端界面自动回滚
            LaborNetworking.sendConfigTo(player, new ConfigSyncPacket(Config.current()));
            return;
        }

        applyLocalAndSave(values);
        broadcast(server);
        player.sendSystemMessage(Component.translatable("chat.createlaborrush.config.updated"));
    }

    /** 客户端：收到服务端下发的权威配置，只记下来给界面显示 */
    public static void onServerBroadcast(Config.Values values) {
        rememberForDisplay(values);
    }

    /** 记下一份"当前应当显示"的配置值，供客户端界面读取 */
    public static void rememberForDisplay(Config.Values values) {
        displayValues = values;
    }

    /** 界面可用的权威值；返回 null 表示应回退到本地配置 */
    public static Config.Values syncedValues() {
        return displayValues;
    }

    /** 服务端：把当前配置广播给所有在线玩家 */
    public static void broadcast(MinecraftServer server) {
        ConfigSyncPacket packet = new ConfigSyncPacket(Config.current());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            LaborNetworking.sendConfigTo(player, packet);
        }
    }

    /**
     * 管理员判定：拥有 OP 权限（等级 2），或者是单人/局域网世界的房主
     * （房主在未开作弊时权限等级可能不到 2，但配置理应归他管）。
     */
    private static boolean canEditConfig(MinecraftServer server, ServerPlayer player) {
        return player.hasPermissions(2) || server.isSingleplayerOwner(player.getGameProfile());
    }

    @Mod.EventBusSubscriber(modid = CreateLaborRush.MODID)
    public static class Events {

        /** 首次加载时记录基线，避免后续把"未变化"误判成改动 */
        @SubscribeEvent
        public static void onConfigLoading(ModConfigEvent.Loading event) {
            if (event.getConfig().getSpec() != Config.SPEC) return;
            lastKnown = Config.current();
        }

        /**
         * 配置热重载：改配置文件、或在配置界面保存都会触发。
         * 客户端推给服务端；专用服务器（管理员直接改配置文件）则广播给所有客户端。
         */
        @SubscribeEvent
        public static void onConfigReloading(ModConfigEvent.Reloading event) {
            if (event.getConfig().getSpec() != Config.SPEC) return;

            Config.Values now = Config.current();
            if (now.equals(lastKnown)) return;
            lastKnown = now;

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConfigSyncClient.push(now));

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && server.isDedicatedServer()) {
                broadcast(server);
            }
        }

        /** 玩家进服时下发一次权威配置，保证界面显示与服务端一致 */
        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            LaborNetworking.sendConfigTo(player, new ConfigSyncPacket(Config.current()));
        }
    }
}