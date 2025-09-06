package com.pepper.faunify.entity.client.model;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.MouseEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class MouseModel extends DefaultedEntityGeoModel<MouseEntity> {

    public MouseModel() {
        super(new ResourceLocation(Faunify.MODID, "mouse"), true);
    }

    @Override
    public ResourceLocation getAnimationResource(MouseEntity object) {
        return new ResourceLocation(Faunify.MODID, "animations/mouse.animation.json");
    }
    
    @Override
    public void setCustomAnimations(MouseEntity entity, long uniqueID, AnimationState<MouseEntity> animationState) {
        if (!this.turnsHead)
            return;

        CoreGeoBone head = getAnimationProcessor().getBone("body");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }

        CoreGeoBone neck = getAnimationProcessor().getBone("body_rotation");
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
