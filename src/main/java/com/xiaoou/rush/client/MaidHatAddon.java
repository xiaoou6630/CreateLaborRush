package com.xiaoou.rush.client;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityMaidRenderer;
import com.xiaoou.rush.ModItems;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * 车万女仆(TLM) 扩展入口：给女仆注册本模组帽子的渲染层（高帽 + 资本帽各一层）。
 *
 * <p>TLM 的 addon 发现机制是「注解扫描」：它在 {@code CommonRegistry.modApiInit()} 里调用
 * {@code AnnotatedInstanceUtil.getModExtensions()}，遍历所有已加载模组的
 * {@code ModFileScanData} 注解，找出标了 {@link LittleMaidExtension} 的类，
 * 再用 {@code Class.forName(...).newInstance()} 实例化（因此需要一个公开无参构造）。
 * 换句话说：只要本类带上 {@code @LittleMaidExtension} 并实现 {@code ILittleMaid}，
 * TLM 就会自动发现它，我们不需要向任何总线主动注册。
 *
 * <p>这也天然构成了「类隔离」：
 * <ul>
 *   <li>没装 TLM 时，没有任何代码会去加载本类（本模组其它地方完全不引用它），
 *       本类也永远不会被实例化，因此它引用的 TLM 类型不会被解析，
 *       与 TLM 无关的玩家不会因为缺少 TLM 而崩溃；</li>
 *   <li>装了 TLM 时，只有 TLM 会反射加载本类。</li>
 * </ul>
 * 所以这里不需要（也不应该）用 {@code ModList.get().isLoaded(...)} 做判断——
 * 一旦在本类里写这种判断，反而会让入口类在无 TLM 时也被加载。
 */
@LittleMaidExtension
public class MaidHatAddon implements ILittleMaid {

    /** 两张帽子贴图都是 64x64，与玩家盔甲层共用 */
    private static final ResourceLocation TALL_HAT_TEXTURE =
        new ResourceLocation("createlaborrush", "textures/models/armor/tall_hat_layer_1.png");
    private static final ResourceLocation CAPITAL_HAT_TEXTURE =
        new ResourceLocation("createlaborrush", "textures/models/armor/capital_hat_layer_1.png");

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
     * GeckoLib 模型路径：往 {@link GeckoEntityMaidRenderer} 上挂两层。
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