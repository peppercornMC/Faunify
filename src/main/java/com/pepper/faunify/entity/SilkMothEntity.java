package com.pepper.faunify.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
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

import java.util.Comparator;
import javax.annotation.Nullable;

import com.pepper.faunify.registry.FaunifySounds;


public class SilkMothEntity extends TamableAnimal implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private BlockPos targetPosition;

    public SilkMothEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
    	super(entityType, world);
    }
    
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
    }
    
    public void tick() {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Level level = this.level();

        if (level.isNight()) {
            BlockPos lightSourcePos = findHighestPriorityLightSource();
            if (lightSourcePos != null) {
                Vec3 targetAroundLight = getDynamicLightTarget(lightSourcePos);
                double distanceSq = this.position().distanceToSqr(targetAroundLight);
                if (distanceSq > 2.0) {
                    moveTowardsTarget(targetAroundLight);
                }
            } else {
                randomMovement();
            }
        } else {
            randomMovement();
        }
    }

    @SuppressWarnings("deprecation")
	private BlockPos findHighestPriorityLightSource() {
        Level level = this.level();
        BlockPos mothPos = this.blockPosition();

        return BlockPos.betweenClosedStream(
                mothPos.offset(-15, -15, -15),
                mothPos.offset(15, 15, 15))
            .map(BlockPos::immutable)
            .filter(pos -> {
                BlockState state = level.getBlockState(pos);
                return state.getLightEmission() >= 8;
            })
            .min(Comparator.<BlockPos>comparingInt(pos -> -level.getBrightness(LightLayer.BLOCK, pos))
                .thenComparingDouble(pos -> pos.distSqr(mothPos)))
            .orElse(null);
    }

    private Vec3 getDynamicLightTarget(BlockPos lightPos) {
        int xOffset = this.random.nextInt(3) - 1;
        int yOffset = this.random.nextInt(3) - 1;
        int zOffset = this.random.nextInt(3) - 1;

        if (this.random.nextBoolean()) {
            yOffset += (this.random.nextBoolean() ? 2 : -2);
        }

        BlockPos target = lightPos.offset(xOffset, yOffset, zOffset);
        return Vec3.atCenterOf(target);
    }

    private void moveTowardsTarget(Vec3 targetPos) {
        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();

        double dx = targetPos.x - currentPos.x;
        double dy = targetPos.y - currentPos.y;
        double dz = targetPos.z - currentPos.z;

        Vec3 adjustment = new Vec3(
            (Math.signum(dx) * 0.5 - motion.x) * 0.07,
            (Math.signum(dy) * 0.7 - motion.y) * (dy > 0 ? 0.12 : 0.08),
            (Math.signum(dz) * 0.5 - motion.z) * 0.07
        );

        Vec3 newMotion = motion.add(adjustment);
        this.setDeltaMovement(newMotion);

        float targetYaw = (float)(Mth.atan2(newMotion.z, newMotion.x) * (180F / Math.PI)) - 90.0F;
        float yawDiff = Mth.wrapDegrees(targetYaw - this.getYRot());
        this.zza = 0.5F;
        this.setYRot(this.getYRot() + yawDiff * 0.2F);
    }

    private void randomMovement() {
        if (this.targetPosition == null || this.random.nextInt(40) == 0 ||
            this.targetPosition.closerToCenterThan(this.position(), 1.5D)) {

            this.targetPosition = BlockPos.containing(
                this.getX() + this.random.nextInt(9) - 4,
                this.getY() + this.random.nextInt(5) - 2,
                this.getZ() + this.random.nextInt(9) - 4
            );
        }

        Vec3 target = Vec3.atCenterOf(this.targetPosition);
        moveTowardsTarget(target);
    }

    @Override
    protected void checkFallDamage(double fallDistance, boolean onGround, BlockState state, BlockPos pos) {
    }
    
    public boolean isPushable() {
        return false;
     }
    
    public static boolean canSpawn(EntityType<SilkMothEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
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

    private PlayState predicate(AnimationState<SilkMothEntity> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().then("fly", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return FaunifySounds.SILKMOTH_HURT.get();
    }
    
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return FaunifySounds.SILKMOTH_HURT.get();
    }

	@Override
	public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob otherParent) {
		return null;
	}
 }
