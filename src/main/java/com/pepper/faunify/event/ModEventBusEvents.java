package com.pepper.faunify.event;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.entity.ChinchillaEntity;
import com.pepper.faunify.entity.FennecEntity;
import com.pepper.faunify.entity.HedgehogEntity;
import com.pepper.faunify.entity.LeafSheepEntity;
import com.pepper.faunify.entity.MouseEntity;
import com.pepper.faunify.entity.OpossumEntity;
import com.pepper.faunify.entity.RingtailCatEntity;
import com.pepper.faunify.entity.SilkMothEntity;
import com.pepper.faunify.entity.WeaselEntity;
import com.pepper.faunify.registry.FaunifyEntities;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Faunify.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FaunifyEntities.WEASEL.get(), WeaselEntity.createAttributes().build());
        event.put(FaunifyEntities.FENNEC.get(), FennecEntity.createAttributes().build());
        event.put(FaunifyEntities.CHINCHILLA.get(), ChinchillaEntity.createAttributes().build());
        event.put(FaunifyEntities.HEDGEHOG.get(), HedgehogEntity.createAttributes().build());
        event.put(FaunifyEntities.RINGTAIL.get(), RingtailCatEntity.createAttributes().build());
        event.put(FaunifyEntities.OPOSSUM.get(), OpossumEntity.createAttributes().build());
        event.put(FaunifyEntities.MOUSE.get(), MouseEntity.createAttributes().build());
        event.put(FaunifyEntities.SILKMOTH.get(), SilkMothEntity.createAttributes().build());
        event.put(FaunifyEntities.LEAFSHEEP.get(), LeafSheepEntity.createAttributes().build());
    }
    
    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
    	event.register(
    			FaunifyEntities.WEASEL.get(), 
    			SpawnPlacements.Type.ON_GROUND, 
    			Heightmap.Types.WORLD_SURFACE, 
    			WeaselEntity::canSpawn, 
    			SpawnPlacementRegisterEvent.Operation.OR
    			);
    	
    	event.register(
    			FaunifyEntities.FENNEC.get(),
    			SpawnPlacements.Type.ON_GROUND, 
    			Heightmap.Types.WORLD_SURFACE, 
    			FennecEntity::canSpawn, 
    			SpawnPlacementRegisterEvent.Operation.OR
    			);
    	
    	event.register(
    			FaunifyEntities.CHINCHILLA.get(), 
    			SpawnPlacements.Type.ON_GROUND, 
    			Heightmap.Types.WORLD_SURFACE, 
    			ChinchillaEntity::canSpawn, 
    			SpawnPlacementRegisterEvent.Operation.OR
    			);
    	
    	event.register(
    			FaunifyEntities.HEDGEHOG.get(), 
    			SpawnPlacements.Type.ON_GROUND, 
    			Heightmap.Types.WORLD_SURFACE, 
    			HedgehogEntity::canSpawn, 
    			SpawnPlacementRegisterEvent.Operation.OR
    			);
    	
    	event.register(
    			FaunifyEntities.RINGTAIL.get(), 
    			SpawnPlacements.Type.ON_GROUND, 
    			Heightmap.Types.WORLD_SURFACE, 
    			RingtailCatEntity::canSpawn, 
    			SpawnPlacementRegisterEvent.Operation.OR
    			);
    	
    	event.register(
    			FaunifyEntities.OPOSSUM.get(), 
    			SpawnPlacements.Type.ON_GROUND, 
    			Heightmap.Types.WORLD_SURFACE, 
    			OpossumEntity::canSpawn, 
    			SpawnPlacementRegisterEvent.Operation.OR
    			);
    	
    	event.register(
    			FaunifyEntities.MOUSE.get(), 
    			SpawnPlacements.Type.ON_GROUND, 
    			Heightmap.Types.WORLD_SURFACE, 
    			MouseEntity::canSpawn, 
    			SpawnPlacementRegisterEvent.Operation.OR
    			);
    	
    	event.register(
    			FaunifyEntities.SILKMOTH.get(), 
    			SpawnPlacements.Type.NO_RESTRICTIONS, 
    			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 
    			SilkMothEntity::canSpawn, 
    			SpawnPlacementRegisterEvent.Operation.OR
    			);
    	
    	event.register(
    		    FaunifyEntities.LEAFSHEEP.get(),
    		    SpawnPlacements.Type.IN_WATER,
    		    Heightmap.Types.OCEAN_FLOOR,
    		    LeafSheepEntity::canSpawn,
    		    SpawnPlacementRegisterEvent.Operation.OR
    		);

    }
}