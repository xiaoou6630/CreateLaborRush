package com.xiaoou.rush.client;

import com.xiaoou.rush.CreateLaborRush;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端入口：把模组注册进"模组列表 → Config"按钮，使用 NeoForge 自带的图形化配置界面。
 */
@Mod(value = CreateLaborRush.MODID, dist = Dist.CLIENT)
public class CreateLaborRushClient {

    public CreateLaborRushClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // 【已停用】挂牌渲染层：写字渲染还有没解决的问题，先不挂这一层。
        // 要恢复就把下面这行加回来，同时恢复 handler.HangingSignHandler / client.HangingSignClientHandler
        // 上的 @EventBusSubscriber 注解。
        // modEventBus.addListener(BodySignRenderers::onAddLayers);
    }
}