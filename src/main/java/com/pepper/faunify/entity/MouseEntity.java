package com.pepper.faunify.entity;

import java.util.Arrays;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
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


public class MouseEntity extends TamableAnimal implements GeoEntity {
	private static final int STEAL_RADIUS = 10;
    private static final int COOLDOWN_TICKS = 100; 
    private long lastStealTime = 0;
    private ItemStack stolenSeed = ItemStack.EMPTY;
    private boolean isRunning = false;
	private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(MouseEntity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public MouseEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
    	super(entityType, world);
    }
    
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
    	this.setVariant(MouseEntity.Variant.getCommonSpawnVariant(world.getRandom()));
        super.finalizeSpawn(world, difficulty, spawnReason, spawnData, dataTag);
        if (spawnData == null) {
            spawnData = new AgeableMob.AgeableMobGroupData(false);
        }
		return spawnData;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.5)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT_ID, 0);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().id);
        tag.putDouble("PosX", this.getX());
        tag.putDouble("PosY", this.getY());
        tag.putDouble("PosZ", this.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(MouseEntity.Variant.byId(tag.getInt("Variant")));
        if (tag.contains("PosX") && tag.contains("PosY") && tag.contains("PosZ")) {
            double posX = tag.getDouble("PosX");
            double posY = tag.getDouble("PosY");
            double posZ = tag.getDouble("PosZ");
            this.setPos(posX, posY, posZ);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new StealSeedGoal(this));
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.25D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.35D, Ingredient.of(Items.SWEET_BERRIES), false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }
    
    public static boolean canSpawn(EntityType<MouseEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
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

    private PlayState predicate(AnimationState<MouseEntity> event) {
        LivingEntity target = this.getTarget();
        if (target != null && !target.isAlive()) {
            this.setTarget(null);
            target = null;
        }
        if (this.isRunning) {
            event.getController().setAnimation(RawAnimation.begin().then("run", Animation.LoopType.LOOP));
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
            return heldItem.is(Items.WHEAT_SEEDS) 
                || heldItem.is(Items.BEETROOT_SEEDS) 
                || heldItem.is(Items.PUMPKIN_SEEDS) 
                || heldItem.is(Items.MELON_SEEDS);
        }
        return false;
    }

    @Override
    public MouseEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob otherParent) {
        MouseEntity babyMouse = FaunifyEntities.MOUSE.get().create(serverLevel);

        if (babyMouse != null && otherParent instanceof MouseEntity parent) {
            RandomSource random = this.getRandom(); 
            
            Variant selectedVariant;
            
            if (random.nextFloat() < 0.05f) { 
                selectedVariant = Variant.getRareBreedVariant(random);
            } else {
                selectedVariant = random.nextBoolean() ? this.getVariant() : parent.getVariant();
            }

            babyMouse.setVariant(selectedVariant);
        }

        return babyMouse;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        int soundIndex = random.nextInt(2);
        switch (soundIndex) {
            case 0:
                return FaunifySounds.MOUSE_IDLE_1.get();
            case 1:
                return FaunifySounds.MOUSE_IDLE_2.get();
            default:
                return FaunifySounds.MOUSE_IDLE_1.get();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return FaunifySounds.MOUSE_HURT.get();
    }
    
    @Override
    public void tick() {
        super.tick();
    }
    
    public MouseEntity.Variant getVariant() {
        return MouseEntity.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
     }

     public void setVariant(MouseEntity.Variant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.id);
     }
     
     public static enum Variant implements StringRepresentable {
 	    FIELD(0, "field", true),
 	    GREY(1, "grey", true),
 	    WOOD(2, "wood", true),
 	    WHITE(3, "white", true),
 	    BLACK(4, "black", true),
 	    BROWNBELLY(5, "brownbelly", true),
 	    ALBINO(6, "albino", false);

 	    public static final Codec<MouseEntity.Variant> CODEC = StringRepresentable.fromEnum(MouseEntity.Variant::values);
 	    private static final IntFunction<MouseEntity.Variant> BY_ID = ByIdMap.continuous(MouseEntity.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
 	    
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

 	    public static MouseEntity.Variant byId(int id) {
 	        return BY_ID.apply(id);
 	    }

 	    public static MouseEntity.Variant getCommonSpawnVariant(RandomSource random) {
 	        return getSpawnVariant(random, true);
 	    }

 	    public static MouseEntity.Variant getRareBreedVariant(RandomSource random) {
 	        return getSpawnVariant(random, false);
 	    }

 	    private static MouseEntity.Variant getSpawnVariant(RandomSource random, boolean isCommon) {
 	        MouseEntity.Variant[] validVariants = Arrays.stream(values())
 	            .filter(variant -> variant.common == isCommon)
 	            .toArray(MouseEntity.Variant[]::new);
 	        return Util.getRandom(validVariants, random);
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
                     this.setInLove(player);
                     if (!player.getAbilities().instabuild) {
                         itemstack.shrink(1);
                     }
                 }
             }
             return InteractionResult.SUCCESS;
         }

         return super.mobInteract(player, hand);
     }
     
     @Override
     public boolean isFood(ItemStack stack) {
         return stack.getItem() == Items.SWEET_BERRIES;
     }
     
     public boolean canSteal() {
         return this.level().getGameTime() - lastStealTime > COOLDOWN_TICKS;
     }

     public void setLastStealTime() {
         lastStealTime = this.level().getGameTime();
     }
     
     public boolean isRunning() {
         return this.isRunning;
     }

     @Override
     public void die(DamageSource cause) {
         super.die(cause);
         if (!stolenSeed.isEmpty()) {
             this.spawnAtLocation(stolenSeed);
         }
     }

     private static class StealSeedGoal extends Goal {
         private final MouseEntity mouse;
         private Player targetPlayer;

         public StealSeedGoal(MouseEntity mouse) {
             this.mouse = mouse;
         }

         @Override
         public boolean canUse() {
             if (!mouse.canSteal()) return false;

             List<Player> players = mouse.level().getEntitiesOfClass(Player.class, 
                 mouse.getBoundingBox().inflate(STEAL_RADIUS));
             
             for (Player player : players) {
                 if (hasSeeds(player)) {
                     this.targetPlayer = player;
                     return true;
                 }
             }
             return false;
         }

         @Override
         public void start() {
             if (targetPlayer != null) {
                 mouse.getNavigation().moveTo(targetPlayer, 0.75);
             }
         }

         @Override
         public void tick() {
             if (targetPlayer == null || !targetPlayer.isAlive()) return;

             double distance = mouse.distanceTo(targetPlayer);

             if (distance > 1.5) { 
                 mouse.getNavigation().moveTo(targetPlayer, 0.75);
             } else { 
                 stealSeed(targetPlayer);
                 mouse.setLastStealTime();
                 fleeFrom(targetPlayer);
             }
         }

         private boolean hasSeeds(Player player) {
             for (ItemStack stack : player.getInventory().items) {
                 if (isSeed(stack)) return true;
             }
             return false;
         }

         private void stealSeed(Player player) {
             for (int i = 0; i < player.getInventory().items.size(); i++) {
                 ItemStack stack = player.getInventory().items.get(i);
                 if (isSeed(stack)) {
                     ItemStack stolen = stack.split(1);
                     mouse.stolenSeed = stolen;
                     mouse.playSound(FaunifySounds.MOUSE_STEAL.get(), 1.0F, 1.0F);
                     break;
                 }
             }
         }

         private boolean isSeed(ItemStack stack) {
             return stack.is(Items.WHEAT_SEEDS) ||
                    stack.is(Items.BEETROOT_SEEDS) ||
                    stack.is(Items.PUMPKIN_SEEDS) ||
                    stack.is(Items.MELON_SEEDS);
         }

         private void fleeFrom(Player player) {
             mouse.isRunning = true;

             Vec3 fleeDirection = mouse.position().subtract(player.position()).normalize().scale(20);
             mouse.getNavigation().moveTo(mouse.getX() + fleeDirection.x, mouse.getY(), mouse.getZ() + fleeDirection.z, 2);

             if (mouse.distanceTo(player) > 25) {
                 mouse.isRunning = false;
             }
         }
     }
 }
