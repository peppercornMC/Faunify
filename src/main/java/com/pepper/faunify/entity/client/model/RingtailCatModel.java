package com.pepper.faunify.entity.client.model;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.RingtailCatEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class RingtailCatModel extends DefaultedEntityGeoModel<RingtailCatEntity> {
    public RingtailCatModel() {
        super(new ResourceLocation(Faunify.MODID, "ringtailcat"), true);
    }
    
    @Override
    public ResourceLocation getAnimationResource(RingtailCatEntity object) {
        return new ResourceLocation(Faunify.MODID, "animations/ringtailcat.animation.json");
    }
    
    @Override
    public void setCustomAnimations(RingtailCatEntity entity, long instanceId, AnimationState<RingtailCatEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        
        if (animationState == null) return;
        
        EntityModelData extraDataOfType = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        
        if (head != null) {
            if (entity.isBaby()) {
                head.setScaleX(1.4F);
                head.setScaleY(1.4F);
                head.setScaleZ(1.4F);
            } else {
                head.setScaleX(1.0F);
                head.setScaleY(1.0F);
                head.setScaleZ(1.0F);
            }
            
            if (!entity.isSleeping()) {
                if (this.turnsHead) {
                    head.setRotX(extraDataOfType.headPitch() * 0.3F * Mth.DEG_TO_RAD);
                    head.setRotY(extraDataOfType.netHeadYaw() * 0.3F * Mth.DEG_TO_RAD);
                }
            }
        }
        
        CoreGeoBone neck = getAnimationProcessor().getBone("head_rotation");
        var moving = animationState.isMoving();
        
        if (neck != null && head != null && !entity.isSleeping()) {
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