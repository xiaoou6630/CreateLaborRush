package com.xiaoou.rush.client;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * 车万女仆(TLM)「GeckoLib 模型」渲染路径下，把本模组的帽子画到女仆头上。
 *
 * <p>TLM 的女仆模型分两条渲染路径：默认 Bedrock 模型走 {@link MaidHatLayer}，
 * 自定义的 GeckoLib 模型走这里（由 {@code GeckoEntityMaidRenderer} 渲染）。
 * 两者互斥，不会重复画。每顶帽子各挂一层，用构造参数区分目标物品、几何部件和贴图。
 *
 * <p>头部坐标系由 TLM 的 {@link RenderUtils#prepMatrixForLocator} 摆好，
 * 与 TLM 自己画头颅方块用的是同一套；变换按 TLM 在该路径下的净变换取（见 render 内注释）。
 *
 * <p>注意：本类直接引用 TLM 的类型，只能在装了 TLM 时被加载（原因见 {@link MaidHatAddon}）。
 */
public class MaidHatGeckoLayer<T extends Mob, R extends IGeoEntityRenderer<T>>
    extends GeoLayerRenderer<T, R> {

    /**
     * 帽子整体缩放。【需实机微调】
     *
     * <p>GeckoLib 模型头部尺寸各不相同，且会带上头部骨骼自身的缩放。
     * 1.0 对应「和默认模型头部一致」的尺寸；若某些模型上帽子偏小/偏大，
     * 直接改这个值即可（这是最可能需要微调的数值）。
     */
    private static final float HAT_SCALE = 1.0F;

    /**
     * 帽子整体上下偏移（单位：方块）。【需实机微调】
     *
     * <p>-y 为上，想往上挪填负值。
     */
    private static final float HAT_OFFSET_Y = 0.0F;

    /** 戴上哪件物品才画这层帽子 */
    private final Item hatItem;
    /** 帽子的几何根部件（来自对应帽子模型） */
    private final ModelPart hatRoot;
    /** 帽子贴图 */
    private final ResourceLocation texture;

    public MaidHatGeckoLayer(R renderer, Item hatItem, ModelPart hatRoot, ResourceLocation texture) {
        super(renderer);
        this.hatItem = hatItem;
        this.hatRoot = hatRoot;
        this.texture = texture;
    }

    @Override
    public GeoLayerRenderer<T, R> copy(R renderer) {
        return new MaidHatGeckoLayer<>(renderer, this.hatItem, this.hatRoot, this.texture);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T maid,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!maid.getItemBySlot(EquipmentSlot.HEAD).is(this.hatItem)) {
            return;
        }
        IGeoEntity geoEntity = this.getGeoEntity(maid);
        if (geoEntity == null) {
            return;
        }
        ILocationModel model = geoEntity.getGeoModel();
        if (model == null) {
            return;
        }
        List<? extends ILocationBone> headBones = model.headBones();
        if (headBones == null || headBones.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        // 摆到头部骨骼（包含头部旋转）
        RenderUtils.prepMatrixForLocator(poseStack, headBones);
        // Y/Z 取反：对齐 TLM 在 Gecko 路径下对原版头颅模型使用的净变换。
        // 推导：TLM 先 scale(-1.1875, 1.1875, -1.1875) + translate(-0.5,0,-0.5)，
        // 随后 SkullBlockRenderer.renderSkull(null,...) 内部再做 translate(0.5,0,0.5) + scale(-1,-1,1)，
        // 平移抵消、缩放叠加后净变换 = scale(1.1875, -1.1875, -1.1875)。
        // 注意符号与 Bedrock 路径（MaidHatLayer）正好相反，这是 TLM 两条渲染路径坐标系不同导致的。
        poseStack.scale(HAT_SCALE, -HAT_SCALE, -HAT_SCALE);
        poseStack.translate(0.0F, HAT_OFFSET_Y, 0.0F);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(this.texture));
        this.hatRoot.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
