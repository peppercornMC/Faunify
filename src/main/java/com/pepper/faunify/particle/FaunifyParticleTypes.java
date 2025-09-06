package com.pepper.faunify.particle;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.core.particles.SimpleParticleType;

import com.pepper.faunify.Faunify;

import net.minecraft.core.particles.ParticleType;

public class FaunifyParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Faunify.MODID);
	public static final RegistryObject<SimpleParticleType> HEALTH = REGISTRY.register("health", () -> new SimpleParticleType(false));
}
