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
 * 批斗大会高帽的模型：白纸圆锥帽，由若干层方块自下而上收窄叠出来。
 *
 * <p>原版头盔层只是在头部方块外面套一层，画不出高过头的尖帽，所以走自定义模型：
 * 把帽子挂在独立于 head 的 hat_root 上，渲染时只画 hat_root，
 * 并把 head 的姿态复制过来，这样帽子仍会跟着头转。
 */
public class TallHatModel extends HumanoidModel<LivingEntity> {

    private static TallHatModel instance;

    private final ModelPart hatRoot;

    public TallHatModel(ModelPart root) {
        super(root);
        this.hatRoot = root.getChild("hat_root");
    }

    /** 首次渲染时再烘焙，避免在客户端初始化之前构造 */
    public static TallHatModel get() {
        if (instance == null) {
            instance = new TallHatModel(createLayer().bakeRoot());
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

        // 帽檐（头部方块底部约 y=-8）
        hat.addOrReplaceChild("brim",
            CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -9.0F, -6.0F, 12, 1, 12), PartPose.ZERO);
        // 帽身：自下而上逐层收窄
        hat.addOrReplaceChild("cone1",
            CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -12.0F, -5.0F, 10, 3, 10), PartPose.ZERO);
        hat.addOrReplaceChild("cone2",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -15.0F, -4.0F, 8, 3, 8), PartPose.ZERO);
        hat.addOrReplaceChild("cone3",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -18.0F, -3.0F, 6, 3, 6), PartPose.ZERO);
        hat.addOrReplaceChild("cone4",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -21.0F, -2.0F, 4, 3, 4), PartPose.ZERO);
        hat.addOrReplaceChild("tip",
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -23.0F, -1.0F, 2, 2, 2), PartPose.ZERO);

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