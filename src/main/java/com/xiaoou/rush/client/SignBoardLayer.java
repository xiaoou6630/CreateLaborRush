package com.xiaoou.rush.client;

import com.xiaoou.rush.util.ClientSignBoardCache;
import com.xiaoou.rush.util.SignBoardData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;

import java.util.function.Function;
import org.joml.Matrix4f;

/**
 * 把「牌子」画到实体胸口：一块木板 + 4 行告示牌文字。
 *
 * <p>只要某位玩家胸甲槽里挂着悬挂告示牌、或某个工人在持久化数据里挂着牌子，
 * 就会在躯干正前方画出来。玩家和村民共用这一层，取文字的方式通过构造参数注入。
 *
 * <p>玩家的牌子直接读胸甲槽物品（物品 NBT 本来就会同步）；
 * 村民的牌子存在服务端的持久化数据里、客户端拿不到，所以服务端会另发一份同步包，
 * 这里读 {@link ClientSignBoardCache}。
 *
 * <p>画笔用的木板是运行时现搓的 {@link ModelPart}（不是任何实体模型的一部分），
 * 所以它只受 PoseStack 影响，和实体模型长什么样无关。
 */
public class SignBoardLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    /** 木板贴图用原版木板，省得再打包一张只有几像素的图 */
    private static final ResourceLocation BOARD_TEXTURE =
        new ResourceLocation("minecraft", "textures/block/oak_planks.png");

    /**
     * 木板在模型空间里的位置和尺寸（单位：像素，16 像素 = 1 格）。【需实机微调】
     *
     * <p>模型里 +Y 向下、-Z 才是实体正前方；躯干占 x∈[-4,4]、y∈[0,12]、z∈[-2,2]，
     * 所以木板放在 z=-3..-2，正好贴在胸口外面、两只胳膊之间。
     */
    private static final float BOARD_MIN_X = -4.0F;
    private static final float BOARD_MIN_Y = 1.0F;
    private static final float BOARD_MIN_Z = -3.0F;
    private static final int BOARD_WIDTH = 8;
    private static final int BOARD_HEIGHT = 10;
    private static final int BOARD_DEPTH = 1;

    /** 文字绘制平面的各项偏移与缩放（模型像素）。【需实机微调】 */
    private static final float TEXT_PLANE_Z = -4.0F;
    /** 文字块竖直中心：木板中心 = BOARD_MIN_Y + BOARD_HEIGHT / 2 */
    private static final float TEXT_CENTER_Y = BOARD_MIN_Y + BOARD_HEIGHT / 2.0F;
    /** 1 个 Font 单位折算成多少模型像素，决定文字大小 */
    private static final float TEXT_PIXEL_SCALE = 0.22F;
    /** Font 的行距（Font 单位） */
    private static final float TEXT_LINE_STEP = 9.0F;
    /** 文字颜色：深色，压在木板上看得清 */
    private static final int TEXT_COLOR = 0x2B2320;

    /**
     * 村民专用的额外前移量（模型像素，负值 = 更靠前）。【需实机微调】
     *
     * <p>村民的双臂是抱在胸前的，胳膊能伸到 z≈-6 像素，木板放在玩家那套位置会被手挡住，
     * 所以给村民再往前推一点。
     */
    private static final float VILLAGER_PUSH_FORWARD = -3.0F;

    /** 懒得为一块板子建模型类，直接烘焙一次留着用 */
    private static ModelPart board;

    private final Function<T, String[]> linesProvider;

    public SignBoardLayer(RenderLayerParent<T, M> parent, Function<T, String[]> linesProvider) {
        super(parent);
        this.linesProvider = linesProvider;
    }

    /** 玩家：文字写在胸甲槽那件告示牌上 */
    public static String[] playerLines(AbstractClientPlayer player) {
        return SignBoardData.readFromItem(player.getItemBySlot(EquipmentSlot.CHEST));
    }

    /** 工人：文字由服务端同步过来，客户端缓存在 {@link ClientSignBoardCache} 里 */
    public static String[] villagerLines(Villager villager) {
        return ClientSignBoardCache.get(villager.getId());
    }

    private static ModelPart boardPart() {
        if (board == null) {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("board",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(BOARD_MIN_X, BOARD_MIN_Y, BOARD_MIN_Z, BOARD_WIDTH, BOARD_HEIGHT, BOARD_DEPTH),
                PartPose.ZERO);
            // 贴图是 16x16，但一块 8x10x1 的方块展开需要 18x11 的 UV 空间，
            // 所以把「声明尺寸」放大到 32x32，让 UV 全部落在真实贴图范围内
            board = LayerDefinition.create(mesh, 32, 32).bakeRoot();
        }
        return board;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        String[] lines = this.linesProvider.apply(entity);
        if (lines == null) {
            return;
        }

        poseStack.pushPose();
        if (entity instanceof Villager) {
            poseStack.translate(0.0F, 0.0F, VILLAGER_PUSH_FORWARD / 16.0F);
        }
        boardPart().getChild("board").render(poseStack,
            buffer.getBuffer(RenderType.entityCutoutNoCull(BOARD_TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);

        // 必须先把木板这一批刷出去再画字：文字用的 RenderType 不写深度缓冲，
        // 万一它排在木板前面，后面画的木板会直接盖掉文字像素（而且不会触发 z-fighting，很难查）
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }

        // 摆到木板正面中心：X/Z 取负、Y 取正，这样文字正对实体前方（-z）且不会左右镜像。
        // 注意只能"X、Z 同时取负"：只把 Z 取负的话是镜像（行列式 < 0），
        // 面片绕序被翻过来会被字体的背面剔除吃掉，屏幕上一个字都看不见。
        poseStack.translate(0.0F, TEXT_CENTER_Y / 16.0F, TEXT_PLANE_Z / 16.0F);
        float scale = TEXT_PIXEL_SCALE / 16.0F;
        poseStack.scale(-scale, scale, -scale);

        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = poseStack.last().pose();
        float firstLineY = -SignBoardData.LINES * TEXT_LINE_STEP / 2.0F;
        for (int i = 0; i < SignBoardData.LINES; i++) {
            FormattedCharSequence text = Component.literal(lines[i]).getVisualOrderText();
            float x = -font.width(text) / 2.0F;
            font.drawInBatch(text, x, firstLineY + i * TEXT_LINE_STEP, TEXT_COLOR, false,
                matrix, buffer, Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        }
        poseStack.popPose();
    }
}
