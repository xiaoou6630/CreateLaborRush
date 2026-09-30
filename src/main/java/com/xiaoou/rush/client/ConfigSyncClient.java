package com.xiaoou.rush.client;

import com.xiaoou.rush.Config;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.handler.ConfigSyncHandler;
import com.xiaoou.rush.network.ConfigSyncPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端侧的配置同步，仅供客户端代码引用。
 *
 * <p>NeoForge 用的是自带的 ConfigurationScreen，保存动作没法拦截，
 * 所以这里监听配置热重载事件（内置界面保存后 FML 会触发 Reloading），
 * 把本地改动推给服务端做管理员校验。
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = CreateLaborRush.MODID)
public final class ConfigSyncClient {

    private ConfigSyncClient() {}

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != Config.SPEC) return;
        if (!ConfigSyncHandler.markChanged()) return;
        push(Config.current());
    }

    /**
     * 把本地改动推给服务端。只有连着服务器（单人集成服务器或远程服务器）时才发；
     * 在主菜单改配置则只影响本地文件。
     */
    public static void push(Config.Values values) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;
        PacketDistributor.sendToServer(new ConfigSyncPayload(values));
    }
}