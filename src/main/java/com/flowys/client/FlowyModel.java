package com.flowys.client;

import com.flowys.FlowysMod;
import com.flowys.entity.FlowyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class FlowyModel extends EntityModel<FlowyEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(FlowysMod.MODID, "flowy"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leafL;
    private final ModelPart leafR;

    public FlowyModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leafL = root.getChild("leaf_l");
        this.leafR = root.getChild("leaf_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cabeza: cara (centro) + 4 pétalos rectos
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3F, -9F, -1F, 6F, 6F, 2F)
                .texOffs(16, 0).addBox(-3F, -11F, -1F, 6F, 2F, 2F)
                .texOffs(32, 0).addBox(-3F, -3F, -1F, 6F, 2F, 2F)
                .texOffs(0, 8).addBox(-5F, -9F, -1F, 2F, 6F, 2F)
                .texOffs(8, 8).addBox(3F, -9F, -1F, 2F, 6F, 2F),
                PartPose.offset(0F, 20F, 0F));

        // 4 pétalos diagonales (rotados 45º)
        float r = (float) (Math.PI / 4.0);
        CubeListBuilder diamond = CubeListBuilder.create().texOffs(16, 8).addBox(-1F, -1F, -1F, 2F, 2F, 2F);
        head.addOrReplaceChild("petal_nw", diamond, PartPose.offsetAndRotation(-4F, -10F, 0F, 0F, 0F, r));
        head.addOrReplaceChild("petal_ne", diamond, PartPose.offsetAndRotation(4F, -10F, 0F, 0F, 0F, r));
        head.addOrReplaceChild("petal_sw", diamond, PartPose.offsetAndRotation(-4F, -2F, 0F, 0F, 0F, r));
        head.addOrReplaceChild("petal_se", diamond, PartPose.offsetAndRotation(4F, -2F, 0F, 0F, 0F, r));

        root.addOrReplaceChild("stem", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offset(0F, 24F, 0F));
        root.addOrReplaceChild("leaf_l", CubeListBuilder.create()
                .texOffs(32, 16).addBox(-4F, -2F, -1F, 4F, 2F, 2F), PartPose.offset(-1F, 22F, 0F));
        root.addOrReplaceChild("leaf_r", CubeListBuilder.create()
                .texOffs(32, 16).addBox(0F, -2F, -1F, 4F, 2F, 2F), PartPose.offset(1F, 22F, 0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(FlowyEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        this.head.zRot = (float) Math.sin(ageInTicks * 0.12F) * 0.12F;
        float flap = (float) Math.sin(ageInTicks * 0.25F) * 0.35F;
        this.leafL.zRot = -0.3F + flap;
        this.leafR.zRot = 0.3F - flap;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
    }
}
