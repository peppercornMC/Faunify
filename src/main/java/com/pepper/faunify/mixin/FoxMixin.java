package com.pepper.faunify.mixin;

import com.pepper.faunify.entity.HedgehogEntity;
import com.pepper.faunify.entity.MouseEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(Fox.class)
public abstract class FoxMixin {
    
    @Shadow
    private static EntityDataAccessor<Optional<UUID>> DATA_TRUSTED_ID_0;
    
    @Shadow
    private static EntityDataAccessor<Optional<UUID>> DATA_TRUSTED_ID_1;
    
    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void onRegisterGoals(CallbackInfo ci) {
        Fox self = (Fox)(Object)this;
        
        self.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(self, HedgehogEntity.class, true, (hedgehog) -> {
            if (hedgehog instanceof TamableAnimal tamableHedgehog && tamableHedgehog.isTame()) {
                boolean hasTrustedPlayers = self.getEntityData().get(DATA_TRUSTED_ID_0).isPresent() || 
                                          self.getEntityData().get(DATA_TRUSTED_ID_1).isPresent();
                return !hasTrustedPlayers;
            }
            return true;
        }));
        
        self.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(self, MouseEntity.class, true));
    }
}