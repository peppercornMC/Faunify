package com.pepper.faunify.entity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.MouseEntity;
import com.pepper.faunify.entity.client.model.MouseModel;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MouseRenderer extends GeoEntityRenderer<MouseEntity> {
	   private static final ResourceLocation FIELD = new ResourceLocation(Faunify.MODID, "textures/entity/mouse1.png");
	   private static final ResourceLocation GREY = new ResourceLocation(Faunify.MODID, "textures/entity/mouse2.png");
	   private static final ResourceLocation WOOD = new ResourceLocation(Faunify.MODID, "textures/entity/mouse3.png");
	   private static final ResourceLocation WHITE = new ResourceLocation(Faunify.MODID, "textures/entity/mouse4.png");
	   private static final ResourceLocation BLACK = new ResourceLocation(Faunify.MODID, "textures/entity/mouse5.png");
	   private static final ResourceLocation BROWNBELLY = new ResourceLocation(Faunify.MODID, "textures/entity/mouse6.png");
	   private static final ResourceLocation ALBINO = new ResourceLocation(Faunify.MODID, "textures/entity/mousealbino.png");

    public MouseRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MouseModel());
        this.shadowRadius = 0.2f;
    }
    
    @Override
    public ResourceLocation getTextureLocation(MouseEntity entity) {
        return getVariantTexture(entity.getVariant());
    }

     public static ResourceLocation getVariantTexture(MouseEntity.Variant variant) {
        ResourceLocation resourcelocation;
        switch (variant) {
           case FIELD:
              resourcelocation = FIELD;
              break;
           case GREY:
               resourcelocation = GREY;
               break;
           case WOOD:
               resourcelocation = WOOD;
               break;
           case WHITE:
               resourcelocation = WHITE;
               break;
           case BLACK:
               resourcelocation = BLACK;
               break;
           case BROWNBELLY:
               resourcelocation = BROWNBELLY;
               break;
           case ALBINO:
               resourcelocation = ALBINO;
               break;
           default:
              throw new IncompatibleClassChangeError();
        }

        return resourcelocation;
     }
     
     @Override
 	 public void preRender(PoseStack stack, MouseEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
    	super.preRender(stack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

         if (animatable.isBaby()) {
        	 
             float babyScale = 0.5f;
             stack.scale(babyScale, babyScale, babyScale);

             float headScale = 1.3f;
             stack.pushPose();

             model.getBone("head").ifPresent(head -> {
                 head.setScaleX(headScale);
                 head.setScaleY(headScale);
                 head.setScaleZ(headScale);
             });

             stack.popPose();
         } else {
             float adultScale = 0.8f;
             stack.scale(adultScale, adultScale, adultScale);
         }
     }
}
