package com.pepper.faunify.items.eggs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.SpawnEggItem;

public class FennecSpawnEgg extends SpawnEggItem {
    @SuppressWarnings("deprecation")
	public FennecSpawnEgg(EntityType<? extends Animal> type, int primaryColor, int secondaryColor, Properties properties) {
        super(type, primaryColor, secondaryColor, properties);
    }
}
