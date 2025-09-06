package com.pepper.faunify.items.eggs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.SpawnEggItem;

public class RingtailCatSpawnEgg extends SpawnEggItem {
    @SuppressWarnings("deprecation")
	public RingtailCatSpawnEgg(EntityType<? extends Animal> type, int primaryColor, int secondaryColor, Properties properties) {
        super(type, primaryColor, secondaryColor, properties);
    }
}
