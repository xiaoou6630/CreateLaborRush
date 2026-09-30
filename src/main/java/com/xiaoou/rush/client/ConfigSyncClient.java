package com.xiaoou.rush.client;

import com.xiaoou.rush.Config;
import com.xiaoou.rush.handler.ConfigSyncHandler;
import com.xiaoou.rush.network.ConfigSyncPacket;
import com.xiaoou.rush.network.LaborNetworking;
import net.minecraft.client.Minecraft;

/**
 * 客户端侧的配置同步，仅供客户端代码引用。
 */
public final class ConfigSyncClient {

    private ConfigSyncClient() {}

    /**
     * 配置界面点"完成"时调用。
     * <ul>
     *   <li>没连服务器：这就是玩家自己的配置，直接写进本地文件。</li>
     *   <li>连着服务器：本地不落盘，只把值记下来给界面显示，交给服务端裁决
     *       （服务端会校验管理员权限，并把权威值广播回来）。</li>
     * </ul>
     */
    public static void applyFromScreen(Config.Values values) {
        if (Minecraft.getInstance().getConnection() == null) {
            ConfigSyncHandler.applyLocalAndSave(values);
            return;
        }
        ConfigSyncHandler.rememberForDisplay(values);
        push(values);
    }

    /**
     * 界面应当显示的值：连着服务器时显示服务端权威值，否则显示本地配置文件的值。
     */
    public static Config.Values displayValues() {
        Config.Values synced = ConfigSyncHandler.syncedValues();
        if (synced != null && Minecraft.getInstance().getConnection() != null) {
            return synced;
        }
        return Config.current();
    }

    /**
     * 把本地改动推给服务端。只有连着服务器（单人集成服务器或远程服务器）时才发；
     * 在主菜单改配置则只影响本地文件。
     */
    public static void push(Config.Values values) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;
        LaborNetworking.CHANNEL.sendToServer(new ConfigSyncPacket(values));
    }
}