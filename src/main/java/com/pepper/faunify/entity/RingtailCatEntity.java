package com.pepper.faunify.entity;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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


public class RingtailCatEntity extends TamableAnimal implements GeoEntity {
	private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(RingtailCatEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(RingtailCatEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> STALKING = SynchedEntityData.defineId(RingtailCatEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DIGGING = SynchedEntityData.defineId(RingtailCatEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private boolean orderedToSit;
    public static final float DEFAULT_HEALTH = 15.0F;
    public static final float TAMED_HEALTH = 25.0F;

    public RingtailCatEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
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
    	this.setVariant(Util.getRandom(RingtailCatEntity.Variant.values(), world.getRandom()));
        super.finalizeSpawn(world, difficulty, spawnReason, spawnData, dataTag);
        if (spawnData == null) {
            spawnData = new AgeableMob.AgeableMobGroupData(false);
        }
		return spawnData;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, DEFAULT_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 1.8D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLEEPING, false);
        this.entityData.define(STALKING, false);
        this.entityData.define(DIGGING, false);
        this.entityData.define(DATA_VARIANT_ID, 0);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id);
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putBoolean("Sitting", this.orderedToSit);
        tag.putBoolean("Digging", this.isDigging());
        tag.putDouble("PosX", this.getX());
        tag.putDouble("PosY", this.getY());
        tag.putDouble("PosZ", this.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(RingtailCatEntity.Variant.byId(tag.getInt("Variant")));
        this.orderedToSit = tag.getBoolean("Sitting");
        this.setSleeping(tag.getBoolean("Sleeping"));
        this.setDigging(tag.getBoolean("Digging"));
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
        this.goalSelector.addGoal(1, new SleepGoal(200));
        
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.2D, 10.0F, 2.0F, false) {
            @Override
            public boolean canUse() {
                return super.canUse() && !RingtailCatEntity.this.isInSittingPose();
            }
            
            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && !RingtailCatEntity.this.isInSittingPose();
            }
        });
        
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.25D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.35D, Ingredient.of(Items.COD, Items.SALMON), false) {
            @Override
            public boolean canUse() {
                return super.canUse() && !RingtailCatEntity.this.isInSittingPose();
            }
        });
        
        this.goalSelector.addGoal(4, new RingtailStalkAndPounceGoal(this));
        this.goalSelector.addGoal(5, new MiningGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return super.canUse() && !RingtailCatEntity.this.isInSittingPose();
            }
        });
        
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Rabbit.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, MouseEntity.class, true));
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
        
        if (this.isTame() && !this.isInSittingPose() && this.getOwner() instanceof Player player) {
            MobEffectInstance current = player.getEffect(MobEffects.DIG_SPEED);
            if (current == null || current.getDuration() < 10) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false, true));
            }
        } else {
            if (this.getOwner() instanceof Player player && player.hasEffect(MobEffects.DIG_SPEED)) {
                player.removeEffect(MobEffects.DIG_SPEED);
            }
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
    
    public static boolean canSpawn(EntityType<RingtailCatEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
        return Animal.checkAnimalSpawnRules(entityType, level, spawnType, position, random);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<RingtailCatEntity> event) {
        LivingEntity target = this.getTarget();
        Vec3 velocity = this.getDeltaMovement();
        double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        double movingThreshold = 0.001;

        if (target != null && !target.isAlive()) {
            this.setTarget(null);
            target = null;
        }
        
        if (this.isDigging()) {
            event.getController().setAnimation(RawAnimation.begin().then("dig", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        if (this.isSleeping()) {
            event.getController().setAnimation(RawAnimation.begin().then("sleep", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        if (this.isStalking()) {
            event.getController().setAnimation(RawAnimation.begin().then("stalk", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        if (this.isInSittingPose()) {
            event.getController().setAnimation(RawAnimation.begin().then("sit", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        if (target instanceof Rabbit || target instanceof MouseEntity) {
            if (horizontalSpeedSq > movingThreshold) {
                event.getController().setAnimation(RawAnimation.begin().then("run", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            } else {
                event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            }
        }

        if (isFollowingPlayerWithFood()) {
            if (horizontalSpeedSq > movingThreshold) {
                event.getController().setAnimation(RawAnimation.begin().then("run", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            } else {
                event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            }
        }

        if (!this.onGround() && velocity.y > 0.1) {
            event.getController().setAnimation(RawAnimation.begin().then("leap", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }

        if (horizontalSpeedSq > movingThreshold) {
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
            return heldItem.getItem() == Items.COD || heldItem.getItem() == Items.SALMON;
        }
        return false;
    }

    @Override
    public RingtailCatEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob otherParent) {
        RingtailCatEntity babyRingtailCat = FaunifyEntities.RINGTAIL.get().create(serverLevel);

        if (babyRingtailCat != null && otherParent instanceof RingtailCatEntity parent) {

            Variant selectedVariant;
            selectedVariant = random.nextBoolean() ? this.getVariant() : parent.getVariant();
            babyRingtailCat.setVariant(selectedVariant);
        }

        return babyRingtailCat;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        int soundIndex = random.nextInt(2);
        switch (soundIndex) {
            case 0:
                return FaunifySounds.RINGTAILCAT_IDLE_1.get();
            case 1:
                return FaunifySounds.RINGTAILCAT_IDLE_2.get();
            default:
                return FaunifySounds.RINGTAILCAT_IDLE_1.get();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return FaunifySounds.RINGTAILCAT_HURT.get();
    }
    
    public class SleepGoal extends Goal {
        private final int countdownTime;
        private int countdown;

        public SleepGoal(int countdownTime) {
            this.countdownTime = countdownTime;
            this.countdown = RingtailCatEntity.this.random.nextInt(reducedTickDelay(countdownTime));
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        public boolean canUse() {
            if (RingtailCatEntity.this.isDigging()) {
                return false;
            }
            
            if (RingtailCatEntity.this.isTame() && !RingtailCatEntity.this.isInSittingPose()) {
                return false;
            }
            
            if (RingtailCatEntity.this.xxa == 0.0F && RingtailCatEntity.this.yya == 0.0F && RingtailCatEntity.this.zza == 0.0F) {
                return this.canSleep() || RingtailCatEntity.this.isSleeping();
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            if (RingtailCatEntity.this.isDigging()) {
                return false;
            }
            
            if (RingtailCatEntity.this.isTame() && !RingtailCatEntity.this.isInSittingPose()) {
                return false;
            }
            
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return RingtailCatEntity.this.level().isNight();
            }
        }

        public void stop() {
            RingtailCatEntity.this.setSleeping(false);
            this.countdown = RingtailCatEntity.this.random.nextInt(this.countdownTime);
        }

        public void start() {
            RingtailCatEntity.this.setJumping(false);
            RingtailCatEntity.this.setSleeping(true);
            RingtailCatEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (RingtailCatEntity.this.isSleeping()) {
                RingtailCatEntity.this.getNavigation().stop();
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
    	if (target instanceof MouseEntity || target instanceof Rabbit) {
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
    
    public RingtailCatEntity.Variant getVariant() {
        return RingtailCatEntity.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
     }

     public void setVariant(RingtailCatEntity.Variant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.id);
     }
     
     public static enum Variant implements StringRepresentable {
         REGULAR(0, "light"),
         BROWN(1, "brown"),
         GREY(2, "grey");

         public static final Codec<RingtailCatEntity.Variant> CODEC = StringRepresentable.fromEnum(RingtailCatEntity.Variant::values);
         private static final IntFunction<RingtailCatEntity.Variant> BY_ID = ByIdMap.continuous(RingtailCatEntity.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
         final int id;
         private final String name;

         private Variant(int p_262571_, String p_262693_) {
            this.id = p_262571_;
            this.name = p_262693_;
         }

         public int getId() {
            return this.id;
         }

         public static RingtailCatEntity.Variant byId(int p_262643_) {
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
        	 
             if (item == Items.RABBIT) {
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

             if (item == Items.COD || item == Items.SALMON) {
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
             
             if (itemstack.isEmpty() || item != Items.COD && item != Items.SALMON && item != Items.RABBIT) {
                 toggleSittingState();
                 return InteractionResult.SUCCESS;
             }
         }

         if (item == Items.RABBIT && !this.isTame()) {
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
         return stack.getItem() == Items.COD || stack.getItem() == Items.SALMON;
     }
     
     public class RingtailStalkAndPounceGoal extends Goal {
    	    private final RingtailCatEntity ringtail;
    	    private int cooldown = 0;

    	    private static final double STALK_SPEED = 0.5D;
    	    private static final double POUNCE_RANGE_SQ = 9.0D;
    	    private static final double START_RANGE_SQ = 64.0D;
    	    private static final int COOLDOWN_TICKS = 60;

    	    public RingtailStalkAndPounceGoal(RingtailCatEntity ringtail) {
    	        this.ringtail = ringtail;
    	        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    	    }

    	    @Override
    	    public boolean canUse() {
    	        LivingEntity potentialTarget = ringtail.getTarget();
    	        if (potentialTarget == null || !potentialTarget.isAlive()) return false;
    	        if (!(potentialTarget instanceof Rabbit || potentialTarget instanceof MouseEntity)) return false;
    	        return ringtail.distanceToSqr(potentialTarget) < START_RANGE_SQ;
    	    }

    	    @Override
    	    public boolean canContinueToUse() {
    	        LivingEntity currentTarget = ringtail.getTarget();
    	        if (currentTarget == null || !currentTarget.isAlive()) return false;
    	        return ringtail.distanceToSqr(currentTarget) < 256.0D;
    	    }

    	    @Override
    	    public void start() {
    	        cooldown = 0;
    	        ringtail.setStalking(true);
    	    }

    	    @Override
    	    public void stop() {
    	        ringtail.getNavigation().stop();
                ringtail.playSound(FaunifySounds.RINGTAILCAT_ANGRY.get(), 0.5F, 1.0F);
    	        ringtail.setStalking(false);
    	    }

    	    @Override
    	    public void tick() {
    	        LivingEntity currentTarget = ringtail.getTarget();
    	        if (currentTarget == null) return;

    	        ringtail.getLookControl().setLookAt(currentTarget, 30.0F, 30.0F);

    	        double distSq = ringtail.distanceToSqr(currentTarget);

    	        if (distSq > POUNCE_RANGE_SQ) {
    	            ringtail.getNavigation().moveTo(currentTarget, STALK_SPEED);
    	        } else {
    	            ringtail.getNavigation().stop();

    	            if (cooldown <= 0 && ringtail.onGround()) {
    	                Vec3 dir = new Vec3(
    	                    currentTarget.getX() - ringtail.getX(),
    	                    currentTarget.getY() - ringtail.getY(),
    	                    currentTarget.getZ() - ringtail.getZ()
    	                ).normalize();

    	                ringtail.setDeltaMovement(dir.x * 0.5, 0.6, dir.z * 0.5);
    	                ringtail.playSound(FaunifySounds.RINGTAILCAT_GROWL.get(), 0.5F, 1.0F);
    	                cooldown = COOLDOWN_TICKS;
    	            } else {
    	                cooldown--;
    	            }

    	            if (cooldown <= 0) {
    	                this.stop();
    	                return;
    	            }

    	            if (ringtail.onGround() && ringtail.distanceToSqr(currentTarget) < 2.0D && currentTarget.isAlive()) {
    	                currentTarget.hurt(ringtail.damageSources().mobAttack(ringtail), 5.0F);
    	                ringtail.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
    	            }
    	        }
    	    }
    	}

     
     public boolean isStalking() {
    	    return this.entityData.get(STALKING);
    	}

		public void setStalking(boolean stalking) {
    	    this.entityData.set(STALKING, stalking);
    	}
		
		public class MiningGoal extends Goal {
		    private final RingtailCatEntity ringtail;
		    private static final int SEARCH_RADIUS = 5;
		    private static final long COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(20);
		    private long lastShriekTime = 0;

		    private BlockPos targetOrePos;
		    private int standStillTicks = 0;
		    private static final int STAND_STILL_DURATION = 60;

		    private static final int MAX_LOCK_ON_TICKS = 100;
		    private int lockOnTicks = 0;

		    private BlockPos diggingBlockPos = null;
		    private int breakProgress = 0;
		    private int totalBreakTicks = 0;

		    public MiningGoal(RingtailCatEntity ringtail) {
		        this.ringtail = ringtail;
		        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		    }

		    @Override
		    public boolean canUse() {
		        long now = System.currentTimeMillis();
		        if ((now - lastShriekTime) < COOLDOWN_MILLIS) return false;

		        Level level = ringtail.level();
		        BlockPos origin = ringtail.blockPosition();

		        for (BlockPos pos : BlockPos.betweenClosed(
		                origin.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
		                origin.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {

		            BlockState state = level.getBlockState(pos);

		            if (isRareOre(state)) {
		                targetOrePos = pos.immutable();
		                return true;
		            }
		        }
		        return false;
		    }

		    private boolean isRareOre(BlockState state) {
		        return state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE) ||
		               state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) ||
		               state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE);
		    }

		    private boolean isSoftBlock(BlockState state) {
		        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.SNOW) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY);
		    }

		    @Override
		    public void start() {
		        if (targetOrePos != null) {
		            ringtail.playSound(FaunifySounds.RINGTAILCAT_ALERT.get(), 1.5F, 1.0F);
		            lastShriekTime = System.currentTimeMillis();
		            standStillTicks = 0;
		            lockOnTicks = 0;
		            updatePathToOre();
		        }
		    }

		    private void updatePathToOre() {
		        if (ringtail.getNavigation() instanceof GroundPathNavigation nav) {
		            nav.moveTo(targetOrePos.getX(), targetOrePos.getY(), targetOrePos.getZ(), 1.0D);
		        }
		    }

		    @Override
		    public boolean canContinueToUse() {
		        return targetOrePos != null && lockOnTicks < MAX_LOCK_ON_TICKS;
		    }

		    @Override
		    public void stop() {
		        targetOrePos = null;
		        standStillTicks = 0;
		        lockOnTicks = 0;
		        
		        clearDiggingAnimation();
		        ringtail.setDigging(false);
		        
		        ringtail.getNavigation().stop();
		    }

		    private void clearDiggingAnimation() {
		        if (diggingBlockPos != null) {
		            sendBlockBreakProgress(-1);
		        }
		        diggingBlockPos = null;
		        breakProgress = 0;
		        totalBreakTicks = 0;
		    }

		    @Override
		    public void tick() {
		        if (targetOrePos == null) return;

		        Level level = ringtail.level();
		        BlockPos ringtailPos = ringtail.blockPosition();

		        if (isOreExposed(level, targetOrePos)) {
		            stop();
		            return;
		        }

		        lockOnTicks++;
		        if (lockOnTicks >= MAX_LOCK_ON_TICKS) {
		            stop();
		            return;
		        }

		        if (ringtailPos.closerThan(targetOrePos, 2.0D)) {
		            standStillTicks++;
		            ringtail.getNavigation().stop();
		            
		            if (diggingBlockPos != null) {
		                clearDiggingAnimation();
		                ringtail.setDigging(false);
		            }
		            
		            if (standStillTicks >= STAND_STILL_DURATION) {
		                stop();
		            }
		            return;
		        }

		        BlockPos blockInFront = getBlockInFront(ringtailPos, ringtail.getYRot());
		        BlockState blockAhead = level.getBlockState(blockInFront);

		        if (isSoftBlock(blockAhead)) {
		            ringtail.setDigging(true);
		            
		            if (diggingBlockPos == null || !diggingBlockPos.equals(blockInFront)) {
		                if (diggingBlockPos != null && !diggingBlockPos.equals(blockInFront)) {
		                    sendBlockBreakProgress(-1);
		                }
		                
		                diggingBlockPos = blockInFront.immutable();
		                breakProgress = 0;
		                totalBreakTicks = (int)(blockAhead.getDestroySpeed(level, blockInFront) * 20);
		                sendBlockBreakProgress(0);
		            }

		            if (breakProgress < totalBreakTicks) {
		                breakProgress++;
		                sendBlockBreakProgress((int)((breakProgress / (float)totalBreakTicks) * 10));
		            } else {
		                level.destroyBlock(diggingBlockPos, true, ringtail);
		                sendBlockBreakProgress(-1);
		                diggingBlockPos = null;
		                breakProgress = 0;
		                totalBreakTicks = 0;
		            }

		            ringtail.getNavigation().stop();
		        } else {
		            if (diggingBlockPos != null) {
		                clearDiggingAnimation();
		            }
		            ringtail.setDigging(false);
		            
		            ringtail.getMoveControl().setWantedPosition(
		                targetOrePos.getX() + 0.5,
		                targetOrePos.getY(),
		                targetOrePos.getZ() + 0.5,
		                1.0D
		            );
		        }
		    }

		    private void sendBlockBreakProgress(int stage) {
		        if (diggingBlockPos == null) return;

		        for (ServerPlayer player : ((ServerLevel) ringtail.level()).players()) {
		            player.connection.send(new ClientboundBlockDestructionPacket(ringtail.getId(), diggingBlockPos, stage));
		        }
		    }

		    private BlockPos getBlockInFront(BlockPos currentPos, float yaw) {
		        double rad = Math.toRadians(yaw);
		        int offsetX = (int) Math.round(-Math.sin(rad));
		        int offsetZ = (int) Math.round(Math.cos(rad));
		        return currentPos.offset(offsetX, 0, offsetZ);
		    }
		    
		    private boolean isOreExposed(Level level, BlockPos pos) {
		        for (Direction dir : Direction.values()) {
		            if (level.getBlockState(pos.relative(dir)).isAir()) {
		                return true;
		            }
		        }
		        return false;
		    }
		}     
		
	public boolean isDigging() {
		return this.entityData.get(DIGGING);
	}

	public void setDigging(boolean digging) {
		this.entityData.set(DIGGING, digging);
	}
}
