package com.pepper.faunify.mixin;

import com.pepper.faunify.entity.MouseEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Cat.class)
public abstract class CatMixin {

    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void onRegisterGoals(CallbackInfo ci) {
        Cat self = (Cat)(Object)this;
        self.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(self, MouseEntity.class, true));
    }
}
