package com.pepper.faunify.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntFunction;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.Animation;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.pepper.faunify.particle.FaunifyParticleTypes;
import com.pepper.faunify.registry.FaunifyEntities;
import com.pepper.faunify.registry.FaunifySounds;


public class FennecEntity extends TamableAnimal implements GeoEntity {
	private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(FennecEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(FennecEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private boolean orderedToSit;
    public static final float DEFAULT_HEALTH = 5.0F;
    public static final float TAMED_HEALTH = 18.0F;

    public FennecEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
    	super(entityType, world);
        if (this.isTame()) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TAMED_HEALTH);
            this.setHealth(TAMED_HEALTH);
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(DEFAULT_HEALTH);
            this.setHealth(DEFAULT_HEALTH);
        }
    }
    
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
    	this.setVariant(Util.getRandom(FennecEntity.Variant.values(), world.getRandom()));
        super.finalizeSpawn(world, difficulty, spawnReason, spawnData, dataTag);
        if (spawnData == null) {
            spawnData = new AgeableMob.AgeableMobGroupData(false);
        }
		return spawnData;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, DEFAULT_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 1.8D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLEEPING, false);
        this.entityData.define(DATA_VARIANT_ID, 0);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id);
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putBoolean("Sitting", this.orderedToSit);
        tag.putDouble("PosX", this.getX());
        tag.putDouble("PosY", this.getY());
        tag.putDouble("PosZ", this.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(FennecEntity.Variant.byId(tag.getInt("Variant")));
        this.orderedToSit = tag.getBoolean("Sitting");
        this.setSleeping(tag.getBoolean("Sleeping"));
        this.setInSittingPose(this.orderedToSit);
        if (tag.contains("PosX") && tag.contains("PosY") && tag.contains("PosZ")) {
            double posX = tag.getDouble("PosX");
            double posY = tag.getDouble("PosY");
            double posZ = tag.getDouble("PosZ");
            this.setPos(posX, posY, posZ);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D) {
            @Override
            public boolean canUse() {
                return !FennecEntity.this.isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, true)); 
        this.goalSelector.addGoal(1, new AlertOnHostileGoal(this)); 
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.25D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.35D, Ingredient.of(Items.CHICKEN), false));
        this.goalSelector.addGoal(4, new SleepGoal(200));
        this.goalSelector.addGoal(5, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.6D, 1.4D) {
            @Override
            public boolean canUse() {
                Player nearestPlayer = FennecEntity.this.level().getNearestPlayer(FennecEntity.this, 10.0D);
                if (nearestPlayer != null) {
                    ItemStack heldItem = nearestPlayer.getMainHandItem();
                    return !heldItem.is(Items.CHICKEN) && super.canUse();
                }
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.3D, 10.0F, 2.0F, false) {
            @Override
            public boolean canUse() {
                return super.canUse() && !FennecEntity.this.isInSittingPose();
            }
        });
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, Chicken.class, true));
        this.targetSelector.addGoal(8, new NearestAttackableTargetGoal<>(this, Rabbit.class, true));
        this.targetSelector.addGoal(9, new NearestAttackableTargetGoal<>(this, MouseEntity.class, true));
    }
    
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (this.isInSittingPose()) {
            this.getNavigation().stop();
            this.setNoGravity(false); 
        } else {
            this.setNoGravity(false); 
            this.getNavigation().recomputePath();
        }
    }
    
    public boolean isSleeping() {
        return this.entityData.get(SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.entityData.set(SLEEPING, sleeping);
    }
    
    public boolean isOrderedToSit() {
       return this.orderedToSit;
    }

    public void setOrderedToSit(boolean isSitting) {
       this.orderedToSit = isSitting;
    }
    
    public boolean isInSittingPose() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
     }

    @Override
    public void setInSittingPose(boolean sitting) {
        super.setInSittingPose(sitting);
        this.orderedToSit = sitting;
        if (sitting) {
            this.getNavigation().stop();
        }
    }
    
    public static boolean canSpawn(EntityType<FennecEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
    	return level.getBlockState(position.below()).is(BlockTags.SAND) && isBrightEnoughToSpawn(level, position);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<FennecEntity> event) {
        LivingEntity target = this.getTarget();
        if (target != null && !target.isAlive()) {
            this.setTarget(null);
            target = null;
        }
        if (this.isSleeping()) {
            event.getController().setAnimation(RawAnimation.begin().then("sleep", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        if (this.isInSittingPose()) {
            event.getController().setAnimation(RawAnimation.begin().then("sit", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        if (target instanceof Chicken || target instanceof Rabbit) {
            if (event.isMoving()) {
                event.getController().setAnimation(RawAnimation.begin().then("run", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            } else {
                event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            }
        }
        if (isFollowingPlayerWithFood()) {
            if (event.isMoving()) {
                event.getController().setAnimation(RawAnimation.begin().then("run", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            } else {
                event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            }
        }
        if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    private boolean isFollowingPlayerWithFood() {
        Player player = this.level().getNearestPlayer(this, 10.0D);
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            return heldItem.getItem() == Items.RABBIT;
        }
        return false;
    }

    @Override
    public FennecEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob otherParent) {
        FennecEntity babyFennec = FaunifyEntities.FENNEC.get().create(serverLevel);

        if (babyFennec != null && otherParent instanceof FennecEntity parent) {

            Variant selectedVariant;
            selectedVariant = random.nextBoolean() ? this.getVariant() : parent.getVariant();
            babyFennec.setVariant(selectedVariant);
        }

        return babyFennec;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        int soundIndex = random.nextInt(2);
        switch (soundIndex) {
            case 0:
                return FaunifySounds.FENNEC_IDLE_1.get();
            case 1:
                return FaunifySounds.FENNEC_IDLE_2.get();
            default:
                return FaunifySounds.FENNEC_IDLE_1.get();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return FaunifySounds.FENNEC_HURT.get();
    }
    
    public class SleepGoal extends Goal {
        private final int countdownTime;
        private int countdown;

        public SleepGoal(int countdownTime) {
            this.countdownTime = countdownTime;
            this.countdown = FennecEntity.this.random.nextInt(reducedTickDelay(countdownTime));
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        public boolean canUse() {
            
            if (FennecEntity.this.isTame() && !FennecEntity.this.isInSittingPose()) {
                return false;
            }
            
            if (FennecEntity.this.xxa == 0.0F && FennecEntity.this.yya == 0.0F && FennecEntity.this.zza == 0.0F) {
                return this.canSleep() || FennecEntity.this.isSleeping();
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            
            if (FennecEntity.this.isTame() && !FennecEntity.this.isInSittingPose()) {
                return false;
            }
            
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return FennecEntity.this.level().isNight();
            }
        }

        public void stop() {
            FennecEntity.this.setSleeping(false);
            this.countdown = FennecEntity.this.random.nextInt(this.countdownTime);
        }

        public void start() {
            FennecEntity.this.setJumping(false);
            FennecEntity.this.setSleeping(true);
            FennecEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (FennecEntity.this.isSleeping()) {
                FennecEntity.this.getNavigation().stop();
            }
        }
    }
    
    @Override
    public void tick() {
        super.tick();
    }
    
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean flag = target.hurt(this.damageSources().mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
    	if (target instanceof Chicken || target instanceof Rabbit) {
            return super.doHurtTarget(target);
        }
		return flag;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (this.isSleeping()) {
            this.setSleeping(false);
        }

        if (this.isInSittingPose()) {
            this.setInSittingPose(false);
            this.setOrderedToSit(false);
        }
        
        return result;
    }
    
    public FennecEntity.Variant getVariant() {
        return FennecEntity.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
     }

     public void setVariant(FennecEntity.Variant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.id);
     }
     
     public static enum Variant implements StringRepresentable {
         LIGHT(0, "light"),
         DARK(1, "dark");

         public static final Codec<FennecEntity.Variant> CODEC = StringRepresentable.fromEnum(FennecEntity.Variant::values);
         private static final IntFunction<FennecEntity.Variant> BY_ID = ByIdMap.continuous(FennecEntity.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
         final int id;
         private final String name;

         private Variant(int p_262571_, String p_262693_) {
            this.id = p_262571_;
            this.name = p_262693_;
         }

         public int getId() {
            return this.id;
         }

         public static FennecEntity.Variant byId(int p_262643_) {
            return BY_ID.apply(p_262643_);
         }

         public String getSerializedName() {
            return this.name;
         }
      }

     @SuppressWarnings("resource")
     @Override
     public InteractionResult mobInteract(Player player, InteractionHand hand) {
         ItemStack itemstack = player.getItemInHand(hand);
         Item item = itemstack.getItem();

         if (this.isTame() && this.isOwnedBy(player)) {
             if (item == Items.CHICKEN) {
                 if (this.getHealth() < this.getMaxHealth()) {
                     int particleCount = 5;
                     this.heal(3.0F);
                     if (!player.getAbilities().instabuild) {
                         itemstack.shrink(1);
                     }
                     for (int i = 0; i < particleCount; i++) {
                         double offsetX = (this.random.nextDouble() - 0.5) * 1;
                         double offsetY = (this.random.nextDouble() - 0.5) * 1;
                         double offsetZ = (this.random.nextDouble() - 0.5) * 1;
                         this.level().addParticle(FaunifyParticleTypes.HEALTH.get(), 
                             this.getX() + offsetX, 
                             this.getY() + this.getEyeHeight() + offsetY, 
                             this.getZ() + offsetZ, 
                             0.0D, 0.0D, 0.0D);
                     }
                 } else {
                     toggleSittingState();
                 }
                 return InteractionResult.SUCCESS;
             }

             if (item == Items.RABBIT) {
                 if (!this.isInLove() && this.getAge() == 0) {
                     if (!this.level().isClientSide) {
                         this.setInLove(player);
                         if (!player.getAbilities().instabuild) {
                             itemstack.shrink(1);
                         }
                     }
                 } else {
                     toggleSittingState();
                 }
                 return InteractionResult.SUCCESS;
             }

             if (itemstack.isEmpty() || item != Items.RABBIT && item != Items.CHICKEN) {
                 toggleSittingState();
                 return InteractionResult.SUCCESS;
             }
         }

         if (item == Items.CHICKEN && !this.isTame()) {
             if (!player.getAbilities().instabuild) {
                 itemstack.shrink(1);
             }

             if (!this.level().isClientSide) {
                 if (this.random.nextInt(3) == 0) {
                     this.tame(player);
                     this.setOwnerUUID(player.getUUID());
                     this.level().broadcastEntityEvent(this, (byte) 7);
                 } else {
                     this.level().broadcastEntityEvent(this, (byte) 6);
                 }
             }

             return InteractionResult.SUCCESS;
         }

         return super.mobInteract(player, hand);
     }

     public class AlertOnHostileGoal extends Goal {
         private final TamableAnimal fennec;
         private final Level level;

         public AlertOnHostileGoal(TamableAnimal fennec) {
             this.fennec = fennec;
             this.level = fennec.level();
         }

         public boolean canStart() {
             if (fennec.getOwner() instanceof Player) {
                 List<LivingEntity> nearbyMobs = level.getEntitiesOfClass(LivingEntity.class, fennec.getBoundingBox().inflate(10.0D), entity -> entity instanceof Monster);
                 return !nearbyMobs.isEmpty();
             }
             return false;
         }

         @Override
         public void start() {
             fennec.playSound(FaunifySounds.FENNEC_ALERT.get(), 1.0F, 1.0F);
             List<LivingEntity> nearbyMobs = level.getEntitiesOfClass(LivingEntity.class, 
                 fennec.getBoundingBox().inflate(15.0D), entity -> entity instanceof Monster);

             for (LivingEntity mob : nearbyMobs) {
                 mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
             }
         }
         
         @Override
         public boolean canUse() {
             if (fennec.getOwner() instanceof Player) {
                 List<LivingEntity> nearbyMobs = level.getEntitiesOfClass(LivingEntity.class, fennec.getBoundingBox().inflate(10.0D), entity -> entity instanceof Monster);
                 return !nearbyMobs.isEmpty();
             }
             return false;
         }
     }

     @SuppressWarnings("resource")
     private void toggleSittingState() {
         boolean currentSittingState = this.isInSittingPose();
         this.setInSittingPose(!currentSittingState);

         if (!this.level().isClientSide) {
             if (!currentSittingState) {
                 this.getNavigation().stop();
             } else {
                 this.setOrderedToSit(false);
                 this.getNavigation().recomputePath();
             }
         }
     }
     
     @Override
     public boolean isFood(ItemStack stack) {
         return stack.getItem() == Items.RABBIT;
     }

}
