package com.flowys.client;

import com.flowys.FlowysMod;
import com.flowys.entity.FlowyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FlowyRenderer extends MobRenderer<FlowyEntity, FlowyModel> {
    private static final ResourceLocation[] TEXTURES = {
            tex("flowy_normal"), tex("flowy_happy"), tex("flowy_surprised"), tex("flowy_sleep")
    };

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(FlowysMod.MODID, "textures/entity/flowy/" + name + ".png");
    }

    public FlowyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FlowyModel(ctx.bakeLayer(FlowyModel.LAYER)), 0.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(FlowyEntity entity) {
        int e = entity.getExpression();
        return TEXTURES[Math.max(0, Math.min(e, TEXTURES.length - 1))];
    }

    @Override
    protected void scale(FlowyEntity entity, PoseStack poseStack, float partialTick) {
        float s = entity.isBaby() ? 0.35F : 0.6F;
        poseStack.scale(s, s, s);
    }
}
