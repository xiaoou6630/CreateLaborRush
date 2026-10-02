package com.xiaoou.rush.client;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityMaidRenderer;
import com.xiaoou.rush.ModItems;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * 车万女仆(TLM) 扩展：给女仆挂本模组的帽子渲染层（高帽 / 资本帽各一层）。
 *
 * <p><b>必须走 TLM 的 {@link ILittleMaid} 扩展接口，两个回调都要实现。</b>
 * TLM 的女仆模型有两条互斥渲染路径（已核对 TLM 1.5.3 字节码）：
 * <ul>
 *   <li>Bedrock 模型：{@code EntityMaidRenderer.render} 最后会调 {@code super.render(...)}，
 *       只有这条路才会遍历 {@code RenderLayer} 列表 → 用 {@link #addAdditionMaidLayer}；</li>
 *   <li>Gecko / YSM 模型：{@code EntityMaidRenderer.render} 在调 {@code super.render} 之前
 *       就 {@code return} 了（转交给内部的 {@code GeckoEntityMaidRenderer}），挂在
 *       {@code EntityMaidRenderer} 上的 {@code RenderLayer} 一次都不会执行
 *       → 必须用 {@link #addAdditionGeckoMaidLayer} 挂到那个内部渲染器上。</li>
 * </ul>
 * 注意 {@code GeckoEntityMaidRenderer} 是 {@code EntityMaidRenderer} 的私有字段，
 * 外部拿不到实例，所以 Gecko 路径只能靠 TLM 回调，不能自己从渲染器注册表里找
 * （渲染器注册表里只有 {@code EntityMaidRenderer}）。
 *
 * <p><b>本类怎么被 TLM 发现：</b>不使用 {@code @LittleMaidExtension} 注解，而是在客户端
 * 初始化时手动 {@code TouhouLittleMaid.EXTENSIONS.add(new MaidHatAddon())}
 * （见 {@code MaidHatClientEvents#onClientSetup}）。原因：TLM 的注解扫描
 * {@code AnnotatedInstanceUtil.getInstances} 在专用服务器上同样执行，会对带注解的类
 * 直接 {@code Class.forName}；而本类的方法签名引用客户端渲染器类型
 * （{@code EntityMaidRenderer} 的继承链里有服务端不存在的 {@code RenderLayerParent}），
 * 服务器上必然抛 {@code NoClassDefFoundError}（2026-10-02 服务器日志已实锤：
 * {@code Failed to load: com.xiaoou.rush.client.MaidHatAddon}）。
 * 手动注册只发生在客户端，专用服务器永远不会加载本类。
 *
 * <p>TLM 的 {@code EntityMaidRenderer} / {@code GeckoEntityMaidRenderer} 构造函数里
 * 是直接读 {@code TouhouLittleMaid.EXTENSIONS} 静态字段来遍历回调的，所以只要在渲染器
 * 创建之前（客户端 setup 阶段）把本实例加进列表即可生效。
 */
public final class MaidHatAddon implements ILittleMaid {

    /** 两张帽子贴图都是 64x64，与玩家盔甲层共用 */
    private static final ResourceLocation TALL_HAT_TEXTURE =
        new ResourceLocation("createlaborrush", "textures/models/armor/tall_hat_layer_1.png");
    private static final ResourceLocation CAPITAL_HAT_TEXTURE =
        new ResourceLocation("createlaborrush", "textures/models/armor/capital_hat_layer_1.png");

    public MaidHatAddon() {
    }

    /**
     * 默认 Bedrock 模型路径：往 {@link EntityMaidRenderer} 上挂两层。
     *
     * <p>{@code LivingEntityRenderer.addLayer(...)} 在 Forge 1.20.1 里是 {@code public final}，
     * 可以直接调用（已用 javap 核对 Forge 映射 jar 确认）。
     */
    @Override
    public void addAdditionMaidLayer(EntityMaidRenderer renderer, EntityRendererProvider.Context context) {
        renderer.addLayer(new MaidHatLayer(renderer, ModItems.TALL_HAT.get(),
            TallHatModel.get().getHatRoot(), TALL_HAT_TEXTURE));
        renderer.addLayer(new MaidHatLayer(renderer, ModItems.CAPITAL_HAT.get(),
            CapitalHatModel.get().getHatRoot(), CAPITAL_HAT_TEXTURE));
    }

    /**
     * GeckoLib / YSM 模型路径：往 TLM 内部的 {@link GeckoEntityMaidRenderer} 上挂两层。
     */
    @Override
    public void addAdditionGeckoMaidLayer(GeckoEntityMaidRenderer<? extends Mob> renderer,
                                          EntityRendererProvider.Context context) {
        renderer.addGeoLayerRenderer(new MaidHatGeckoLayer<>(renderer, ModItems.TALL_HAT.get(),
            TallHatModel.get().getHatRoot(), TALL_HAT_TEXTURE));
        renderer.addGeoLayerRenderer(new MaidHatGeckoLayer<>(renderer, ModItems.CAPITAL_HAT.get(),
            CapitalHatModel.get().getHatRoot(), CAPITAL_HAT_TEXTURE));
    }
}
