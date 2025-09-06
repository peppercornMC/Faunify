package com.pepper.faunify.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.animal.Animal;
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

import java.util.function.IntFunction;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.pepper.faunify.particle.FaunifyParticleTypes;
import com.pepper.faunify.registry.FaunifyEntities;
import com.pepper.faunify.registry.FaunifyItems;
import com.pepper.faunify.registry.FaunifySounds;


public class HedgehogEntity extends TamableAnimal implements GeoEntity {
	private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> CURLED = SynchedEntityData.defineId(HedgehogEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private boolean orderedToSit;
    private boolean hurt;
    private int hurtCooldown = 100;
    private int quillDropTime = 0;
    private int quillDropInterval;
    public static final float DEFAULT_HEALTH = 5.0F;
    public static final float TAMED_HEALTH = 15.0F;
    private static final long DAMAGE_COOLDOWN = 20;
    private Map<LivingEntity, Long> lastDamageTime = new HashMap<>();
    private static final Random RANDOM = new Random();

    public HedgehogEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
    	super(entityType, world);
        resetQuillDropInterval();

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
        this.setVariant(Util.getRandom(HedgehogEntity.Variant.values(), world.getRandom()));
        super.finalizeSpawn(world, difficulty, spawnReason, spawnData, dataTag);
       
        if (spawnData == null) {
            spawnData = new AgeableMob.AgeableMobGroupData(false);
        }
		return spawnData;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, DEFAULT_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLEEPING, false);
        this.entityData.define(CURLED, false);
        this.entityData.define(DATA_VARIANT_ID, 0);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id);
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putBoolean("Curled", this.isCurledUp());
        tag.putBoolean("Sitting", this.orderedToSit);
        tag.putDouble("PosX", this.getX());
        tag.putDouble("PosY", this.getY());
        tag.putDouble("PosZ", this.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(HedgehogEntity.Variant.byId(tag.getInt("Variant")));
        this.orderedToSit = tag.getBoolean("Sitting");
        this.setSleeping(tag.getBoolean("Sleeping"));
        this.setCurled(tag.getBoolean("Curled"));
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
    	this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.5D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.5D, Ingredient.of(Items.PUMPKIN_SEEDS), false));
        this.goalSelector.addGoal(4, new SleepGoal(200));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.3D, 10.0F, 2.0F, false) {
            @Override
            public boolean canUse() {
                return super.canUse() && !HedgehogEntity.this.isInSittingPose();
            }
        });
        this.goalSelector.addGoal(7, new BreedGoal(this, 1.5D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }
    
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        
        if (this.isCurledUp()) {
            this.getNavigation().stop();
            return;
        }

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
    
    public static boolean canSpawn(EntityType<HedgehogEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
        return Animal.checkAnimalSpawnRules(entityType, level, spawnType, position, random);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(
            new AnimationController<>(this, "controller", 5, this::predicate)
        );
    }

    private PlayState predicate(AnimationState<HedgehogEntity> event) {
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
        
        if (this.isCurledUp()) {
            event.getController().setAnimation(RawAnimation.begin().then("curl", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
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
            return heldItem.getItem() == Items.PUMPKIN_SEEDS;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (hurt) {
            this.hurtCooldown--;
            if (this.hurtCooldown <= 0) {
                this.hurt = false;
                this.entityData.set(CURLED, false);
            }
        }

        quillDropTime++;

        if (quillDropTime >= quillDropInterval) {
            quillDropTime = 0;
            resetQuillDropInterval();

            if (this.level().getRandom().nextFloat() < 1.0F) {
                this.spawnAtLocation(FaunifyItems.QUILL.get());
            }
        }
    }

    private void resetQuillDropInterval() {
        this.quillDropInterval = 6000 + RANDOM.nextInt(6001);
    }

    @Override
    public HedgehogEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob otherParent) {
        HedgehogEntity babyHedgehog = FaunifyEntities.HEDGEHOG.get().create(serverLevel);

        if (babyHedgehog != null && otherParent instanceof HedgehogEntity parent) {

            Variant selectedVariant;
            selectedVariant = random.nextBoolean() ? this.getVariant() : parent.getVariant();
            babyHedgehog.setVariant(selectedVariant);
        }

        return babyHedgehog;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        int soundIndex = random.nextInt(3);
        switch (soundIndex) {
            case 0:
                return FaunifySounds.HEDGEHOG_IDLE_1.get();
            case 1:
                return FaunifySounds.HEDGEHOG_IDLE_2.get();
            case 2:
                return FaunifySounds.HEDGEHOG_IDLE_3.get();
            default:
                return FaunifySounds.HEDGEHOG_IDLE_1.get();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return FaunifySounds.HEDGEHOG_HURT.get();
    }
    
    public class SleepGoal extends Goal {
        private final int countdownTime;
        private int countdown;

        public SleepGoal(int countdownTime) {
            this.countdownTime = countdownTime;
            this.countdown = HedgehogEntity.this.random.nextInt(reducedTickDelay(countdownTime));
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        public boolean canUse() {
            if (HedgehogEntity.this.isCurledUp()) {
                return false;
            }
            
            if (HedgehogEntity.this.isTame() && !HedgehogEntity.this.isInSittingPose()) {
                return false;
            }
            
            if (HedgehogEntity.this.xxa == 0.0F && HedgehogEntity.this.yya == 0.0F && HedgehogEntity.this.zza == 0.0F) {
                return this.canSleep() || HedgehogEntity.this.isSleeping();
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            if (HedgehogEntity.this.isCurledUp()) {
                return false;
            }
            
            if (HedgehogEntity.this.isTame() && !HedgehogEntity.this.isInSittingPose()) {
                return false;
            }
            
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return HedgehogEntity.this.level().isNight();
            }
        }

        public void stop() {
            HedgehogEntity.this.setSleeping(false);
            this.countdown = HedgehogEntity.this.random.nextInt(this.countdownTime);
        }

        public void start() {
            HedgehogEntity.this.setJumping(false);
            HedgehogEntity.this.setSleeping(true);
            HedgehogEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (HedgehogEntity.this.isSleeping()) {
                HedgehogEntity.this.getNavigation().stop();
            }
        }
    }
    
    public class HedgehogCurlUpGoal extends Goal {
    	private final HedgehogEntity hedgehog;
        private int timer;

        public HedgehogCurlUpGoal(HedgehogEntity hedgehog) {
            this.hedgehog = hedgehog;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return hedgehog.hurt && !hedgehog.isCurledUp();
        }

        @Override
        public void start() {
        	if (hedgehog.isSleeping()) {
        		return;
        	} else {
            hedgehog.setCurled(true);
            timer = 0;
            hedgehog.getNavigation().stop();
            hedgehog.getLookControl().setLookAt(hedgehog.getX(), hedgehog.getY(), hedgehog.getZ());
        	}
        }

        @Override
        public void tick() {
            timer++;
            if (timer >= 40) {
                uncurlHedgehog();
                this.stop();
            }
        }

        private void uncurlHedgehog() {
            if (hedgehog.isCurledUp()) {
                hedgehog.setCurled(false);
            }
        }
    }
    
    public boolean isCurledUp() {
        return this.entityData.get(CURLED);
    }

    public void setCurled(boolean curled) {
        this.entityData.set(CURLED, curled);
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

        if (result) {
            this.hurt = true;
            this.hurtCooldown = 100;

            if (!(source.getEntity() instanceof HedgehogEntity)) {
                this.goalSelector.addGoal(1, new HedgehogCurlUpGoal(this));
            }

            if (source.getEntity() instanceof LivingEntity pushingEntity && pushingEntity != this) {
                if (pushingEntity instanceof Player player && player.isCreative()) {
                    return result;
                }

                long currentTime = this.level().getGameTime();
                if (!lastDamageTime.containsKey(pushingEntity) || (currentTime - lastDamageTime.get(pushingEntity)) >= DAMAGE_COOLDOWN) {
                    float damage = 1.0F;
                    pushingEntity.setHealth(pushingEntity.getHealth() - damage);

                    this.playSound(SoundEvents.GENERIC_HURT, 0.5F, 1.0F);
                    lastDamageTime.put(pushingEntity, currentTime);
                }
            }
        }

        return result;
    }

    
    @Override
    public void push(Entity entity) {
        super.push(entity);

        if (entity instanceof LivingEntity pushingEntity && pushingEntity != this) {
            
            if (pushingEntity instanceof Player player && player.isCreative()) {
                return;
            }

            if (pushingEntity instanceof HedgehogEntity) {
                return;
            }

            long currentTime = this.level().getGameTime();

            if (!lastDamageTime.containsKey(pushingEntity) || (currentTime - lastDamageTime.get(pushingEntity)) >= DAMAGE_COOLDOWN) {
                float damage = 1.0F;
                pushingEntity.setHealth(pushingEntity.getHealth() - damage);

                this.playSound(SoundEvents.GENERIC_HURT, 0.5F, 1.0F);

                lastDamageTime.put(pushingEntity, currentTime);
            }
        }
    }
    
    public HedgehogEntity.Variant getVariant() {
        return HedgehogEntity.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
     }

     public void setVariant(HedgehogEntity.Variant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.id);
     }
     
     public static enum Variant implements StringRepresentable {
         LIGHT(0, "light"),
         DARK(1, "dark"),
         ALBINO(2, "albino");

         public static final Codec<HedgehogEntity.Variant> CODEC = StringRepresentable.fromEnum(HedgehogEntity.Variant::values);
         private static final IntFunction<HedgehogEntity.Variant> BY_ID = ByIdMap.continuous(HedgehogEntity.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
         final int id;
         private final String name;

         private Variant(int p_262571_, String p_262693_) {
            this.id = p_262571_;
            this.name = p_262693_;
         }

         public int getId() {
            return this.id;
         }

         public static HedgehogEntity.Variant byId(int p_262643_) {
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
        	 
             if (item == Items.SWEET_BERRIES) {
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

             if (item == Items.PUMPKIN_SEEDS) {
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
             
             if (itemstack.isEmpty() || item != Items.PUMPKIN_SEEDS && item != Items.SWEET_BERRIES) {
                 toggleSittingState();
                 return InteractionResult.SUCCESS;
             }
         }

         if (item == Items.SWEET_BERRIES && !this.isTame()) {
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
     public void setTarget(@Nullable LivingEntity target) {
         if (this.isTame() && this.getOwner() != null && target == this.getOwner()) {
             return;
         }
         super.setTarget(target);
     }
     
     @Override
     public boolean isFood(ItemStack stack) {
         return stack.getItem() == Items.PUMPKIN_SEEDS;
     }
}
