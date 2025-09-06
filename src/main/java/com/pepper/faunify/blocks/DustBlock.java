package com.pepper.faunify.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

import javax.annotation.Nullable;

import com.pepper.faunify.registry.FaunifyItems;

public class DustBlock extends Block {
	
    public DustBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        super.playerDestroy(level, player, pos, state, blockEntity, stack);
        
        RandomSource random = level.getRandom();
        int dropCount = 2 + random.nextInt(3);
        
        for (int i = 0; i < dropCount; i++) {
            ItemStack dustItem = new ItemStack(FaunifyItems.DUST_POWDER.get());
            level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), dustItem));
        }
    }
}
