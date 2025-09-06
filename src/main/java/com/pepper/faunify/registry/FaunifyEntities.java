package com.pepper.faunify.registry;


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
import com.pepper.faunify.entity.projectile.DustPowderEntity;
import com.pepper.faunify.entity.projectile.QuillEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FaunifyEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Faunify.MODID);

    public static final RegistryObject<EntityType<WeaselEntity>> WEASEL = ENTITY_TYPES.register("weasel", 
        () -> EntityType.Builder.of(WeaselEntity::new, MobCategory.CREATURE)
            .sized(0.8F, 0.6F)
            .build("weasel")
    );
    
    public static final RegistryObject<EntityType<FennecEntity>> FENNEC = ENTITY_TYPES.register("fennec", 
            () -> EntityType.Builder.of(FennecEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 0.7F)
                .build("fennec")
        );
    
    public static final RegistryObject<EntityType<ChinchillaEntity>> CHINCHILLA = ENTITY_TYPES.register("chinchilla", 
            () -> EntityType.Builder.of(ChinchillaEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 0.7F)
                .build("chinchilla")
        );
    
    public static final RegistryObject<EntityType<HedgehogEntity>> HEDGEHOG = ENTITY_TYPES.register("hedgehog", 
            () -> EntityType.Builder.of(HedgehogEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 0.7F)
                .build("hedgehog")
        );
    
    public static final RegistryObject<EntityType<RingtailCatEntity>> RINGTAIL = ENTITY_TYPES.register("ringtailcat", 
            () -> EntityType.Builder.of(RingtailCatEntity::new, MobCategory.CREATURE)
                .sized(0.9F, 0.7F)
                .build("ringtailcat")
        );
    
    public static final RegistryObject<EntityType<OpossumEntity>> OPOSSUM = ENTITY_TYPES.register("opossum", 
            () -> EntityType.Builder.of(OpossumEntity::new, MobCategory.CREATURE)
            	.sized(0.6F, 0.7F)
                .build("opossum")
        );
    
    public static final RegistryObject<EntityType<MouseEntity>> MOUSE = ENTITY_TYPES.register("mouse", 
            () -> EntityType.Builder.of(MouseEntity::new, MobCategory.CREATURE)
                .sized(0.3F, 0.3F)
                .build("mouse")
        );
    
    public static final RegistryObject<EntityType<SilkMothEntity>> SILKMOTH = ENTITY_TYPES.register("silkmoth", 
            () -> EntityType.Builder.of(SilkMothEntity::new, MobCategory.CREATURE)
                .sized(0.3F, 0.3F)
                .build("silkmoth")
        );
    
    public static final RegistryObject<EntityType<LeafSheepEntity>> LEAFSHEEP = ENTITY_TYPES.register("leafsheep", 
            () -> EntityType.Builder.of(LeafSheepEntity::new, MobCategory.CREATURE)
                .sized(0.3F, 0.3F)
                .build("leafsheep")
        );

    public static final RegistryObject<EntityType<DustPowderEntity>> DUST_POWDER_PROJECTILE =
            ENTITY_TYPES.register("dust_powder", () -> EntityType.Builder.<DustPowderEntity>of(DustPowderEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).build("dust_powder"));

    public static final RegistryObject<EntityType<QuillEntity>> QUILL_PROJECTILE =
            ENTITY_TYPES.register("quill", () -> EntityType.Builder.<QuillEntity>of(QuillEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).build("quill"));
    
    
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
