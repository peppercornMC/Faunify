package com.pepper.faunify.entity.projectile;

import com.pepper.faunify.registry.FaunifyEntities;
import com.pepper.faunify.registry.FaunifyItems;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DustPowderEntity extends ThrowableItemProjectile {
	
    public DustPowderEntity(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public DustPowderEntity(Level pLevel) {
        super(FaunifyEntities.DUST_POWDER_PROJECTILE.get(), pLevel);
    }

    public DustPowderEntity(Level pLevel, LivingEntity livingEntity) {
        super(FaunifyEntities.DUST_POWDER_PROJECTILE.get(), livingEntity, pLevel);
    }

    @Override
    protected Item getDefaultItem() {
        return FaunifyItems.DUST_POWDER.get();
    }
    
    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId == 3) {
            ParticleOptions particleOptions = ParticleTypes.POOF;

            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(particleOptions, this.getX(), this.getY(), this.getZ(),
                                         0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        
        if (result.getType() == HitResult.Type.BLOCK) {
                BlockParticleOption gravelParticle = new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState());

                for (int i = 0; i < 8; i++) {
                    this.level().addParticle(gravelParticle, 
                        this.getX(), 
                        this.getY(), 
                        this.getZ(), 
                        (Math.random() - 0.5) * 0.1, 
                        (Math.random() - 0.5) * 0.1, 
                        (Math.random() - 0.5) * 0.1);
                }
            }
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof Mob mob) {
            mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
            mob.setTarget(null);

            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));

            mob.setTarget(null);

            this.level().broadcastEntityEvent(this, (byte) 3);

            mob.hurt(this.damageSources().thrown(this, this.getOwner()), 0); 

            double knockbackStrength = 0.1;
            Vec3 direction = this.getDeltaMovement().normalize();
            mob.push(direction.x * knockbackStrength, 0.05, direction.z * knockbackStrength);

            this.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        this.discard();
    }
}