package com.pepper.faunify.mixin;

import com.pepper.faunify.entity.HedgehogEntity;
import com.pepper.faunify.entity.WeaselEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Wolf.class)
public abstract class WolfMixin {
    
    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void onRegisterGoals(CallbackInfo ci) {
        Wolf self = (Wolf)(Object)this;
        
        self.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(self, HedgehogEntity.class, true, (hedgehog) -> {
            if (hedgehog instanceof TamableAnimal tamableHedgehog && tamableHedgehog.isTame() && self.isTame()) {
                return false;
            }
            return true;
        }));
        
        self.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(self, WeaselEntity.class, true, (weasel) -> {
            if (weasel instanceof TamableAnimal tamableWeasel && tamableWeasel.isTame() && self.isTame()) {
                return false;
            }
            return true;
        }));
    }
}