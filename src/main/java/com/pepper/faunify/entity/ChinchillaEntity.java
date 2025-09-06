package com.pepper.faunify.entity;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.pepper.faunify.particle.FaunifyParticleTypes;
import com.pepper.faunify.registry.FaunifyBlocks;
import com.pepper.faunify.registry.FaunifyEntities;
import com.pepper.faunify.registry.FaunifySounds;


public class ChinchillaEntity extends TamableAnimal implements GeoEntity {
	private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(ChinchillaEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(ChinchillaEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> BATHING = SynchedEntityData.defineId(ChinchillaEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private boolean orderedToSit;
    public static final float DEFAULT_HEALTH = 8.0F;
    public static final float TAMED_HEALTH = 20.0F;

    public ChinchillaEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
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
        this.setVariant(Util.getRandom(ChinchillaEntity.Variant.values(), world.getRandom()));
        super.finalizeSpawn(world, difficulty, spawnReason, spawnData, dataTag);
       
        if (spawnData == null) {
            spawnData = new AgeableMob.AgeableMobGroupData(false);
        }
		return spawnData;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, DEFAULT_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLEEPING, false);
        this.entityData.define(BATHING, false);
        this.entityData.define(DATA_VARIANT_ID, 0);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id);
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putBoolean("Bathing", this.isDustBathing());
        tag.putBoolean("Sitting", this.orderedToSit);
        tag.putDouble("PosX", this.getX());
        tag.putDouble("PosY", this.getY());
        tag.putDouble("PosZ", this.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(ChinchillaEntity.Variant.byId(tag.getInt("Variant")));
        this.orderedToSit = tag.getBoolean("Sitting");
        this.setSleeping(tag.getBoolean("Sleeping"));
        this.setDustBathing(tag.getBoolean("Bathing"));
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
                return !ChinchillaEntity.this.isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(1, new DustBatheGoal(this));
        this.goalSelector.addGoal(1, new DustBatheSuspiciousGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.3D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.3D, Ingredient.of(Items.WHEAT), false));
        this.goalSelector.addGoal(2, new SleepGoal(200));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.3D, 10.0F, 2.0F, false) {
            @Override
            public boolean canUse() {
                return super.canUse() && !ChinchillaEntity.this.isInSittingPose();
            }
        });
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
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
    
    public static boolean canSpawn(EntityType<ChinchillaEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
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

    private PlayState predicate(AnimationState<ChinchillaEntity> event) {
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
            event.getController().setAnimation(RawAnimation.begin().then("stand", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        if (this.isDustBathing()) {

        	BlockParticleOption gravelParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAVEL.defaultBlockState());
       	    
       	    for (int i = 0; i < 3; i++) {
       	        double offsetX = (this.getRandom().nextFloat() - 0.5) * 0.5;
       	        double offsetZ = (this.getRandom().nextFloat() - 0.5) * 0.5;
       	        this.level().addParticle(gravelParticle, 
       	        		this.getX() + offsetX, 
       	        		this.getY() + 0.1,
       	        		this.getZ() + offsetZ, 
       	            0, 0, 0
       	        );
       	    }
       	    
            event.getController().setAnimation(RawAnimation.begin().then("bath", Animation.LoopType.LOOP));
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

    public class DustBatheGoal extends Goal {
        private final ChinchillaEntity chinchilla;
        private static final int DUST_BATH_DURATION = 35;

        private int dustBathTimer = 0;
        private BlockPos bathingBlock;

        public DustBatheGoal(ChinchillaEntity chinchilla) {
            this.chinchilla = chinchilla;
        }

        @Override
        public boolean canUse() {
            BlockPos pos = chinchilla.blockPosition().below();
            return chinchilla.level().getBlockState(pos).is(Blocks.GRAVEL);
        }

        @Override
        public void start() {
            bathingBlock = chinchilla.blockPosition().below();
            chinchilla.setDustBathing(true);
            dustBathTimer = DUST_BATH_DURATION;
            chinchilla.getNavigation().stop();
        }

        @Override
        public void tick() {
            chinchilla.setDeltaMovement(Vec3.ZERO);
            chinchilla.setYRot(chinchilla.yRotO);
            chinchilla.setXRot(0);

            chinchilla.playSound(SoundEvents.SAND_PLACE, 0.5F, 1.0F);

            if (dustBathTimer > 0) {
                dustBathTimer--;
            } else {
                chinchilla.setDustBathing(false);
                chinchilla.level().setBlockAndUpdate(bathingBlock, FaunifyBlocks.DUST_BLOCK.get().defaultBlockState());
            }
        }

        @Override
        public boolean canContinueToUse() {
            return chinchilla.isDustBathing()
                && chinchilla.level().getBlockState(bathingBlock).is(Blocks.GRAVEL);
        }

        @Override
        public void stop() {
            chinchilla.setDustBathing(false);
            chinchilla.getNavigation().stop();
            chinchilla.setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public EnumSet<Flag> getFlags() {
            return EnumSet.of(Flag.MOVE, Flag.LOOK);
        }
    }

    public class DustBatheSuspiciousGoal extends Goal {
        private final ChinchillaEntity chinchilla;
        private static final int DUST_BATH_DURATION = 35;
        private static final int BRUSH_STAGE_TIME = 5;

        private int dustBathTimer = 0;
        private int brushStageTimer = BRUSH_STAGE_TIME;
        private boolean isSuspicious = false;
        private boolean hasDroppedItem = false;
        private BlockPos bathingBlock;

        public DustBatheSuspiciousGoal(ChinchillaEntity chinchilla) {
            this.chinchilla = chinchilla;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            BlockPos pos = chinchilla.blockPosition().below();
            return chinchilla.level().getBlockState(pos).is(Blocks.SUSPICIOUS_GRAVEL);
        }

        @Override
        public void start() {
            bathingBlock = chinchilla.blockPosition().below();
            chinchilla.setDustBathing(true);
            chinchilla.getNavigation().stop();
            dustBathTimer = DUST_BATH_DURATION;
            brushStageTimer = BRUSH_STAGE_TIME;
            isSuspicious = chinchilla.level().getBlockState(bathingBlock).is(Blocks.SUSPICIOUS_GRAVEL);
            hasDroppedItem = false;
        }

        @Override
        public void tick() {
            chinchilla.setDeltaMovement(Vec3.ZERO);
            chinchilla.setYRot(chinchilla.yRotO);
            chinchilla.setXRot(0);

            chinchilla.playSound(SoundEvents.SAND_PLACE, 0.5F, 1.0F);

            if (dustBathTimer > 0) {
                dustBathTimer--;
            }

            if (dustBathTimer <= 0) {
                if (isSuspicious) {
                    if (brushStageTimer-- <= 0) {
                        dustAwaySuspiciousGravel(bathingBlock);
                        brushStageTimer = BRUSH_STAGE_TIME;
                    }
                } else {
                    chinchilla.level().setBlockAndUpdate(bathingBlock, FaunifyBlocks.DUST_BLOCK.get().defaultBlockState());
                    if (!hasDroppedItem) {
                        dropLootFromSuspiciousGravel();
                        hasDroppedItem = true;
                    }
                    chinchilla.setDustBathing(false);
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return chinchilla.isDustBathing()
                && chinchilla.level().getBlockState(bathingBlock).is(Blocks.SUSPICIOUS_GRAVEL);
        }

        @Override
        public void stop() {
            chinchilla.setDustBathing(false);
            chinchilla.getNavigation().stop();
            chinchilla.setDeltaMovement(Vec3.ZERO);
        }

        private void dustAwaySuspiciousGravel(BlockPos pos) {
            if (chinchilla.level() instanceof ServerLevel serverLevel) {
                BlockState state = serverLevel.getBlockState(pos);
                if (state.getBlock() instanceof BrushableBlock) {
                    BrushableBlockEntity blockEntity = (BrushableBlockEntity) serverLevel.getBlockEntity(pos);
                    if (blockEntity != null) {
                        Player nearestPlayer = serverLevel.getPlayers(p -> p.distanceTo(chinchilla) < 5.0).stream().findFirst().orElse(null);
                        if (nearestPlayer != null) {
                            blockEntity.brush(serverLevel.getGameTime(), nearestPlayer, Direction.UP);
                        }
                    }
                }
            }
        }

        private void dropLootFromSuspiciousGravel() {
            if (!(chinchilla.level() instanceof ServerLevel serverLevel)) return;

            ResourceLocation lootTableId = chinchilla.getRandom().nextInt(5) == 0
                    ? BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE
                    : BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON;

            LootTable lootTable = serverLevel.getServer().getLootData().getLootTable(lootTableId);

            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, chinchilla.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, chinchilla);

            List<ItemStack> loot = lootTable.getRandomItems(builder.create(LootContextParamSets.EMPTY));

            for (ItemStack stack : loot) {
                if (!stack.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(serverLevel, chinchilla.getX(), chinchilla.getY(), chinchilla.getZ(), stack);
                    serverLevel.addFreshEntity(itemEntity);
                }
            }
        }
    }
    
    public boolean isDustBathing() {
        return this.entityData.get(BATHING);
    }

    public void setDustBathing(boolean bathing) {
        this.entityData.set(BATHING, bathing);
        if (bathing) {
            this.getNavigation().stop();
        }
    }

    private boolean isFollowingPlayerWithFood() {
        Player player = this.level().getNearestPlayer(this, 10.0D);
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            return heldItem.getItem() == Items.WHEAT;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public ChinchillaEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob otherParent) {
    	ChinchillaEntity babyChinchilla = FaunifyEntities.CHINCHILLA.get().create(serverLevel);

        if (babyChinchilla != null && otherParent instanceof ChinchillaEntity parent) {

            Variant selectedVariant;
            selectedVariant = random.nextBoolean() ? this.getVariant() : parent.getVariant();
            babyChinchilla.setVariant(selectedVariant);
        }

        return babyChinchilla;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        int soundIndex = random.nextInt(3);
        switch (soundIndex) {
            case 0:
                return FaunifySounds.CHINCHILLA_IDLE_1.get();
            case 1:
                return FaunifySounds.CHINCHILLA_IDLE_2.get();
            case 2:
                return FaunifySounds.CHINCHILLA_IDLE_3.get();
            default:
                return FaunifySounds.CHINCHILLA_IDLE_1.get();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return FaunifySounds.CHINCHILLA_HURT.get();
    }
    
    public class SleepGoal extends Goal {
        private final int countdownTime;
        private int countdown;

        public SleepGoal(int countdownTime) {
            this.countdownTime = countdownTime;
            this.countdown = ChinchillaEntity.this.random.nextInt(reducedTickDelay(countdownTime));
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        public boolean canUse() {
            if (ChinchillaEntity.this.isDustBathing()) {
                return false;
            }
            
            if (ChinchillaEntity.this.isTame() && !ChinchillaEntity.this.isInSittingPose()) {
                return false;
            }
            
            if (ChinchillaEntity.this.xxa == 0.0F && ChinchillaEntity.this.yya == 0.0F && ChinchillaEntity.this.zza == 0.0F) {
                return this.canSleep() || ChinchillaEntity.this.isSleeping();
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            if (ChinchillaEntity.this.isDustBathing()) {
                return false;
            }
            
            if (ChinchillaEntity.this.isTame() && !ChinchillaEntity.this.isInSittingPose()) {
                return false;
            }
            
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return ChinchillaEntity.this.level().isNight();
            }
        }

        public void stop() {
            ChinchillaEntity.this.setSleeping(false);
            this.countdown = ChinchillaEntity.this.random.nextInt(this.countdownTime);
        }

        public void start() {
            ChinchillaEntity.this.setJumping(false);
            ChinchillaEntity.this.setSleeping(true);
            ChinchillaEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (ChinchillaEntity.this.isSleeping()) {
                ChinchillaEntity.this.getNavigation().stop();
            }
        }
    }
    
    @SuppressWarnings("resource")
	@Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (source == null) {
            return false;
        }
        
        DamageSource safeSource = source;
        if (this.isTame() && source.getEntity() == null) {
            safeSource = new DamageSource(source.typeHolder(), this, source.getDirectEntity());
        }
        
        try {
            boolean result = super.hurt(safeSource, amount);
            
            if (result) {
                if (this.isSleeping()) {
                    this.setSleeping(false);
                }
                
                if (this.isInSittingPose()) {
                    this.setInSittingPose(false);
                    this.setOrderedToSit(false);
                }
            }
            
            return result;
            
        } catch (NullPointerException e) {
            
            if (!this.isInvulnerableTo(source) && !this.level().isClientSide) {
                float oldHealth = this.getHealth();
                this.setHealth(Math.max(0, oldHealth - amount));
                
                if (this.isSleeping()) {
                    this.setSleeping(false);
                }
                
                if (this.isInSittingPose()) {
                    this.setInSittingPose(false);
                    this.setOrderedToSit(false);
                }
                
                this.playHurtSound(source);
                
                return amount > 0.0F;
            }
            
            return false;
        }
    }
    
    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        try {
            return super.isInvulnerableTo(damageSource);
        } catch (NullPointerException e) {
            return false;
        }
    }
    
    public ChinchillaEntity.Variant getVariant() {
        return ChinchillaEntity.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
     }

     public void setVariant(ChinchillaEntity.Variant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.id);
     }
     
     public static enum Variant implements StringRepresentable {
         GREY(0, "grey"),
         BROWN(1, "brown"),
         BLACK(2, "black"),
         WHITE(3, "white");

         public static final Codec<ChinchillaEntity.Variant> CODEC = StringRepresentable.fromEnum(ChinchillaEntity.Variant::values);
         private static final IntFunction<ChinchillaEntity.Variant> BY_ID = ByIdMap.continuous(ChinchillaEntity.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
         final int id;
         private final String name;

         private Variant(int p_262571_, String p_262693_) {
            this.id = p_262571_;
            this.name = p_262693_;
         }

         public int getId() {
            return this.id;
         }

         public static ChinchillaEntity.Variant byId(int p_262643_) {
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
        	 
             if (item == Items.DANDELION) {
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

             if (item == Items.WHEAT) {
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
             
             if (itemstack.isEmpty() || item != Items.WHEAT && item != Items.DANDELION) {
                 toggleSittingState();
                 return InteractionResult.SUCCESS;
             }
         }

         if (item == Items.DANDELION && !this.isTame()) {
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
         return stack.getItem() == Items.WHEAT;
     }
}
