package com.pepper.faunify.entity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.RingtailCatEntity;
import com.pepper.faunify.entity.client.model.RingtailCatModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RingtailCatRenderer extends GeoEntityRenderer<RingtailCatEntity> {
    private static final ResourceLocation REGULAR = new ResourceLocation(Faunify.MODID, "textures/entity/ringtailcat1.png");
    private static final ResourceLocation BROWN = new ResourceLocation(Faunify.MODID, "textures/entity/ringtailcat1.png");
    private static final ResourceLocation GREY = new ResourceLocation(Faunify.MODID, "textures/entity/ringtailcat1.png");
    
    private static final ResourceLocation REGULAR_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/ringtailcat1.png");
    private static final ResourceLocation BROWN_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/ringtailcat1.png");
    private static final ResourceLocation GREY_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/ringtailcat1.png");
    
    public RingtailCatRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RingtailCatModel());
        this.shadowRadius = 0.5f;
    }
    
    @Override
    public ResourceLocation getTextureLocation(RingtailCatEntity entity) {
        return getVariantTexture(entity.getVariant(), entity.isSleeping());
    }
    
    public static ResourceLocation getVariantTexture(RingtailCatEntity.Variant variant, boolean isSleeping) {
        ResourceLocation resourceLocation;
        switch (variant) {
            case REGULAR:
                resourceLocation = isSleeping ? REGULAR_SLEEPING : REGULAR;
                break;
            case BROWN:
                resourceLocation = isSleeping ? BROWN_SLEEPING : BROWN;
                break;
            case GREY:
                resourceLocation = isSleeping ? GREY_SLEEPING : GREY;
                break;
            default:
                throw new IncompatibleClassChangeError();
        }
        return resourceLocation;
    }
    
    public static ResourceLocation getVariantTexture(RingtailCatEntity.Variant variant) {
        return getVariantTexture(variant, false);
    }
    
    @Override
    public void render(RingtailCatEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isBaby()) {
            float babyScale = 0.5f;
            poseStack.scale(babyScale, babyScale, babyScale);
        } else {
            float adultScale = 0.9f;
            poseStack.scale(adultScale, adultScale, adultScale);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}