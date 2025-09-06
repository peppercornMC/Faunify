package com.pepper.faunify.entity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.FennecEntity;
import com.pepper.faunify.entity.client.model.FennecModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FennecRenderer extends GeoEntityRenderer<FennecEntity> {
    private static final ResourceLocation LIGHT = new ResourceLocation(Faunify.MODID, "textures/entity/fennec1.png");
    private static final ResourceLocation DARK = new ResourceLocation(Faunify.MODID, "textures/entity/fennec2.png");
    
    private static final ResourceLocation LIGHT_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/fennec1.png");
    private static final ResourceLocation DARK_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/fennec2.png");

    public FennecRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FennecModel());
        this.shadowRadius = 0.5f;
    }
    
    @Override
    public ResourceLocation getTextureLocation(FennecEntity entity) {
        return getVariantTexture(entity.getVariant(), entity.isSleeping());
    }
    
    public static ResourceLocation getVariantTexture(FennecEntity.Variant variant, boolean isSleeping) {
        ResourceLocation resourceLocation;
        switch (variant) {
            case LIGHT:
                resourceLocation = isSleeping ? LIGHT_SLEEPING : LIGHT;
                break;
            case DARK:
                resourceLocation = isSleeping ? DARK_SLEEPING : DARK;
                break;
            default:
                throw new IncompatibleClassChangeError();
        }
        return resourceLocation;
    }
    
    public static ResourceLocation getVariantTexture(FennecEntity.Variant variant) {
        return getVariantTexture(variant, false);
    }
     
    @Override
    public void preRender(PoseStack stack, FennecEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(stack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        
        if (animatable.isBaby()) {
            float babyScale = 0.65f;
            stack.scale(babyScale, babyScale, babyScale);
        } else {
            float adultScale = 0.9f;
            stack.scale(adultScale, adultScale, adultScale);
        }
    }
}