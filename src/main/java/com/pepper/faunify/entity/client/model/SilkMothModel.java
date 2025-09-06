package com.pepper.faunify.entity.client.model;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.SilkMothEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SilkMothModel extends DefaultedEntityGeoModel<SilkMothEntity> {

    public SilkMothModel() {
        super(new ResourceLocation(Faunify.MODID, "silkmoth"), true);
    }

    @Override
    public ResourceLocation getAnimationResource(SilkMothEntity object) {
        return new ResourceLocation(Faunify.MODID, "animations/silkmoth.animation.json");
    }
}
