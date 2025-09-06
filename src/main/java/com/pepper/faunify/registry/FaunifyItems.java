package com.pepper.faunify.registry;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.items.BlowpipeItem;
import com.pepper.faunify.items.DustPowderItem;
import com.pepper.faunify.items.LeafSheepBucketItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FaunifyItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Faunify.MODID);

    public static final RegistryObject<Item> WEASEL_SPAWN_EGG = ITEMS.register("weasel_spawn_egg",
            () -> new ForgeSpawnEggItem(FaunifyEntities.WEASEL, 0xac7156, 0xfaede1, new Item.Properties()));
    public static final RegistryObject<Item> FENNEC_SPAWN_EGG = ITEMS.register("fennec_spawn_egg",
            () -> new ForgeSpawnEggItem(FaunifyEntities.FENNEC, 0xc7b7bc, 0xb18b70, new Item.Properties()));
    public static final RegistryObject<Item> CHINCHILLA_SPAWN_EGG = ITEMS.register("chinchilla_spawn_egg",
            () -> new ForgeSpawnEggItem(FaunifyEntities.CHINCHILLA, 0xc9b9c0, 0x9b8b91, new Item.Properties()));
    public static final RegistryObject<Item> HEDGEHOG_SPAWN_EGG = ITEMS.register("hedgehog_spawn_egg",
            () -> new ForgeSpawnEggItem(FaunifyEntities.HEDGEHOG, 0xe9dec7, 0xae5b35, new Item.Properties()));
    public static final RegistryObject<Item> RINGTAIL_SPAWN_EGG = ITEMS.register("ringtailcat_spawn_egg",
            () -> new ForgeSpawnEggItem(FaunifyEntities.RINGTAIL, 0xa78971, 0x423637, new Item.Properties()));
    public static final RegistryObject<Item> OPOSSUM_SPAWN_EGG = ITEMS.register("opossum_spawn_egg",
            () -> new ForgeSpawnEggItem(FaunifyEntities.OPOSSUM, 0xf3efee, 0xe9a0a9, new Item.Properties()));
    public static final RegistryObject<Item> MOUSE_SPAWN_EGG = ITEMS.register("mouse_spawn_egg",
            () -> new ForgeSpawnEggItem(FaunifyEntities.MOUSE, 0x935233, 0xb3815d, new Item.Properties()));
    public static final RegistryObject<Item> SILKMOTH_SPAWN_EGG = ITEMS.register("silkmoth_spawn_egg",
            () -> new ForgeSpawnEggItem(FaunifyEntities.SILKMOTH, 0xf9f8f4, 0xd4c7b7, new Item.Properties()));
    public static final RegistryObject<Item> LEAFSHEEP_SPAWN_EGG = ITEMS.register("leafsheep_spawn_egg",
            () -> new ForgeSpawnEggItem(FaunifyEntities.LEAFSHEEP, 0x07a906, 0xb22c5e, new Item.Properties()));

    // Leaf Sheep Bucket
    public static final RegistryObject<Item> LEAF_SHEEP_BUCKET = ITEMS.register("leaf_sheep_bucket",
            () -> new LeafSheepBucketItem(() -> FaunifyEntities.LEAFSHEEP.get(), () -> Fluids.WATER, 
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DUST_POWDER = ITEMS.register("dust_powder",
            () -> new DustPowderItem(new Item.Properties()));
    public static final RegistryObject<Item> QUILL = ITEMS.register("quill",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FUR = ITEMS.register("fur",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BLOWPIPE = ITEMS.register("blowpipe",
            () -> new BlowpipeItem(new Item.Properties().durability(350)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static final TagKey<Item> EGGS = TagKey.create(Registries.ITEM, new ResourceLocation("faunify", "eggs"));
}