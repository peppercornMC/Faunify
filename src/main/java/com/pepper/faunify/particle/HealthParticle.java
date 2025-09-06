package com.pepper.faunify.particle;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.multiplayer.ClientLevel;

@OnlyIn(Dist.CLIENT)
public class HealthParticle extends TextureSheetParticle {
	public static HealthParticleProvider provider(SpriteSet spriteSet) {
		return new HealthParticleProvider(spriteSet);
	}

	public static class HealthParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public HealthParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new HealthParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	@SuppressWarnings("unused")
	private final SpriteSet spriteSet;

	protected HealthParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.setSize(0.5f, 0.5f);
		this.pickSprite(spriteSet);
		this.speedUpWhenYMotionIsBlocked = true;
	    this.friction = 0.86F;
	    this.xd *= (double)0.01F;
	    this.yd *= (double)0.01F;
	    this.zd *= (double)0.01F;
	    this.yd += 0.08D;
	    this.quadSize *= 1.5F;
	    this.lifetime = 16;
	    this.hasPhysics = false;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
	}
}