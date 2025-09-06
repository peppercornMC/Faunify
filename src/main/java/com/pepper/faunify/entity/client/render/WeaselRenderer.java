package com.pepper.faunify.entity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.WeaselEntity;
import com.pepper.faunify.entity.client.model.WeaselModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WeaselRenderer extends GeoEntityRenderer<WeaselEntity> {
    private static final ResourceLocation STOAT = new ResourceLocation(Faunify.MODID, "textures/entity/weasel1.png");
    private static final ResourceLocation STEPPE = new ResourceLocation(Faunify.MODID, "textures/entity/weasel2.png");
    private static final ResourceLocation EUROPEAN = new ResourceLocation(Faunify.MODID, "textures/entity/weasel3.png");
    private static final ResourceLocation SIBERIAN = new ResourceLocation(Faunify.MODID, "textures/entity/weasel4.png");
    private static final ResourceLocation YELLOWBELLIED = new ResourceLocation(Faunify.MODID, "textures/entity/weasel5.png");
    private static final ResourceLocation FERRETDARK = new ResourceLocation(Faunify.MODID, "textures/entity/ferret1.png");
    private static final ResourceLocation FERRETLIGHT = new ResourceLocation(Faunify.MODID, "textures/entity/ferret2.png");
    
    private static final ResourceLocation STOAT_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/weasel1.png");
    private static final ResourceLocation STEPPE_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/weasel2.png");
    private static final ResourceLocation EUROPEAN_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/weasel3.png");
    private static final ResourceLocation SIBERIAN_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/weasel4.png");
    private static final ResourceLocation YELLOWBELLIED_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/weasel5.png");
    private static final ResourceLocation FERRETDARK_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/ferret1.png");
    private static final ResourceLocation FERRETLIGHT_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/ferret2.png");
    
    private static final ResourceLocation SNOWY = new ResourceLocation(Faunify.MODID, "textures/entity/weasel_snowy.png");
    private static final ResourceLocation SNOWY_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/weasel_snowy.png");

    public WeaselRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WeaselModel());
        this.shadowRadius = 0.5f;
    }
    
    @Override
    public ResourceLocation getTextureLocation(WeaselEntity entity) {
        if (entity.isSnowVariant()) {
            return entity.isSleeping() ? SNOWY_SLEEPING : SNOWY;
        }
        return getVariantTexture(entity.getVariant(), entity.isSleeping());
    }

    public static ResourceLocation getVariantTexture(WeaselEntity.Variant variant, boolean isSleeping) {
        ResourceLocation resourceLocation;
        switch (variant) {
            case STOAT:
                resourceLocation = isSleeping ? STOAT_SLEEPING : STOAT;
                break;
            case STEPPE:
                resourceLocation = isSleeping ? STEPPE_SLEEPING : STEPPE;
                break;
            case EUROPEAN:
                resourceLocation = isSleeping ? EUROPEAN_SLEEPING : EUROPEAN;
                break;
            case SIBERIAN:
                resourceLocation = isSleeping ? SIBERIAN_SLEEPING : SIBERIAN;
                break;
            case YELLOWBELLIED:
                resourceLocation = isSleeping ? YELLOWBELLIED_SLEEPING : YELLOWBELLIED;
                break;
            case FERRETDARK:
                resourceLocation = isSleeping ? FERRETDARK_SLEEPING : FERRETDARK;
                break;
            case FERRETLIGHT:
                resourceLocation = isSleeping ? FERRETLIGHT_SLEEPING : FERRETLIGHT;
                break;
            default:
                throw new IncompatibleClassChangeError();
        }
        return resourceLocation;
    }
    
    public static ResourceLocation getVariantTexture(WeaselEntity.Variant variant) {
        return getVariantTexture(variant, false);
    }
    
    @Override
    public void preRender(PoseStack stack, WeaselEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(stack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        
        if (animatable.isBaby()) {
            float babyScale = 0.65f;
            stack.scale(babyScale, babyScale, babyScale);
        } else {
            float adultScale = 0.95f;
            stack.scale(adultScale, adultScale, adultScale);
        }
    }
}