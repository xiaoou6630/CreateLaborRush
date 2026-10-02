package com.xiaoou.rush.client;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.compat.MaidCompat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端 MOD 事件订阅：把女仆帽子扩展注册进 TLM 的扩展列表。
 *
 * <p>为什么手动注册而不是用 TLM 的 {@code @LittleMaidExtension} 注解：注解扫描
 * （{@code AnnotatedInstanceUtil.getInstances}）在专用服务器上也会执行并对带注解的类
 * 直接 {@code Class.forName}，而我们的扩展类方法签名引用客户端渲染器类型，
 * 服务器上会抛 {@code NoClassDefFoundError}。手动注册只发生在客户端。
 *
 * <p>本类不带任何 TLM 类型引用（TLM 类型只出现在 lambda 体里，未加载 TLM 时不会被解析）；
 * 真正引用 TLM 类型的 {@link MaidHatAddon} 只在 {@code MaidCompat.isLoaded()} 之后才会被触碰。
 */
@Mod.EventBusSubscriber(modid = CreateLaborRush.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaidHatClientEvents {

    private MaidHatClientEvents() {
    }

    /**
     * 客户端初始化：把女仆帽子扩展追加到 TLM 的扩展列表。
     *
     * <p>时机：TLM 在 {@code FMLCommonSetupEvent} 里给 {@code EXTENSIONS} 赋值，
     * 女仆渲染器要到客户端资源重载时才构造，所以客户端 setup 阶段追加一定赶得上。
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!MaidCompat.isLoaded()) {
            return;
        }
        event.enqueueWork(() -> {
            if (TouhouLittleMaid.EXTENSIONS != null) {
                TouhouLittleMaid.EXTENSIONS.add(new MaidHatAddon());
            }
        });
    }
}
