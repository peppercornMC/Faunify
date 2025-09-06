package com.pepper.faunify.entity.client.model;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.HedgehogEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class HedgehogModel extends DefaultedEntityGeoModel<HedgehogEntity> {

    public HedgehogModel() {
        super(new ResourceLocation(Faunify.MODID, "hedgehog"), true);
    }

    @Override
    public ResourceLocation getAnimationResource(HedgehogEntity object) {
        return new ResourceLocation(Faunify.MODID, "animations/hedgehog.animation.json");
    }
    
    @Override
    public void setCustomAnimations(HedgehogEntity entity, long uniqueID, AnimationState<HedgehogEntity> animationState) {
        if (!entity.isSleeping() && !entity.isCurledUp()) {
            if (!this.turnsHead) {
                return;
            }

            CoreGeoBone head = getAnimationProcessor().getBone("head");

            if (head != null) {
                EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

                head.setRotX(entityData.headPitch() * 0.3F * Mth.DEG_TO_RAD);
                head.setRotY(entityData.netHeadYaw() * 0.3F * Mth.DEG_TO_RAD);
            }
        }

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        CoreGeoBone neck = getAnimationProcessor().getBone("head_rotation");
        var moving = animationState.isMoving();

        if (neck != null && head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            var pitch = entityData.headPitch();
            neck.setRotX(Math.min(20, pitch) * 0.3F * Mth.DEG_TO_RAD);
            if (pitch > 20 && !moving) {
                head.setRotX((pitch - 30) * 0.3F * Mth.DEG_TO_RAD);
            }
            neck.setRotZ(entityData.netHeadYaw() * 0.3F * Mth.DEG_TO_RAD * -0.5F);
        }
    }
}
