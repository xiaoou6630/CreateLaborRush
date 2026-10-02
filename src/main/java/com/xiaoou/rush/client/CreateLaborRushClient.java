package com.xiaoou.rush.client;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.compat.MaidCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端入口：把模组注册进"模组列表 → Config"按钮，使用 NeoForge 自带的图形化配置界面。
 */
@Mod(value = CreateLaborRush.MODID, dist = Dist.CLIENT)
public class CreateLaborRushClient {

    public CreateLaborRushClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onClientSetup);
        // 【已停用】挂牌渲染层：写字渲染还有没解决的问题，先不挂这一层。
        // 要恢复就把下面这行加回来，同时恢复 handler.HangingSignHandler / client.HangingSignClientHandler
        // 上的 @EventBusSubscriber 注解。
        // modEventBus.addListener(BodySignRenderers::onAddLayers);
    }

    /**
     * 客户端初始化：把女仆帽子扩展注册进 TLM 的扩展列表。
     *
     * <p>为什么手动注册而不是用 TLM 的 {@code @LittleMaidExtension} 注解：注解扫描在
     * 专用服务器上也会执行并 {@code Class.forName} 我们的扩展类，而那个类的方法签名
     * 引用客户端渲染器类型，服务器上会抛 {@code NoClassDefFoundError}。手动注册只走客户端。
     *
     * <p>时机：TLM 在 {@code FMLCommonSetupEvent} 里给 {@code EXTENSIONS} 赋值，
     * 女仆渲染器要到客户端资源重载时才构造，所以客户端 setup 阶段追加一定赶得上。
     */
    private void onClientSetup(FMLClientSetupEvent event) {
        if (!MaidCompat.isLoaded()) {
            return;
        }
        // MaidHatAddon 内部引用 TLM 客户端类型，必须确认 TLM 已加载后才能触碰
        event.enqueueWork(() -> {
            if (TouhouLittleMaid.EXTENSIONS != null) {
                TouhouLittleMaid.EXTENSIONS.add(new MaidHatAddon());
            }
        });
    }
}
