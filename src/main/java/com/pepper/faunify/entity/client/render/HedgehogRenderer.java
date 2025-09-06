package com.pepper.faunify.entity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.HedgehogEntity;
import com.pepper.faunify.entity.client.model.HedgehogModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HedgehogRenderer extends GeoEntityRenderer<HedgehogEntity> {
    private static final ResourceLocation LIGHT = new ResourceLocation(Faunify.MODID, "textures/entity/hedgehog1.png");
    private static final ResourceLocation DARK = new ResourceLocation(Faunify.MODID, "textures/entity/hedgehog2.png");
    private static final ResourceLocation ALBINO = new ResourceLocation(Faunify.MODID, "textures/entity/hedgehog3.png");
    private static final ResourceLocation SONIC = new ResourceLocation(Faunify.MODID, "textures/entity/hedgehog_sonic.png");
    private static final ResourceLocation SHADOW = new ResourceLocation(Faunify.MODID, "textures/entity/hedgehog_shadow.png");
    
    private static final ResourceLocation LIGHT_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/hedgehog1.png");
    private static final ResourceLocation DARK_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/hedgehog2.png");
    private static final ResourceLocation ALBINO_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/hedgehog3.png");
    private static final ResourceLocation SONIC_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/hedgehog_sonic.png");
    private static final ResourceLocation SHADOW_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/hedgehog_shadow.png");

    public HedgehogRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HedgehogModel());
        this.shadowRadius = 0.3f;
    }
    
    @Override
    public ResourceLocation getTextureLocation(HedgehogEntity entity) {
        if ("Sonic".equals(entity.getName().getString())) {
            return entity.isSleeping() ? SONIC_SLEEPING : SONIC;
        }
        if ("Shadow".equals(entity.getName().getString())) {
            return entity.isSleeping() ? SHADOW_SLEEPING : SHADOW;
        }
        
        return getVariantTexture(entity.getVariant(), entity.isSleeping());
    }
    
    public static ResourceLocation getVariantTexture(HedgehogEntity.Variant variant, boolean isSleeping) {
        ResourceLocation resourceLocation;
        switch (variant) {
            case LIGHT:
                resourceLocation = isSleeping ? LIGHT_SLEEPING : LIGHT;
                break;
            case DARK:
                resourceLocation = isSleeping ? DARK_SLEEPING : DARK;
                break;
            case ALBINO:
                resourceLocation = isSleeping ? ALBINO_SLEEPING : ALBINO;
                break;
            default:
                throw new IncompatibleClassChangeError();
        }
        return resourceLocation;
    }
    
    public static ResourceLocation getVariantTexture(HedgehogEntity.Variant variant) {
        return getVariantTexture(variant, false);
    }
    
   @Override
   public void preRender(PoseStack stack, HedgehogEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
       super.preRender(stack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
       
        if (animatable.isBaby()) {
            float babyScale = 0.6f;
            stack.scale(babyScale, babyScale, babyScale);
        } else {
            float adultScale = 0.9f;
            stack.scale(adultScale, adultScale, adultScale);
        }
    }
}