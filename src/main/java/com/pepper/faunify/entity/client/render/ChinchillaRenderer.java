package com.pepper.faunify.entity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.ChinchillaEntity;
import com.pepper.faunify.entity.client.model.ChinchillaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChinchillaRenderer extends GeoEntityRenderer<ChinchillaEntity> {
    private static final ResourceLocation GREY = new ResourceLocation(Faunify.MODID, "textures/entity/chinchilla1.png");
    private static final ResourceLocation BROWN = new ResourceLocation(Faunify.MODID, "textures/entity/chinchilla2.png");
    private static final ResourceLocation BLACK = new ResourceLocation(Faunify.MODID, "textures/entity/chinchilla3.png");
    private static final ResourceLocation WHITE = new ResourceLocation(Faunify.MODID, "textures/entity/chinchilla4.png");
    
    private static final ResourceLocation GREY_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/chinchilla1.png");
    private static final ResourceLocation BROWN_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/chinchilla2.png");
    private static final ResourceLocation BLACK_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/chinchilla3.png");
    private static final ResourceLocation WHITE_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/chinchilla4.png");

    public ChinchillaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ChinchillaModel());
        this.shadowRadius = 0.5f;
    }
    
    @Override
    public ResourceLocation getTextureLocation(ChinchillaEntity entity) {
        return getVariantTexture(entity.getVariant(), entity.isSleeping());
    }
    
    public static ResourceLocation getVariantTexture(ChinchillaEntity.Variant variant, boolean isSleeping) {
        ResourceLocation resourceLocation;
        switch (variant) {
            case GREY:
                resourceLocation = isSleeping ? GREY_SLEEPING : GREY;
                break;
            case BROWN:
                resourceLocation = isSleeping ? BROWN_SLEEPING : BROWN;
                break;
            case BLACK:
                resourceLocation = isSleeping ? BLACK_SLEEPING : BLACK;
                break;
            case WHITE:
                resourceLocation = isSleeping ? WHITE_SLEEPING : WHITE;
                break;
            default:
                throw new IncompatibleClassChangeError();
        }
        return resourceLocation;
    }
    
    public static ResourceLocation getVariantTexture(ChinchillaEntity.Variant variant) {
        return getVariantTexture(variant, false);
    }
     
    @Override
    public void preRender(PoseStack stack, ChinchillaEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(stack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        
        if (animatable.isBaby()) {
            float babyScale = 0.5f;
            stack.scale(babyScale, babyScale, babyScale);
        } else {
            float adultScale = 0.8f;
            stack.scale(adultScale, adultScale, adultScale);
        }
    }
}