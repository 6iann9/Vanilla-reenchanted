package net.iann.vanillareenchanted.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class UpdraftParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final double centerX, centerY, centerZ, angle, radius, height;
    private final boolean burst;

    private UpdraftParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
            SpriteSet sprites, boolean burst) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.burst = burst;
        centerX = x; centerY = y; centerZ = z;
        angle = Math.atan2(dz, dx);
        radius = Math.hypot(dx, dz);
        height = dy;
        lifetime = burst ? 14 : 20;
        hasPhysics = false;
        gravity = 0;
        quadSize = burst ? 0.22F : 0.12F;
        setColor(1, 1, 1);
        setAlpha(burst ? 0.8F : 0);
        if (!burst) setPos(x + dx, y + dy, z + dz);
        xo = this.x; yo = this.y; zo = this.z;
        setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (++age >= lifetime) { remove(); return; }
        double t = (double)age / lifetime;
        double r = burst ? 0.3 + 3.0 * t : radius * (1 - t);
        double a = burst ? angle : angle + t * Math.PI * 1.5;
        setPos(centerX + Math.cos(a) * r, centerY + (burst ? t * 0.3 : height * (1 - t)), centerZ + Math.sin(a) * r);
        setAlpha((float)(burst ? (1 - t) * 0.8 : Math.min(t * 5, 1 - t) * 0.8));
        setSpriteFromAge(sprites);
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean burst;
        public Provider(SpriteSet sprites, boolean burst) { this.sprites = sprites; this.burst = burst; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double dx, double dy, double dz) {
            return new UpdraftParticle(level, x, y, z, dx, dy, dz, sprites, burst);
        }
    }
}
