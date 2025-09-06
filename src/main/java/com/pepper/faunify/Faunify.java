package com.pepper.faunify;

import com.mojang.logging.LogUtils;
import com.pepper.faunify.items.tabs.FaunifyCreativeTabs;
import com.pepper.faunify.particle.FaunifyParticleTypes;
import com.pepper.faunify.registry.FaunifyBlocks;
import com.pepper.faunify.registry.FaunifyEntities;
import com.pepper.faunify.registry.FaunifyItems;
import com.pepper.faunify.registry.FaunifySounds;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;

@Mod(Faunify.MODID)
public class Faunify {
    public static final String MODID = "faunify";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Faunify.MODID);
    public static final RegistryObject<SimpleParticleType> HEALTH = PARTICLES.register("health", () -> new SimpleParticleType(false));

    public Faunify() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        FaunifyCreativeTabs.register(modEventBus);

        FaunifyItems.register(modEventBus);
        FaunifyBlocks.register(modEventBus);
        FaunifySounds.register(modEventBus);
        FaunifyEntities.register(modEventBus);
        FaunifyParticleTypes.REGISTRY.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    	
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    	
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
}