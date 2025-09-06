package com.pepper.faunify.entity.client.model;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.FennecEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class FennecModel extends DefaultedEntityGeoModel<FennecEntity> {

    public FennecModel() {
        super(new ResourceLocation(Faunify.MODID, "fennec"), true);
    }

    @Override
    public ResourceLocation getAnimationResource(FennecEntity object) {
        return new ResourceLocation(Faunify.MODID, "animations/fennec.animation.json");
    }
    
    @Override
    public void setCustomAnimations(FennecEntity entity, long uniqueID, AnimationState<FennecEntity> animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        CoreGeoBone neck = getAnimationProcessor().getBone("head_rotation");
        
        if (head != null) {
            if (entity.isBaby()) {
                float headScale = 1.3f;
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
                head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
            }
        }
        
        var moving = animationState.isMoving();
        if (neck != null && head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            var pitch = entityData.headPitch();
            neck.setRotX(Math.min(20, pitch) * Mth.DEG_TO_RAD);
            if (pitch > 20 && !moving) {
                head.setRotX((pitch - 30) * Mth.DEG_TO_RAD);
            }
            neck.setRotZ(entityData.netHeadYaw() * Mth.DEG_TO_RAD * -0.5F);
        }
    }
}