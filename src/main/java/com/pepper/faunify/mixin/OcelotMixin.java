package com.pepper.faunify.mixin;

import com.pepper.faunify.entity.MouseEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Ocelot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Ocelot.class)
public abstract class OcelotMixin {

    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void onRegisterGoals(CallbackInfo ci) {
        Ocelot self = (Ocelot)(Object)this;
        self.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(self, MouseEntity.class, true));
    }
}
