package net.iann.vanillareenchanted.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.joml.Quaternionf;

/** Vanilla sweep frames on a horizontal plane aligned with the attacking player. */
public final class SweepSlashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final Quaternionf facing;
    private final Quaternionf reverseFacing;

    private SweepSlashParticle(ClientLevel level, double x, double y, double z,
                               double forwardX, double forwardZ, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = 4;
        this.hasPhysics = false;
        this.quadSize = 1;
        this.rCol = this.gCol = this.bCol = random.nextFloat() * 0.6F + 0.4F;
        float yaw = (float)Math.atan2(-forwardX, forwardZ);
        this.facing = new Quaternionf().rotationY(-yaw).rotateX((float)Math.PI / 2);
        this.reverseFacing = new Quaternionf(facing).rotateY((float)Math.PI);
        setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) remove();
        else setSpriteFromAge(sprites);
    }
    @Override public int getLightColor(float partialTick) { return 15728880; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_LIT; }
    @Override public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        // Choose the visible side without rotating the plane toward the camera.
        // Emit only one quad, avoiding overlapping faces with shader/resource packs.
        renderRotatedQuad(buffer, camera, camera.getPosition().y >= y ? reverseFacing : facing, partialTick);
    }
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double forwardX, double unused, double forwardZ) {
            return new SweepSlashParticle(level, x, y, z, forwardX, forwardZ, sprites);
        }
    }
}
