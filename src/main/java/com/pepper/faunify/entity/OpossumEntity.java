package com.pepper.faunify.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
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
import com.pepper.faunify.registry.FaunifyEntities;
import com.pepper.faunify.registry.FaunifySounds;


public class OpossumEntity extends Animal implements GeoEntity {
	private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> BABY_COUNT = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> HAS_RIDDEN = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> RIDING_COOLDOWN = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> PLAYING_DEAD = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> PLAY_DEAD_COOLDOWN = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SHOULD_PLAY_DEAD = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<ItemStack> DATA_MOUTH_ITEM = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Integer> EATING_TIMER = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> IS_ACTIVELY_EATING = SynchedEntityData.defineId(OpossumEntity.class, EntityDataSerializers.BOOLEAN);
    
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private boolean aggressive = false;
    public static final float DEFAULT_HEALTH = 15.0F;
    private boolean shouldSpawnBabies = false;
    private int deferredBabyCount = 0;
    private int babySpawnTimer = 0;
    
    private static final int HOLDING_DURATION = 1200;
    private static final int EATING_DURATION = 120;

    public OpossumEntity(EntityType<? extends Animal> entityType, Level world) {
    	super(entityType, world);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(DEFAULT_HEALTH);
        this.setHealth(DEFAULT_HEALTH);
    }
    
    @SuppressWarnings("resource")
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        this.setVariant(Util.getRandom(OpossumEntity.Variant.values(), world.getRandom()));
        super.finalizeSpawn(world, difficulty, spawnReason, spawnData, dataTag);

        if (spawnData == null) {
            spawnData = new AgeableMob.AgeableMobGroupData(false);
        }

        if (!this.level().isClientSide && !this.isBaby()) {
            RandomSource random = world.getRandom();
            int roll = random.nextInt(100);

            int babyCount;
            if (roll < 25) {
                babyCount = 0;
            } else if (roll < 70) {
                babyCount = 1;
            } else {
                babyCount = 2;
            }

            this.setBabyCount(babyCount);

            if (babyCount > 0) {
                this.deferredBabyCount = babyCount;
                this.shouldSpawnBabies = true;
                this.babySpawnTimer = 5;
            }
        }

