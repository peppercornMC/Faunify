package com.pepper.faunify.items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;


import java.util.function.Supplier;

public class LeafSheepBucketItem extends MobBucketItem {

    public LeafSheepBucketItem(Supplier<? extends EntityType<?>> entitySupplier, Supplier<? extends Fluid> fluidSupplier, Properties properties) {
        super(entitySupplier, fluidSupplier, () -> SoundEvents.BUCKET_EMPTY_FISH, properties);
    }

    @Override
    public void checkExtraContent(@Nullable Player player, Level level, ItemStack stack, BlockPos pos) {
        if (level instanceof ServerLevel) {
            this.spawn((ServerLevel) level, stack, pos);
        }
    }

    protected void spawn(ServerLevel level, ItemStack stack, BlockPos pos) {
        Entity entity = this.getFishType().spawn(level, stack, (Player)null, pos, MobSpawnType.BUCKET, true, false);
        if (entity instanceof Bucketable bucketable) {
            bucketable.loadFromBucketTag(stack.getOrCreateTag());
            bucketable.setFromBucket(true);
        }
    }
}