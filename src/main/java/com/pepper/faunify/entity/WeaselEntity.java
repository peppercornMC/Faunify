package com.pepper.faunify.entity;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.Animation;

import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.pepper.faunify.particle.FaunifyParticleTypes;
import com.pepper.faunify.registry.FaunifyEntities;
import com.pepper.faunify.registry.FaunifyItems;
import com.pepper.faunify.registry.FaunifySounds;


public class WeaselEntity extends TamableAnimal implements GeoEntity {
	private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(WeaselEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(WeaselEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DIGGING = SynchedEntityData.defineId(WeaselEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> SNOWY_VARIANT = SynchedEntityData.defineId(WeaselEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private boolean orderedToSit;
    private boolean hasBittenPlayer = false;
    private long lastDigTime = 0L;
    public static final float DEFAULT_HEALTH = 8.0F;
    public static final float TAMED_HEALTH = 20.0F;

    public WeaselEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
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
    	this.setVariant(WeaselEntity.Variant.getCommonSpawnVariant(world.getRandom()));
        super.finalizeSpawn(world, difficulty, spawnReason, spawnData, dataTag);
        
        if (this.isInSnowyBiome()) {
            this.setSnowVariant(true);
        } else {
            this.setSnowVariant(false);
        }
       
        if (spawnData == null) {
            spawnData = new AgeableMob.AgeableMobGroupData(false);
        }
		return spawnData;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, DEFAULT_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.5D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DIGGING, false);
        this.entityData.define(SLEEPING, false);
        this.entityData.define(SNOWY_VARIANT, false);
        this.entityData.define(DATA_VARIANT_ID, 0);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id);
        tag.putBoolean("IsSnowVariant", this.entityData.get(SNOWY_VARIANT));
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putBoolean("Sitting", this.orderedToSit);
        tag.putDouble("PosX", this.getX());
        tag.putDouble("PosY", this.getY());
        tag.putDouble("PosZ", this.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(WeaselEntity.Variant.byId(tag.getInt("Variant")));
        this.entityData.set(SNOWY_VARIANT, tag.getBoolean("IsSnowVariant"));
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
    	this.goalSelector.addGoal(1, new FloatGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.35D, true)); 
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new DigGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.25D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.35D, Ingredient.of(Items.CHICKEN), false));
        this.goalSelector.addGoal(4, new SleepGoal(200));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.3D, 10.0F, 2.0F, false) {
            @Override
            public boolean canUse() {
                return super.canUse() && !WeaselEntity.this.isInSittingPose();
            }
        });
        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, Chicken.class, true));
        this.targetSelector.addGoal(8, new NearestAttackableTargetGoal<>(this, Rabbit.class, true));
        this.targetSelector.addGoal(9, new NearestAttackableTargetGoal<>(this, MouseEntity.class, true));
        this.targetSelector.addGoal(9, new NearestAttackableTargetGoal<>(this, HedgehogEntity.class, true, (hedgehog) -> {
            if (hedgehog instanceof TamableAnimal tamableHedgehog && tamableHedgehog.isTame() && this.isTame()) {
                return false;
            }
            return true;
        }));
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
    
    public boolean isDigging() {
        return this.entityData.get(DIGGING);
    }

    public void setDigging(boolean digging) {
        this.entityData.set(DIGGING, digging);
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
    
    public static boolean canSpawn(EntityType<WeaselEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
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

    private PlayState predicate(AnimationState<WeaselEntity> event) {
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
        
        if (this.isDigging()) {
            event.getController().setAnimation(RawAnimation.begin().then("dig", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        if (target instanceof Chicken || target instanceof Rabbit || target instanceof HedgehogEntity) {
            if (event.isMoving()) {
                event.getController().setAnimation(RawAnimation.begin().then("run", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            }
            else {
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
            return heldItem.getItem() == Items.CHICKEN;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Nullable
    @Override
    public WeaselEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob otherParent) {
        WeaselEntity babyWeasel = FaunifyEntities.WEASEL.get().create(serverLevel);

        if (babyWeasel != null && otherParent instanceof WeaselEntity parent) {
            boolean thisIsSnowy = this.isSnowVariant();
            boolean parentIsSnowy = parent.isSnowVariant();
            WeaselEntity.Variant selectedVariant;

            if (thisIsSnowy && parentIsSnowy) {
                babyWeasel.setSnowVariant(true);
                selectedVariant = WeaselEntity.Variant.STOAT;
            } 
            else if (thisIsSnowy || parentIsSnowy) {
                babyWeasel.setSnowVariant(false);
                selectedVariant = thisIsSnowy ? parent.getVariant() : this.getVariant();
            } 
            else {
                if (this.isTame() && parent.isTame() && this.random.nextBoolean()) {
                    selectedVariant = WeaselEntity.Variant.getRareBreedVariant(this.random);
                } else {
                    selectedVariant = this.random.nextBoolean() ? this.getVariant() : parent.getVariant();
                }
            }

            babyWeasel.setVariant(selectedVariant);
            babyWeasel.setPersistenceRequired();
        }

        return babyWeasel;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        int soundIndex = random.nextInt(3);
        switch (soundIndex) {
            case 0:
                return FaunifySounds.WEASEL_IDLE_1.get();
            case 1:
                return FaunifySounds.WEASEL_IDLE_2.get();
            case 2:
                return FaunifySounds.WEASEL_IDLE_3.get();
            default:
                return FaunifySounds.WEASEL_IDLE_1.get();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return FaunifySounds.WEASEL_HURT.get();
    }
    
    private class DigGoal extends Goal {
        private final WeaselEntity weasel;
        private ItemEntity targetEgg;
        private static final double SEARCH_RADIUS = 5.0D;
        private int diggingTime;
        private static final int DIGGING_DURATION = 25;
        private boolean hasPickedUpEgg = false;
        private static final long DIG_COOLDOWN = TimeUnit.MINUTES.toMillis(3);

        public DigGoal(WeaselEntity weasel) {
            this.weasel = weasel;
        }

        @Override
        public boolean canUse() {
            if (this.weasel.isInSittingPose()) return false;

            long currentTime = System.currentTimeMillis();
            if ((currentTime - weasel.lastDigTime) < DIG_COOLDOWN) return false;

            List<ItemEntity> nearbyItems = weasel.level().getEntitiesOfClass(ItemEntity.class,
                weasel.getBoundingBox().inflate(SEARCH_RADIUS),
                itemEntity -> itemEntity.getItem().is(FaunifyItems.EGGS)
            );

            if (!nearbyItems.isEmpty()) {
                targetEgg = nearbyItems.get(0);
                return true;
            }

            return false;
        }

        @Override
        public void start() {
            diggingTime = 0;
            hasPickedUpEgg = false;
            weasel.getNavigation().moveTo(targetEgg, 1.0D);
        }

        @Override
        public void tick() {
            BlockPos weaselPos = weasel.blockPosition().below();
            BlockState blockState = weasel.level().getBlockState(weaselPos);
            if (blockState.is(BlockTags.DIRT) || blockState.is(Blocks.GRAVEL) || blockState.is(BlockTags.SAND)) {
                if (targetEgg != null && weasel.distanceTo(targetEgg) < 1.0D) {
                    targetEgg.discard();
                    if (!hasPickedUpEgg) {
                        weasel.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                        hasPickedUpEgg = true;
                    }

                    if (diggingTime == 0) {
                        weasel.getNavigation().stop();
                        weasel.setDigging(true);
                    }

                    diggingTime++;

                    if (weasel.level() instanceof ServerLevel serverLevel) {
                        double d0 = WeaselEntity.this.random.nextGaussian() * 0.01D;
                        double d1 = WeaselEntity.this.random.nextGaussian() * 0.01D;
                        double d2 = WeaselEntity.this.random.nextGaussian() * 0.01D;

                        double offsetX = -Math.sin(Math.toRadians(weasel.getYRot())) * 0.5;
                        double offsetZ = Math.cos(Math.toRadians(weasel.getYRot())) * 0.5;

                        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                                weasel.getX() + offsetX, weasel.getY(), weasel.getZ() + offsetZ, 2, d0, d1, d2, 0.1D);
                        weasel.playSound(SoundEvents.GRAVEL_BREAK, 0.5F, 1.0F);
                    }

                    weasel.setNoGravity(true);
                    weasel.setDeltaMovement(0, 0, 0);

                    if (diggingTime >= DIGGING_DURATION) {
                        dropRandomLoot();
                        weasel.lastDigTime = System.currentTimeMillis();
                        targetEgg = null;
                        weasel.setNoGravity(false);
                        weasel.setDigging(false);
                    }
                } else if (targetEgg != null) {
                    weasel.getNavigation().moveTo(targetEgg, 1.0D);
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (this.weasel.isInSittingPose()) {
                return false;
            } else {
                return targetEgg != null;
            }
        }

        private void dropRandomLoot() {
            weasel.playSound(SoundEvents.GRAVEL_BREAK, 1.0F, 1.0F);
            RandomSource random = weasel.getRandom();
            if (random.nextInt(100) < 1) {
                weasel.spawnItem(Items.NAME_TAG);
            } else {
                int dropType = random.nextInt(6);
                switch (dropType) {
                    case 0 -> weasel.spawnItem(Items.RAW_IRON);
                    case 1 -> weasel.spawnItem(Items.COAL);
                    case 2 -> weasel.spawnItem(Items.BONE);
                    case 3 -> weasel.spawnItem(Items.PUMPKIN_SEEDS);
                    case 4 -> weasel.spawnItem(Items.BEETROOT_SEEDS);
                    case 5 -> weasel.spawnItem(Items.WHEAT_SEEDS);
                }
            }
        }
    }

    private void spawnItem(Item item) {
        ItemStack stack = new ItemStack(item, 1);
        this.spawnAtLocation(stack);
    }
    
    public class SleepGoal extends Goal {
        private final int countdownTime;
        private int countdown;

        public SleepGoal(int countdownTime) {
            this.countdownTime = countdownTime;
            this.countdown = WeaselEntity.this.random.nextInt(reducedTickDelay(countdownTime));
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        public boolean canUse() {
            if (WeaselEntity.this.isDigging()) {
                return false;
            }
            
            if (WeaselEntity.this.isTame() && !WeaselEntity.this.isInSittingPose()) {
                return false;
            }
            
            if (WeaselEntity.this.xxa == 0.0F && WeaselEntity.this.yya == 0.0F && WeaselEntity.this.zza == 0.0F) {
                return this.canSleep() || WeaselEntity.this.isSleeping();
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            if (WeaselEntity.this.isDigging()) {
                return false;
            }
            
            if (WeaselEntity.this.isTame() && !WeaselEntity.this.isInSittingPose()) {
                return false;
            }
            
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return WeaselEntity.this.level().isNight();
            }
        }

        public void stop() {
            WeaselEntity.this.setSleeping(false);
            this.countdown = WeaselEntity.this.random.nextInt(this.countdownTime);
        }

        public void start() {
            WeaselEntity.this.setJumping(false);
            WeaselEntity.this.setSleeping(true);
            WeaselEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (WeaselEntity.this.isSleeping()) {
                WeaselEntity.this.getNavigation().stop();
            }
        }
    }
    
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean flag = target.hurt(this.damageSources().mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
    	if (target instanceof Chicken || target instanceof Rabbit || target instanceof HedgehogEntity) {
            return super.doHurtTarget(target);
        }
    	
        if (target instanceof Player && !hasBittenPlayer) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.4D, 0.0D));
            this.playSound(FaunifySounds.WEASEL_BITE.get(), 1.0F, 1.0F);

            hasBittenPlayer = true;

            this.setTarget(null);
            this.setLastHurtByMob(null);

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

        if (source.getEntity() instanceof Player player) {
            if (!player.equals(this.getOwner())) {
                this.setTarget((LivingEntity) source.getEntity());
            }
            hasBittenPlayer = false;
        }

        return result;
    }
    
    public boolean isInSnowyBiome() {
        Holder<Biome> biomeHolder = this.level().getBiome(this.blockPosition());
        boolean isSnowy = biomeHolder.is(Tags.Biomes.IS_SNOWY);
        return isSnowy;
    }
    
    public boolean isSnowVariant() {
        return this.entityData.get(SNOWY_VARIANT) != null && this.entityData.get(SNOWY_VARIANT);
    }

    public void setSnowVariant(boolean isSnowy) {
        this.entityData.set(SNOWY_VARIANT, isSnowy);
    }
    
    public WeaselEntity.Variant getVariant() {
        return WeaselEntity.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
     }

     public void setVariant(WeaselEntity.Variant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.id);
     }
     
     public static enum Variant implements StringRepresentable {
    	    STOAT(0, "stoat", true),
    	    STEPPE(1, "steppe", true),
    	    EUROPEAN(2, "european", true),
    	    SIBERIAN(3, "siberian", true),
    	    YELLOWBELLIED(4, "yellowbellied", true),
    	    FERRETDARK(5, "ferretdark", false),
    	    FERRETLIGHT(6, "ferretlight", false);

    	    public static final Codec<WeaselEntity.Variant> CODEC = StringRepresentable.fromEnum(WeaselEntity.Variant::values);
    	    private static final IntFunction<WeaselEntity.Variant> BY_ID = ByIdMap.continuous(WeaselEntity.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
    	    
    	    private final int id;
    	    private final String name;
    	    private final boolean common;

    	    private Variant(int id, String name, boolean common) {
    	        this.id = id;
    	        this.name = name;
    	        this.common = common;
    	    }

    	    public int getId() {
    	        return this.id;
    	    }

    	    public String getSerializedName() {
    	        return this.name;
    	    }

    	    public static WeaselEntity.Variant byId(int id) {
    	        return BY_ID.apply(id);
    	    }

    	    public static WeaselEntity.Variant getCommonSpawnVariant(RandomSource random) {
    	        return getSpawnVariant(random, true);
    	    }

    	    public static WeaselEntity.Variant getRareBreedVariant(RandomSource random) {
    	        return getSpawnVariant(random, false);
    	    }

    	    private static WeaselEntity.Variant getSpawnVariant(RandomSource random, boolean isCommon) {
    	        WeaselEntity.Variant[] validVariants = Arrays.stream(values())
    	            .filter(variant -> variant.common == isCommon)
    	            .toArray(WeaselEntity.Variant[]::new);
    	        return Util.getRandom(validVariants, random);
    	    }
    	}
     
     @SuppressWarnings("resource")
     @Override
     public InteractionResult mobInteract(Player player, InteractionHand hand) {
         ItemStack itemstack = player.getItemInHand(hand);
         Item item = itemstack.getItem();

         if (this.isTame() && this.isOwnedBy(player)) {
        	 
        	 if (this.isDigging()) {
                 return InteractionResult.PASS;
             }
        	 
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

             if (item == Items.CHICKEN) {
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
     public void setTarget(@Nullable LivingEntity target) {
         if (this.isTame() && this.getOwner() != null && target == this.getOwner()) {
             return;
         }
         
         if (this.isInSittingPose()) {
             return;
         }
         
         super.setTarget(target);
     }
     
     @Override
     public boolean isFood(ItemStack stack) {
         return stack.getItem() == Items.CHICKEN;
     }
}
