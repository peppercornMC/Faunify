package com.pepper.faunify.entity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.OpossumEntity;
import com.pepper.faunify.entity.client.model.OpossumModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import javax.annotation.Nullable;

public class OpossumRenderer extends GeoEntityRenderer<OpossumEntity> {
    private static final ResourceLocation REGULAR = new ResourceLocation(Faunify.MODID, "textures/entity/opossum1.png");
    private static final ResourceLocation LIGHT = new ResourceLocation(Faunify.MODID, "textures/entity/opossum2.png");
    private static final ResourceLocation DARK = new ResourceLocation(Faunify.MODID, "textures/entity/opossum2.png");
    
    private static final ResourceLocation REGULAR_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/opossum1.png");
    private static final ResourceLocation LIGHT_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/opossum2.png");
    private static final ResourceLocation DARK_SLEEPING = new ResourceLocation(Faunify.MODID, "textures/entity/sleep/opossum2.png");
    
    private static final String SNOUT_BONE = "snout";

    public OpossumRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new OpossumModel());
        this.shadowRadius = 0.3f;
        
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Nullable
            @Override
            protected ItemStack getStackForBone(GeoBone bone, OpossumEntity animatable) {
                if (SNOUT_BONE.equals(bone.getName()) && !animatable.getMouthItem().isEmpty()) {
                    return animatable.getMouthItem();
                }
                return null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, OpossumEntity animatable) {
                return SNOUT_BONE.equals(bone.getName()) ? ItemDisplayContext.FIXED : ItemDisplayContext.NONE;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, OpossumEntity animatable,
                                              MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                if (SNOUT_BONE.equals(bone.getName())) {
                    if (animatable.isBaby()) {
                        poseStack.translate(0.0, -0.03, -0.15);
                        poseStack.scale(0.35f, 0.35f, 0.35f);
                    } else {
                        poseStack.translate(0.0, -0.03, -0.15);
                        poseStack.scale(0.35f, 0.35f, 0.35f);
                    }
                    
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                }

                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }
    
    @Override
    public ResourceLocation getTextureLocation(OpossumEntity entity) {
        return getVariantTexture(entity.getVariant(), entity.isSleeping());
    }
    
    public static ResourceLocation getVariantTexture(OpossumEntity.Variant variant, boolean isSleeping) {
        ResourceLocation resourceLocation;
        switch (variant) {
            case REGULAR:
                resourceLocation = isSleeping ? REGULAR_SLEEPING : REGULAR;
                break;
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
    
    public static ResourceLocation getVariantTexture(OpossumEntity.Variant variant) {
        return getVariantTexture(variant, false);
    }
    
    @Override
    public void preRender(PoseStack stack, OpossumEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(stack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        
        if (animatable.isBaby()) {
            float babyScale = 0.45f;
            stack.scale(babyScale, babyScale, babyScale);
        } else {
            float adultScale = 1.1f;
            stack.scale(adultScale, adultScale, adultScale);
        }
    }
}