package com.pepper.faunify.entity.client.render;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.projectile.QuillEntity;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class QuillRenderer extends ArrowRenderer<QuillEntity> {
    public QuillRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public ResourceLocation getTextureLocation(QuillEntity pEntity) {
        return new ResourceLocation(Faunify.MODID, "textures/entity/projectiles/quill.png");
    }
}
