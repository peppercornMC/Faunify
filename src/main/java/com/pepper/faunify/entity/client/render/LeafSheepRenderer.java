package com.pepper.faunify.entity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.LeafSheepEntity;
import com.pepper.faunify.entity.client.model.LeafSheepModel;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LeafSheepRenderer extends GeoEntityRenderer<LeafSheepEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Faunify.MODID, "textures/entity/leafsheep.png");

    public LeafSheepRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LeafSheepModel());
        this.shadowRadius = 0.3f;
    }
    
    @Override
    public ResourceLocation getTextureLocation(LeafSheepEntity entity) {
        return TEXTURE;
    }
     
    @Override
    public void preRender(PoseStack stack, LeafSheepEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(stack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        float leafsheepScale = 1f;
        stack.scale(leafsheepScale, leafsheepScale, leafsheepScale);
    }
}
