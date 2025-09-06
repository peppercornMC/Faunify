package com.pepper.faunify.entity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.SilkMothEntity;
import com.pepper.faunify.entity.client.model.SilkMothModel;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SilkMothRenderer extends GeoEntityRenderer<SilkMothEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Faunify.MODID, "textures/entity/silkmoth.png");

    public SilkMothRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SilkMothModel());
        this.shadowRadius = 0.2f;
    }
    
    @Override
    public ResourceLocation getTextureLocation(SilkMothEntity entity) {
        return TEXTURE;
    }
     
    @Override
    public void preRender(PoseStack stack, SilkMothEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(stack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        float mothScale = 0.8f;
        stack.scale(mothScale, mothScale, mothScale);
    }
}
