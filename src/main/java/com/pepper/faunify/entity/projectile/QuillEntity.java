package com.pepper.faunify.entity.projectile;

import com.pepper.faunify.registry.FaunifyEntities;
import com.pepper.faunify.registry.FaunifyItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class QuillEntity extends AbstractArrow {
    
    public QuillEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public QuillEntity(LivingEntity pShooter, Level pLevel) {
        super(FaunifyEntities.QUILL_PROJECTILE.get(), pShooter, pLevel);
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(FaunifyItems.QUILL.get());
    }
    
    public boolean isInGround() {
        return this.inGround;
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        
        // Apply Weakness effect for 15 seconds (300 ticks)
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0)); // 0 = Weakness Level 1
    }

    @Override
    public double getBaseDamage() {
        return super.getBaseDamage() / 1.5; // Reduce damage by 1.5x
    }
}