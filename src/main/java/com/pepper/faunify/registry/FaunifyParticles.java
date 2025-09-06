package com.pepper.faunify.registry;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.particle.FaunifyParticleTypes;
import com.pepper.faunify.particle.HealthParticle;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Faunify.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FaunifyParticles {
	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(FaunifyParticleTypes.HEALTH.get(), HealthParticle::provider);
	}
}
