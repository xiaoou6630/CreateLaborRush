package com.xiaoou.rush.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * 资本帽的模型：黑呢礼帽（帽檐 + 帽带 + 帽筒），几何数据由 Blockbench 导出。
 *
 * <p>结构与 {@link TallHatModel} 完全一致：帽子挂在独立于 head 的 hat_root 上，
 * 渲染时只画 hat_root，并把 head 的姿态复制过来，这样帽子仍会跟着头转。
 *
 * <p>导出代码里 root 带 {@code PartPose.offset(0, 24, 0)}，那是 Blockbench 为了把模型
 * 抬到编辑框里；原版头部方块就在 y ∈ [-8, 0]（原点在颈部），所以这里直接丢掉该偏移，
 * 让 hat_root 挂在 {@code PartPose.ZERO}。
 */
public class CapitalHatModel extends HumanoidModel<LivingEntity> {

    private static CapitalHatModel instance;

    private final ModelPart hatRoot;

    public CapitalHatModel(ModelPart root) {
        super(root);
        this.hatRoot = root.getChild("hat_root");
    }

    /** 首次渲染时再烘焙，避免在客户端初始化之前构造 */
    public static CapitalHatModel get() {
        if (instance == null) {
            instance = new CapitalHatModel(createLayer().bakeRoot());
        }
        return instance;
    }

    /**
     * 供女仆渲染层使用：直接取出烘焙好的帽子部件。
     *
     * <p>它的 PartPose 是 ZERO，所以渲染时不受本模型 head 的影响，
     * 由调用方自己把 PoseStack 摆到女仆头部坐标系即可。
     */
    public ModelPart getHatRoot() {
        return this.hatRoot;
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition hat = mesh.getRoot()
            .addOrReplaceChild("hat_root", CubeListBuilder.create(), PartPose.ZERO);

        // 帽檐：比头略宽一圈，压在头部方块底部（约 y=-9）
        hat.addOrReplaceChild("brim",
            CubeListBuilder.create().texOffs(0, 32).addBox(-7.0F, -9.0F, -7.0F, 14, 1, 14), PartPose.ZERO);
        // 帽带：深红一圈，箍在帽筒根部
        hat.addOrReplaceChild("band",
            CubeListBuilder.create().texOffs(0, 48).addBox(-4.25F, -11.0F, -4.25F, 8.5F, 2, 8.5F), PartPose.ZERO);
        // 帽筒：笔直向上的黑呢筒身
        hat.addOrReplaceChild("crown",
            CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -19.0F, -4.0F, 8, 10, 8), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay, int color) {
        // 跟随头部转动，然后只画帽子
        this.hatRoot.copyFrom(this.head);
        this.hatRoot.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
