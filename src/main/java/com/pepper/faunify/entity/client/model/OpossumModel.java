package com.pepper.faunify.entity.client.model;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.OpossumEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class OpossumModel extends DefaultedEntityGeoModel<OpossumEntity> {

    public OpossumModel() {
        super(new ResourceLocation(Faunify.MODID, "opossum"), true);
    }

    @Override
    public ResourceLocation getAnimationResource(OpossumEntity object) {
        return new ResourceLocation(Faunify.MODID, "animations/opossum.animation.json");
    }
    
    @Override
    public void setCustomAnimations(OpossumEntity entity, long uniqueID, AnimationState<OpossumEntity> animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        CoreGeoBone neck = getAnimationProcessor().getBone("head_rotation");
        
        if (head != null) {
            if (entity.isBaby()) {
                float headScale = 1.6f;
                head.setScaleX(headScale);
                head.setScaleY(headScale);
                head.setScaleZ(headScale);
            } else {
                head.setScaleX(1.0f);
                head.setScaleY(1.0f);
                head.setScaleZ(1.0f);
            }
        }
        
        if (!entity.isSleeping()) {
            if (this.turnsHead && head != null) {
                EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

                head.setRotX(entityData.headPitch() * 0.3F * Mth.DEG_TO_RAD);
                head.setRotY(entityData.netHeadYaw() * 0.3F * Mth.DEG_TO_RAD);
            }
        }
        
        if (neck != null && head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            var moving = animationState.isMoving();

            var pitch = entityData.headPitch();
            neck.setRotX(Math.min(20, pitch) * 0.3F * Mth.DEG_TO_RAD);
            if (pitch > 20 && !moving) {
                head.setRotX((pitch - 30) * 0.3F * Mth.DEG_TO_RAD);
            }
            neck.setRotZ(entityData.netHeadYaw() * 0.3F * Mth.DEG_TO_RAD * -0.5F);
        }
    }
}