        return spawnData;
    }
    
    private void spawnBabies() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        for (int i = 0; i < deferredBabyCount; i++) {
            OpossumEntity baby = FaunifyEntities.OPOSSUM.get().create(this.level());
            if (baby != null) {
                baby.setBaby(true);
                baby.setVariant(this.getVariant());
                baby.finalizeSpawn(serverLevel, this.level().getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.NATURAL, null, null);
                baby.setVariant(this.getVariant());
                baby.setPos(this.getX(), this.getY(), this.getZ());
                this.level().addFreshEntity(baby);
                baby.startRiding(this, true);
                baby.setHasRidden(true);
            }
        }
        this.deferredBabyCount = 0;
        this.shouldSpawnBabies = false;
        this.babySpawnTimer = 0;
    }
    
    @SuppressWarnings("resource")
	@Override
    public void tick() {
        super.tick();

        if (shouldSpawnBabies && !this.level().isClientSide) {
            if (babySpawnTimer > 0) {
                babySpawnTimer--;
            } else {
                if (this.level() instanceof ServerLevel) {
                    spawnBabies();
                }
            }
        }
        
        if (!this.level().isClientSide && this.isBaby()) {
            int cooldown = this.getRidingCooldown();
            if (cooldown > 0) {
                this.setRidingCooldown(cooldown - 1);
            }
        }
        
        if (!this.level().isClientSide) {
            int playDeadCooldown = this.getPlayDeadCooldown();
            if (playDeadCooldown > 0) {
                this.setPlayDeadCooldown(playDeadCooldown - 1);
            }
        }
        
        if (!this.level().isClientSide) {
            for (Entity passenger : this.getPassengers()) {
                if (passenger instanceof OpossumEntity babyOpossum && !babyOpossum.isBaby()) {
                    passenger.stopRiding();
                    babyOpossum.setHasRidden(false);
                }
            }
        }
        
        handleEatingBerry();
    }
    
    @SuppressWarnings("resource")
	private void handleEatingBerry() {
        if (!this.getMouthItem().isEmpty() && this.getMouthItem().is(Items.SWEET_BERRIES)) {
            if (!this.level().isClientSide) {
                int currentTimer = this.getEatingTimer();
                
                if (currentTimer == 0) {
                    this.setEatingTimer(HOLDING_DURATION + EATING_DURATION);
                    this.setActivelyEating(false);
                } else if (currentTimer > 0) {
                    this.setEatingTimer(currentTimer - 1);
                    
                    if (currentTimer == EATING_DURATION + 1) {
                        this.setActivelyEating(true);
                    }
                    
                    if (currentTimer <= EATING_DURATION && this.isActivelyEating()) {
                        if (currentTimer % 40 == 0) {
                            this.createEatingParticles();
                            this.playSound(SoundEvents.GENERIC_EAT, 0.8F, 1.0F);
                        }
                    }
                    
                    if (currentTimer == 1) {
                        this.consumeBerry();
                    }
                }
            } else {
                int currentTimer = this.getEatingTimer();
                if (currentTimer > 0 && currentTimer <= EATING_DURATION && this.isActivelyEating() && currentTimer % 40 == 0) {
                    this.createEatingParticles();
                }
            }
        } else {
            if (this.getEatingTimer() > 0) {
                this.setEatingTimer(0);
                this.setActivelyEating(false);
            }
        }
    }
    
    private void createEatingParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            ItemStack berryStack = new ItemStack(Items.SWEET_BERRIES);
            ItemParticleOption particleData = new ItemParticleOption(ParticleTypes.ITEM, berryStack);
            
            float yaw = this.getYRot();
            double forwardX = -Math.sin(Math.toRadians(yaw)) * 0.6;
            double forwardZ = Math.cos(Math.toRadians(yaw)) * 0.6;
            
            Vec3 mouthPos = this.position().add(forwardX, this.getBbHeight() * 0.5, forwardZ);
            
            for (int i = 0; i < 3; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.1;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.1;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.1;
                
                serverLevel.sendParticles(particleData, 
                    mouthPos.x + offsetX, 
                    mouthPos.y + offsetY, 
                    mouthPos.z + offsetZ, 
                    1, 0, 0, 0, 0.05);
            }
        }
    }
    
    private void consumeBerry() {
        this.createEatingParticles();
        
        this.playSound(SoundEvents.FOX_EAT, 1.0F, 1.0F);
        this.setMouthItem(ItemStack.EMPTY);
        
        this.setEatingTimer(0);
        this.setActivelyEating(false);
        
        this.heal(1.0F);
    }
    
    public class PlayDeadGoal extends Goal {
        private int playDeadTimer;
        private int particleTimer = 0;
        private static final int PLAY_DEAD_DURATION = 160;
        private static final int PLAY_DEAD_COOLDOWN_TIME = 100;
        private static final int PARTICLE_INTERVAL = 15;

        public PlayDeadGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!OpossumEntity.this.getShouldPlayDead()) {
                return false;
            }

            if (OpossumEntity.this.isPlayingDead() || OpossumEntity.this.getPlayDeadCooldown() > 0) {
                return false;
            }

            if (OpossumEntity.this.isSleeping()) {
                return false;
            }

            if (OpossumEntity.this.isBaby() && OpossumEntity.this.isPassenger()) {
                return false;
            }

            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (OpossumEntity.this.isBaby() && OpossumEntity.this.isPassenger()) {
                return false;
            }
            
            return this.playDeadTimer > 0 && OpossumEntity.this.isPlayingDead();
        }

        @Override
        public void start() {
            this.playDeadTimer = PLAY_DEAD_DURATION;
            this.particleTimer = 0;
            OpossumEntity.this.setPlayingDead(true);
            OpossumEntity.this.setShouldPlayDead(false);
            
            OpossumEntity.this.getNavigation().stop();
            OpossumEntity.this.setDeltaMovement(Vec3.ZERO);
            
            if (!OpossumEntity.this.isBaby() && OpossumEntity.this.hasBabyPassengers()) {
                OpossumEntity.this.dismountBabies();
            }
            
            OpossumEntity.this.setTarget(null);
            OpossumEntity.this.setAggressive(false);
        }

        @Override
        public void stop() {
            OpossumEntity.this.setPlayingDead(false);
            OpossumEntity.this.setPlayDeadCooldown(PLAY_DEAD_COOLDOWN_TIME);
            this.playDeadTimer = 0;
            this.particleTimer = 0;
        }

        @Override
        public void tick() {
            if (OpossumEntity.this.isBaby() && OpossumEntity.this.isPassenger()) {
                OpossumEntity.this.setPlayingDead(false);
                return;
            }
            
            if (this.playDeadTimer > 0) {
                this.playDeadTimer--;
                
                OpossumEntity.this.getNavigation().stop();
                OpossumEntity.this.setDeltaMovement(Vec3.ZERO);
                
                OpossumEntity.this.xxa = 0.0F;
                OpossumEntity.this.yya = 0.0F;
                OpossumEntity.this.zza = 0.0F;
                
                this.particleTimer++;
                if (this.particleTimer >= PARTICLE_INTERVAL) {
                    this.particleTimer = 0;
                    OpossumEntity.this.createStinkyParticles();
                }
            }
            
            if (this.playDeadTimer <= 0 && OpossumEntity.this.isPlayingDead()) {
                OpossumEntity.this.setPlayingDead(false);
            }
        }
    }

    private void createStinkyParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            float red = 0.4f;
            float green = 0.6f;
            float blue = 0.3f;
            
            net.minecraft.core.particles.DustParticleOptions dustOptions = 
                new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(red, green, blue), 1.0f);
            
            for (int i = 0; i < 5; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.8;
                double offsetY = this.random.nextDouble() * 0.3 + 0.1;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.8;
                
                double velX = (this.random.nextDouble() - 0.5) * 0.02;
                double velY = this.random.nextDouble() * 0.05 + 0.02;
                double velZ = (this.random.nextDouble() - 0.5) * 0.02;
                
                serverLevel.sendParticles(dustOptions,
                    this.getX() + offsetX,
                    this.getY() + offsetY,
                    this.getZ() + offsetZ,
                    1, velX, velY, velZ, 0.0);
            }
        }
    }
    
    public class OpossumSearchForItemsGoal extends Goal {
        public OpossumSearchForItemsGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public boolean canUse() {
            if (!OpossumEntity.this.getMouthItem().isEmpty()) {
                return false;
            }
            if (OpossumEntity.this.getTarget() != null || OpossumEntity.this.getLastHurtByMob() != null) {
                return false;
            }
            if (OpossumEntity.this.isPlayingDead() || OpossumEntity.this.isSleeping()) {
                return false;
            }
            if (OpossumEntity.this.isBaby() && OpossumEntity.this.isPassenger()) {
                return false;
            }
            if (OpossumEntity.this.getRandom().nextInt(reducedTickDelay(10)) != 0) {
                return false;
            }
            
            List<ItemEntity> list = OpossumEntity.this.level().getEntitiesOfClass(ItemEntity.class, 
                OpossumEntity.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), OpossumEntity.ALLOWED_ITEMS);
            return !list.isEmpty();
        }

        public void tick() {
            List<ItemEntity> list = OpossumEntity.this.level().getEntitiesOfClass(ItemEntity.class, 
                OpossumEntity.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), OpossumEntity.ALLOWED_ITEMS);
            
            if (OpossumEntity.this.getMouthItem().isEmpty() && !list.isEmpty()) {
                ItemEntity closest = list.get(0);
                double closestDistance = OpossumEntity.this.distanceToSqr(closest);
                for (ItemEntity item : list) {
                    double distance = OpossumEntity.this.distanceToSqr(item);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closest = item;
                    }
                }
                
                if (closestDistance <= 1.5D * 1.5D) {
                    OpossumEntity.this.pickUpItem(closest);
                } else {
                    OpossumEntity.this.getNavigation().moveTo(closest.getX(), closest.getY(), closest.getZ(), 1.2D);
                }
            }
        }

        public void start() {
            List<ItemEntity> list = OpossumEntity.this.level().getEntitiesOfClass(ItemEntity.class, 
                OpossumEntity.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), OpossumEntity.ALLOWED_ITEMS);
            if (!list.isEmpty()) {
                ItemEntity target = list.get(0);
                OpossumEntity.this.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.2D);
            }
        }
    }
    
    public class OpossumEatBerriesGoal extends MoveToBlockGoal {
        private static final int WAIT_TICKS = 40;
        protected int ticksWaited;

        public OpossumEatBerriesGoal(double speedModifier, int searchRange, int verticalSearchRange) {
            super(OpossumEntity.this, speedModifier, searchRange, verticalSearchRange);
        }

        public double acceptedDistance() {
            return 2.0D;
        }

        public boolean shouldRecalculatePath() {
            return this.tryTicks % 100 == 0;
        }

        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            BlockState blockstate = level.getBlockState(pos);
            return blockstate.is(Blocks.SWEET_BERRY_BUSH) && blockstate.getValue(SweetBerryBushBlock.AGE) >= 2;
        }

        public void tick() {
            if (this.isReachedTarget()) {
                if (this.ticksWaited >= WAIT_TICKS) {
                    this.onReachedTarget();
                } else {
                    ++this.ticksWaited;
                }
            } else if (!this.isReachedTarget() && OpossumEntity.this.getRandom().nextFloat() < 0.05F) {
                OpossumEntity.this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.0F);
            }

            super.tick();
        }

        protected void onReachedTarget() {
            if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(OpossumEntity.this.level(), this.blockPos, OpossumEntity.this.level().getBlockState(this.blockPos), true)) {
                BlockState blockstate = OpossumEntity.this.level().getBlockState(this.blockPos);
                if (blockstate.is(Blocks.SWEET_BERRY_BUSH)) {
                    this.pickSweetBerries(blockstate);
                }
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(OpossumEntity.this.level(), this.blockPos, OpossumEntity.this.level().getBlockState(this.blockPos));
            }
        }

        private void pickSweetBerries(BlockState state) {
            int currentAge = state.getValue(SweetBerryBushBlock.AGE);
            
            @SuppressWarnings("resource")
			int berriesToDrop = 1 + OpossumEntity.this.level().random.nextInt(2) + (currentAge == 3 ? 1 : 0);
            
            ItemStack mouthItem = OpossumEntity.this.getMouthItem();
            if (mouthItem.isEmpty()) {
                OpossumEntity.this.setMouthItem(new ItemStack(Items.SWEET_BERRIES));
            }

            if (berriesToDrop > 0) {
                Block.popResource(OpossumEntity.this.level(), this.blockPos, new ItemStack(Items.SWEET_BERRIES, berriesToDrop));
            }

            OpossumEntity.this.playSound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
            OpossumEntity.this.level().setBlock(this.blockPos, state.setValue(SweetBerryBushBlock.AGE, 1), 2);
        }

        public boolean canUse() {
            return !OpossumEntity.this.isSleeping() && 
                   !OpossumEntity.this.isPlayingDead() &&
                   !(OpossumEntity.this.isBaby() && OpossumEntity.this.isPassenger()) &&
                   super.canUse();
        }

        public void start() {
            this.ticksWaited = 0;
            super.start();
        }
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, DEFAULT_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLEEPING, false);
        this.entityData.define(HAS_RIDDEN, false);
        this.entityData.define(BABY_COUNT, 0);
        this.entityData.define(DATA_VARIANT_ID, 0);
        this.entityData.define(RIDING_COOLDOWN, 0);
        this.entityData.define(PLAYING_DEAD, false);
        this.entityData.define(PLAY_DEAD_COOLDOWN, 0);
        this.entityData.define(SHOULD_PLAY_DEAD, false);
        this.entityData.define(DATA_MOUTH_ITEM, ItemStack.EMPTY);
        this.entityData.define(EATING_TIMER, 0);
        this.entityData.define(IS_ACTIVELY_EATING, false);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id);
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putBoolean("PlayingDead", this.isPlayingDead());
        tag.putBoolean("ShouldPlayDead", this.getShouldPlayDead());
        tag.putDouble("PosX", this.getX());
        tag.putDouble("PosY", this.getY());
        tag.putDouble("PosZ", this.getZ());
        tag.putBoolean("ShouldSpawnBabies", this.shouldSpawnBabies);
        tag.putInt("DeferredBabyCount", this.deferredBabyCount);
        tag.putInt("BabySpawnTimer", this.babySpawnTimer);
        tag.putInt("RidingCooldown", this.getRidingCooldown());
        tag.putInt("PlayDeadCooldown", this.getPlayDeadCooldown());
        tag.putInt("EatingTimer", this.getEatingTimer());
        tag.putBoolean("ActivelyEating", this.isActivelyEating());
        
        ItemStack itemstack = this.getMouthItem();
        if (!itemstack.isEmpty()) {
            tag.put("MouthItem", itemstack.save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(OpossumEntity.Variant.byId(tag.getInt("Variant")));
        this.setSleeping(tag.getBoolean("Sleeping"));
        
        boolean savedPlayingDead = tag.getBoolean("PlayingDead");
        if (savedPlayingDead && !this.level().isClientSide) {
            this.setPlayingDead(false);
            this.setPlayDeadCooldown(20);
        } else {
            this.setPlayingDead(savedPlayingDead);
        }
        
        this.setShouldPlayDead(tag.getBoolean("ShouldPlayDead"));
        
        if (tag.contains("PosX") && tag.contains("PosY") && tag.contains("PosZ")) {
            double posX = tag.getDouble("PosX");
            double posY = tag.getDouble("PosY");
            double posZ = tag.getDouble("PosZ");
            this.setPos(posX, posY, posZ);
        }
        this.shouldSpawnBabies = tag.getBoolean("ShouldSpawnBabies");
        this.deferredBabyCount = tag.getInt("DeferredBabyCount");
        this.babySpawnTimer = tag.getInt("BabySpawnTimer");
        this.setRidingCooldown(tag.getInt("RidingCooldown"));
        this.setPlayDeadCooldown(tag.getInt("PlayDeadCooldown"));
        this.setEatingTimer(tag.getInt("EatingTimer"));
        this.setActivelyEating(tag.getBoolean("ActivelyEating"));
        
        if (tag.contains("MouthItem", 10)) {
            this.setMouthItem(ItemStack.of(tag.getCompound("MouthItem")));
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PlayDeadGoal());
        this.goalSelector.addGoal(2, new OpossumEatBerriesGoal(1.0D, 16, 2));
        this.goalSelector.addGoal(3, new OpossumSearchForItemsGoal());
        
        this.goalSelector.addGoal(4, new FollowAdultOpossumGoal(this, 1.0D, 2.0F, 10.0F));
        
        this.goalSelector.addGoal(5, new BreedGoal(this, 1D));
        this.goalSelector.addGoal(6, new TemptGoal(this, 1.1D, Ingredient.of(Items.SWEET_BERRIES), false));
        this.goalSelector.addGoal(7, new SleepGoal(200));
        this.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        
        this.goalSelector.addGoal(8, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                return OpossumEntity.this.isAggressive() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return OpossumEntity.this.isAggressive() && super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return OpossumEntity.this.isAggressive() && super.canUse();
            }
        });
    }
    
    @SuppressWarnings("resource")
	public void dismountBabies() {
        if (!this.level().isClientSide) {
            List<Entity> passengersToRemove = new ArrayList<>();
            
            for (Entity passenger : this.getPassengers()) {
                if (passenger instanceof OpossumEntity babyOpossum && babyOpossum.isBaby()) {
                    passengersToRemove.add(passenger);
                }
            }
            
            for (Entity passenger : passengersToRemove) {
                passenger.stopRiding();
                if (passenger instanceof OpossumEntity babyOpossum) {
                    babyOpossum.setHasRidden(false);
                    babyOpossum.setRidingCooldown(200);
                }
            }
        }
    }

    public boolean hasBabyPassengers() {
        return this.getPassengers().stream()
            .anyMatch(entity -> entity instanceof OpossumEntity opossum && opossum.isBaby());
    }

    @Override
    public void setInLove(@Nullable Player player) {
        if (this.hasBabyPassengers()) {
            this.dismountBabies();
        }
        super.setInLove(player);
    }
    
    public boolean isSleeping() {
        return this.entityData.get(SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.entityData.set(SLEEPING, sleeping);
    }
    
    public boolean isPlayingDead() {
        return this.entityData.get(PLAYING_DEAD);
    }

    public void setPlayingDead(boolean playingDead) {
        this.entityData.set(PLAYING_DEAD, playingDead);
    }
    
    public boolean getShouldPlayDead() {
        return this.entityData.get(SHOULD_PLAY_DEAD);
    }

    public void setShouldPlayDead(boolean shouldPlayDead) {
        this.entityData.set(SHOULD_PLAY_DEAD, shouldPlayDead);
    }
    
    public ItemStack getMouthItem() {
        return this.entityData.get(DATA_MOUTH_ITEM);
    }

    public void setMouthItem(ItemStack stack) {
        this.entityData.set(DATA_MOUTH_ITEM, stack);
    }
    
    public int getEatingTimer() {
        return this.entityData.get(EATING_TIMER);
    }

    public void setEatingTimer(int timer) {
        this.entityData.set(EATING_TIMER, timer);
    }
    
    public boolean isActivelyEating() {
        return this.entityData.get(IS_ACTIVELY_EATING);
    }

    public void setActivelyEating(boolean eating) {
        this.entityData.set(IS_ACTIVELY_EATING, eating);
    }
    
    void dropMouthItem() {
        if (!this.getMouthItem().isEmpty()) {
            this.spawnAtLocation(this.getMouthItem());
            this.setMouthItem(ItemStack.EMPTY);
            this.setEatingTimer(0);
            this.setActivelyEating(false);
        }
    }
    
    private static final Predicate<ItemEntity> ALLOWED_ITEMS = (itemEntity) -> {
        return !itemEntity.hasPickUpDelay() && itemEntity.isAlive() && itemEntity.getItem().is(Items.SWEET_BERRIES);
    };
    
    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.canPickUpItem(itemstack)) {
            int itemCount = itemstack.getCount();
            
            if (!this.getMouthItem().isEmpty()) {
                this.dropMouthItem();
            }
            
            this.setMouthItem(new ItemStack(Items.SWEET_BERRIES, 1));
            this.take(itemEntity, 1);
            
            if (itemCount > 1) {
                itemstack.shrink(1);
            } else {
                itemEntity.discard();
            }
            
            this.playSound(SoundEvents.ITEM_PICKUP, 0.2F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }
    }

    public boolean canPickUpItem(ItemStack stack) {
        return stack.is(Items.SWEET_BERRIES) && 
               this.getMouthItem().isEmpty() && 
               !this.isPlayingDead() && 
               !this.isSleeping() &&
               !(this.isBaby() && this.isPassenger());
    }
    
    public static boolean canSpawn(EntityType<OpossumEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
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

    private PlayState predicate(AnimationState<OpossumEntity> event) {
        LivingEntity target = this.getTarget();
        Vec3 velocity = this.getDeltaMovement();
        double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        double movingThreshold = 0.001;

        if (target != null && !target.isAlive()) {
            this.setTarget(null);
            target = null;
        }
        
        if (this.isSleeping()) {
            event.getController().setAnimation(RawAnimation.begin().then("sleep", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        if (this.isPlayingDead()) {
            event.getController().setAnimation(RawAnimation.begin().then("play_dead", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
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
            return heldItem.getItem() == Items.SWEET_BERRIES;
        }
        return false;
    }

    @Override
    public OpossumEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob otherParent) {
        OpossumEntity babyOpossum = FaunifyEntities.OPOSSUM.get().create(serverLevel);

        if (babyOpossum != null && otherParent instanceof OpossumEntity parent) {

            Variant selectedVariant;
            selectedVariant = random.nextBoolean() ? this.getVariant() : parent.getVariant();
            babyOpossum.setVariant(selectedVariant);
        }

        return babyOpossum;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        int soundIndex = random.nextInt(2);
        switch (soundIndex) {
            case 0:
                return FaunifySounds.OPOSSUM_IDLE_1.get();
            case 1:
                return FaunifySounds.OPOSSUM_IDLE_2.get();
            default:
                return FaunifySounds.OPOSSUM_IDLE_1.get();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return FaunifySounds.OPOSSUM_HURT.get();
    }
    
    public class FollowAdultOpossumGoal extends Goal {
        private final OpossumEntity baby;
        private OpossumEntity targetAdult;
        private final double speedModifier;
        private final float stopDistance;
        private final float startDistance;
        private int timeToRecalcPath;

        public FollowAdultOpossumGoal(OpossumEntity baby, double speedModifier, float stopDistance, float startDistance) {
            this.baby = baby;
            this.speedModifier = speedModifier;
            this.stopDistance = stopDistance;
            this.startDistance = startDistance;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.baby.isBaby()) {
                return false;
            }
            
            if (this.baby.isPassenger()) {
                return false;
            }

            OpossumEntity nearestAdult = this.findNearestAdult();
            if (nearestAdult == null) {
                return false;
            }

            double distance = this.baby.distanceTo(nearestAdult);
            if (distance < this.startDistance) {
                this.targetAdult = nearestAdult;
                return true;
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.targetAdult == null || !this.targetAdult.isAlive() || !this.baby.isBaby()) {
                return false;
            }
            
            if (this.baby.isPassenger()) {
                return false;
            }

            double distance = this.baby.distanceTo(this.targetAdult);
            return distance < this.startDistance * 1.5;
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
        }

        @Override
        public void stop() {
            this.targetAdult = null;
            this.baby.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.targetAdult == null) {
                return;
            }

            this.baby.getLookControl().setLookAt(this.targetAdult);
            
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.adjustedTickDelay(10);
                double distance = this.baby.distanceTo(this.targetAdult);

                if (distance <= 2.5 && 
                    this.targetAdult.canAddPassenger(this.baby) && 
                    this.baby.canRideAdult() &&
                    !this.targetAdult.isPlayingDead()) {
                    this.baby.startRiding(this.targetAdult, true);
                    this.baby.setHasRidden(true);
                    return;
                }

                if (distance > this.stopDistance) {
                    this.baby.getNavigation().moveTo(this.targetAdult, this.speedModifier);
                } else {
                    this.baby.getNavigation().stop();
                }
            }
        }

        private OpossumEntity findNearestAdult() {
            double closestDistance = Double.MAX_VALUE;
            OpossumEntity closestAdult = null;

            for (Entity entity : this.baby.level().getEntitiesOfClass(OpossumEntity.class, 
                    this.baby.getBoundingBox().inflate(this.startDistance))) {
                
                if (entity instanceof OpossumEntity opossum && 
                    !opossum.isBaby() && 
                    opossum != this.baby &&
                    opossum.isAlive()) {
                    
                    double distance = this.baby.distanceTo(opossum);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestAdult = opossum;
                    }
                }
            }

            return closestAdult;
        }
    }
    
    public class SleepGoal extends Goal {
        private final int countdownTime;
        private int countdown;

        public SleepGoal(int countdownTime) {
            this.countdownTime = countdownTime;
            this.countdown = OpossumEntity.this.random.nextInt(reducedTickDelay(countdownTime));
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        public boolean canUse() {
            if (OpossumEntity.this.isPlayingDead()) {
                return false;
            }
            
            if (OpossumEntity.this.xxa == 0.0F && OpossumEntity.this.yya == 0.0F && OpossumEntity.this.zza == 0.0F) {
                return this.canSleep() || OpossumEntity.this.isSleeping();
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            if (OpossumEntity.this.isPlayingDead()) {
                return false;
            }
            
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return OpossumEntity.this.level().isNight();
            }
        }

        public void stop() {
            OpossumEntity.this.setSleeping(false);
            this.countdown = OpossumEntity.this.random.nextInt(this.countdownTime);
        }

        public void start() {
            OpossumEntity.this.setJumping(false);
            OpossumEntity.this.setSleeping(true);
            OpossumEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (OpossumEntity.this.isSleeping()) {
                OpossumEntity.this.getNavigation().stop();
            }
        }
    }
    
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean flag = target.hurt(this.damageSources().mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
    	if (target instanceof MouseEntity || target instanceof Rabbit) {
            return super.doHurtTarget(target);
        }
		return flag;
    }
    
    @SuppressWarnings("resource")
	@Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        
        if (source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            return false;
        }
        
        if (this.isSleeping()) {
            this.setSleeping(false);
        }
        
        if (!this.level().isClientSide && this.getHealth() <= 8.0F && !this.isPlayingDead() && this.getPlayDeadCooldown() <= 0) {
            this.setShouldPlayDead(true);
        }
        
        if (!this.level().isClientSide) {
            this.dropMouthItem();
        }
        
        if (!this.isBaby()) {
            if (this.hasBabyPassengers()) {
                this.dismountBabies();
            }
        }
        
        if (this.isBaby() && this.isPassenger()) {
            Entity adult = this.getVehicle();

            this.stopRiding();
            this.setHasRidden(false);
            
            this.setRidingCooldown(100);
            
            if (this.getHealth() <= 8.0F) {
                this.setPlayDeadCooldown(20);
                this.setShouldPlayDead(true);
            }

            if (source.getEntity() instanceof Player player && adult instanceof OpossumEntity adultOpossum) {
                adultOpossum.setTarget(player);
                adultOpossum.setAggressive(true);
                this.playSound(FaunifySounds.OPOSSUM_ANGRY.get(), 1.0F, 1.0F);
            }
        }

        return result;
    }
    
    public OpossumEntity.Variant getVariant() {
        return OpossumEntity.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
     }

     public void setVariant(OpossumEntity.Variant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.id);
     }
     
     public static enum Variant implements StringRepresentable {
         REGULAR(0, "regular"),
         LIGHT(1, "light"),
         DARK(2, "dark");

         public static final Codec<OpossumEntity.Variant> CODEC = StringRepresentable.fromEnum(OpossumEntity.Variant::values);
         private static final IntFunction<OpossumEntity.Variant> BY_ID = ByIdMap.continuous(OpossumEntity.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
         final int id;
         private final String name;

         private Variant(int p_262571_, String p_262693_) {
            this.id = p_262571_;
            this.name = p_262693_;
         }

         public int getId() {
            return this.id;
         }

         public static OpossumEntity.Variant byId(int p_262643_) {
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

         if (item == Items.SWEET_BERRIES) {
             if (!this.isInLove() && this.getAge() == 0) {
                 if (!this.level().isClientSide) {
                     if (this.hasBabyPassengers()) {
                         this.dismountBabies();
                     }
                     this.setInLove(player);
                     if (!player.getAbilities().instabuild) {
                         itemstack.shrink(1);
                     }
                 }
                 return InteractionResult.SUCCESS;
             }
         }

         return super.mobInteract(player, hand);
     }
     
     @Override
     public boolean isFood(ItemStack stack) {
         return stack.getItem() == Items.SWEET_BERRIES;
     }
     
     @Override
     public boolean canAddPassenger(Entity passenger) {
         if (!(passenger instanceof OpossumEntity)) {
             return false;
         }
         OpossumEntity opossumPassenger = (OpossumEntity) passenger;
         return !this.isBaby() && 
                opossumPassenger.isBaby() && 
                this.getPassengers().size() < 2 && 
                !this.isPlayingDead();
     }

     @Override
     public boolean isPushable() {
         return true;
     }
     
     public void setHasRidden(boolean value) {
    	    this.entityData.set(HAS_RIDDEN, value);
    	}

    	public boolean hasRidden() {
    	    return this.entityData.get(HAS_RIDDEN);
    	}
    	
    	@Override
    	protected void positionRider(Entity passenger, MoveFunction moveFunction) {
    	    super.positionRider(passenger, moveFunction);

    	    if (!(passenger instanceof OpossumEntity)) return;

    	    int currentBabyRiders = 0;
    	    for (Entity e : this.getPassengers()) {
    	        if (e instanceof OpossumEntity opossum && opossum.isBaby()) {
    	            currentBabyRiders++;
    	        }
    	    }

    	    int index = this.getPassengers().indexOf(passenger);
    	    if (index == -1) return;

    	    double sideOffset = 0;

    	    if (currentBabyRiders == 2) {
    	        if (index == 0) sideOffset = -0.15;
    	        else if (index == 1) sideOffset = 0.15;
    	    } else {
    	        sideOffset = 0;
    	    }

    	    float yaw = this.getYRot();
    	    double radians = Math.toRadians(yaw + 180);
    	    double offsetX = Math.cos(radians) * sideOffset;
    	    double offsetZ = Math.sin(radians) * sideOffset;

    	    double yOffset = this.getBbHeight() * 0.8;
    	    passenger.setPos(this.getX() + offsetX, this.getY() + yOffset, this.getZ() + offsetZ);
    	}

    	
    	@Override
    	public void ageUp(int growthSeconds, boolean forced) {
    	    super.ageUp(growthSeconds, forced);

    	    if (!this.isBaby() && this.isPassenger()) {
    	        this.stopRiding();
    	        this.setHasRidden(false);
    	    }
    	}
    	
    	public void setBabyCount(int count) {
    	    this.entityData.set(BABY_COUNT, count);
    	}

    	public int getBabyCount() {
    	    return this.entityData.get(BABY_COUNT);
    	}
    	
    	public void setAggressive(boolean angry) {
    	    this.aggressive = angry;
    	}

    	public boolean isAggressive() {
    	    return this.aggressive;
    	}
    	
    	public void setRidingCooldown(int ticks) {
    	    this.entityData.set(RIDING_COOLDOWN, ticks);
    	}

    	public int getRidingCooldown() {
    	    return this.entityData.get(RIDING_COOLDOWN);
    	}

    	public boolean canRideAdult() {
    	    return this.getRidingCooldown() <= 0;
    	}
    	
    	public void setPlayDeadCooldown(int ticks) {
    	    this.entityData.set(PLAY_DEAD_COOLDOWN, ticks);
    	}

    	public int getPlayDeadCooldown() {
    	    return this.entityData.get(PLAY_DEAD_COOLDOWN);
    	}
}