package com.xiaoou.rush.client;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

/**
 * 车万女仆(TLM)「默认 Bedrock 模型」渲染路径下，把本模组的帽子画到女仆头上。
 *
 * <p>TLM 的女仆模型不是 {@code HumanoidModel}，也没有 {@code HumanoidArmorLayer}
 * （已核对 TLM 源码：client/renderer 包里完全没有护甲渲染），所以帽子必须作为独立渲染层挂上去。
 * 每顶帽子（高帽 / 资本帽）各挂一层，用构造参数区分目标物品、几何部件和贴图。
 *
 * <p><b>变换怎么来的：</b>我们的帽子几何建在「原版模型空间」（头部方块 y ∈ [-8,0]，-y 为上），
 * 与原版头颅模型（{@code SkullModel}，8x8x8）是同一套空间。TLM 的
 * {@code LayerMaidBipedHead} 正是把原版头颅模型摆到女仆头上的，所以照抄它对原版头颅模型
 * 施加的<b>净变换</b>，就能把同空间的帽子几何正确摆上去。
 *
 * <p>TLM 那段的完整调用是：
 * <pre>
 *   head.translateAndRotate(poseStack);
 *   poseStack.scale(1.1875F, -1.1875F, -1.1875F);
 *   poseStack.translate(-0.5D, 0.0D, -0.5D);
 *   SkullBlockRenderer.renderSkull(null, 180F, 0F, poseStack, ...);  // 内部还有 translate(0.5,0,0.5) + scale(-1,-1,1)
 * </pre>
 * 其中 {@code translate(-0.5,0,-0.5)} 是给 {@code renderSkull} 的 {@code translate(0.5,0,0.5)} 配套抵消的，
 * <b>我们不用 renderSkull，所以两个平移都要略过</b>。叠加后净变换就是
 * {@code scale(-1.1875, 1.1875, -1.1875)}；把 1.1875（原版给头颅用的"略微放大避免与头方块打架"系数）
 * 换成本模组的 {@link #HAT_SCALE} 即可。
 *
 * <p>注意：本类直接引用 TLM 的类型，因此只能在装了 TLM 时被加载。
 * 它只由 {@link MaidHatAddon} 创建，而 {@code MaidHatAddon} 又只由 TLM 自己
 * 通过注解扫描后反射实例化，所以没装 TLM 时本类永远不会被加载。
 */
public class MaidHatLayer extends RenderLayer<Mob, BedrockModel<Mob>> {

    /**
     * 帽子整体缩放。【需实机微调】
     *
     * <p>1.0 表示「帽子几何按原始像素尺寸贴合女仆头部」（头部方块 8x8x8 像素，与几何基准一致）。
     * 原版给头颅用的是 1.1875，那只是为了让头颅略微大于头方块避免闪面；帽子不需要。
     */
    private static final float HAT_SCALE = 1.0F;

    /**
     * 帽子整体上下偏移（单位：缩放前的模型像素，16 = 1 格）。【需实机微调】
     *
     * <p>本坐标系 -y 为上，想往上挪就填负值。
     */
    private static final float HAT_OFFSET_Y = 0.0F;

    /** 戴上哪件物品才画这层帽子 */
    private final Item hatItem;
    /** 帽子的几何根部件（来自对应帽子模型） */
    private final ModelPart hatRoot;
    /** 帽子贴图（与玩家盔甲层共用同一张 64x64 贴图） */
    private final ResourceLocation texture;

    public MaidHatLayer(RenderLayerParent<Mob, BedrockModel<Mob>> parent, Item hatItem,
                        ModelPart hatRoot, ResourceLocation texture) {
        super(parent);
        this.hatItem = hatItem;
        this.hatRoot = hatRoot;
        this.texture = texture;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Mob maid,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        // 只有头盔位戴着对应的帽子时才画
        if (!maid.getItemBySlot(EquipmentSlot.HEAD).is(this.hatItem)) {
            return;
        }
        BedrockModel<Mob> model = this.getParentModel();
        // 极少数模型没有 "head" 骨骼，此时直接跳过，避免 NPE
        if (!model.hasHead()) {
            return;
        }
        BedrockPart head = model.getHead();

        poseStack.pushPose();
        // 摆到头部骨骼：位置 + 头部旋转都会跟着走，所以帽子会随头转动
        head.translateAndRotate(poseStack);
        // X/Z 取反：对齐 TLM 对原版头颅模型使用的净变换（见类注释推导）
        poseStack.scale(-HAT_SCALE, HAT_SCALE, -HAT_SCALE);
        poseStack.translate(0.0F, HAT_OFFSET_Y, 0.0F);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(this.texture));
        this.hatRoot.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }
}