package com.xiaoou.rush.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 在实体躯干正前方挂一块木牌，并把该实体身上的 4 行告示牌文字画上去。
 *
 * <p>牌子的位置不用父模型的躯干部件，而是直接往 PoseStack 里塞偏移——玩家和村民的躯干
 * 尺寸不同，用两个子类各自给一组偏移常量就够了，也省得依赖各模型内部部件名。
 *
 * <p>坐标约定（渲染层拿到的 PoseStack 就是实体的"模型空间"）：
 * 原点在颈部、1.0 = 16 像素、+y 向下、-z 是实体正面（原版披风画在 +z 一侧）。
 * 因此这里的偏移常量都以"像素 / 16"给出。
 */
public abstract class BodySignLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    /** 木板贴图：直接用原版橡木木板，省一张自定义贴图 */
    private static final ResourceLocation BOARD_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/block/oak_planks.png");

    /** 边框颜色乘算值：同一张贴图压暗后当作深色边框 */
    private static final int FRAME_COLOR = 0xFF5A4630;
    /** 文字颜色（深色） */
    private static final int TEXT_COLOR = 0xFF1F1F1F;

    /**
     * 文字缩放：1 个字体像素等于多少格。【需实机微调】
     *
     * <p>0.0125 与原版告示牌的文字大小同量级，能让 4 行字塞进 10 像素高的板子里。
     */
    private static final float TEXT_SCALE = 0.0125F;
    /** 原版字体行高（像素） */
    private static final float TEXT_LINE_HEIGHT = 9.0F;
    /**
     * 文字平面相对板子正面再往前挪的距离（模型像素）。【需实机微调】
     *
     * <p>板子 z 范围是 ±0.5 像素，所以这个值必须大于 0.5，否则文字会陷在木板里被挡掉。
     */
    private static final float TEXT_DEPTH = 1.5F;

    /** 板子几何只建一次（与帽子模型一样，第一次渲染时再烘焙） */
    private static ModelPart frame;
    private static ModelPart board;

    /** 板子中心相对躯干原点的上下偏移（格）。【需实机微调】 */
    private final float boardY;
    /** 板子中心相对躯干原点向前（-z）的偏移（格）。【需实机微调】 */
    private final float boardZ;

    protected BodySignLayer(RenderLayerParent<T, M> parent, float boardY, float boardZ) {
        super(parent);
        this.boardY = boardY;
        this.boardZ = boardZ;
    }

    /** 该实体身上挂着的 4 行文字；没挂返回 {@code null} */
    protected abstract String[] getWornLines(T entity);

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        String[] lines = this.getWornLines(entity);
        // 没挂牌、或者牌子是空白的，就别在胸前挂块空木板
        if (lines == null || !hasAnyText(lines)) {
            return;
        }
        buildBoardIfNeeded();

        poseStack.pushPose();
        poseStack.translate(0.0F, this.boardY, this.boardZ);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(BOARD_TEXTURE));
        frame.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, FRAME_COLOR);
        board.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        // 必须先把木板这一批刷出去再画字：文字用的 RenderType 不写深度缓冲，
        // 万一它排在木板前面，后面画的木板会直接盖掉文字像素（而且不会触发 z-fighting，很难查）
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
        renderText(poseStack, buffer, packedLight, lines);
        poseStack.popPose();
    }

    /** 把 4 行文字居中画在板子正面 */
    private static void renderText(PoseStack poseStack, MultiBufferSource buffer, int packedLight, String[] lines) {
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        // 再往前挪一丁点：贴面画会与木板 z-fighting
        poseStack.translate(0.0F, 0.0F, -TEXT_DEPTH / 16.0F);
        // 模型空间本身就是 +y 向下，与字体坐标系同向，所以 Y 取正；
        // X/Z 取负是为了让文字面朝实体正前方（-z）——只把 Z 取负会变成镜像（行列式 < 0），
        // 面片绕序被翻过来会被字体的背面剔除吃掉，屏幕上就一个字都看不见。
        poseStack.scale(-TEXT_SCALE, TEXT_SCALE, -TEXT_SCALE);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isEmpty()) {
                continue;
            }
            float x = -font.width(line) / 2.0F;
            float y = (i - lines.length / 2.0F) * TEXT_LINE_HEIGHT;
            font.drawInBatch(line, x, y, TEXT_COLOR, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, packedLight);
        }
        poseStack.popPose();
    }

    private static boolean hasAnyText(String[] lines) {
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void buildBoardIfNeeded() {
        if (board != null) {
            return;
        }
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 边框：比板子宽一点、但更薄，于是从正面看只露出板子四周一圈深色边
        root.addOrReplaceChild("frame",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.4F, -5.4F, -0.4F, 8.8F, 10.8F, 0.8F), PartPose.ZERO);
        // 板面 8x10x1
        root.addOrReplaceChild("board",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.0F, -0.5F, 8.0F, 10.0F, 1.0F), PartPose.ZERO);

        ModelPart baked = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        frame = baked.getChild("frame");
        board = baked.getChild("board");
    }
}
