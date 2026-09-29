package net.iann.vanillareenchanted.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.joml.Quaternionf;

/** Plays the authored impact frames on one stationary, camera-facing quad. */
public final class EchoImpactRingParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 4;
    private static final float HALF_SIZE = 0.4125F;
    private static final double CAMERA_CLEARANCE = 0.45;
    private final SpriteSet sprites;

    private EchoImpactRingParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = FRAME_COUNT;
        this.hasPhysics = false;
        this.quadSize = HALF_SIZE;
        this.rCol = this.gCol = this.bCol = 1;
        this.alpha = 1;
        this.setSprite(sprites.get(0, FRAME_COUNT - 1));
    }

    @Override public void tick() {
        if (++age >= lifetime) {
            remove();
            return;
        }
        // Using count - 1 ensures the last authored frame is actually displayed.
        setSprite(sprites.get(age, FRAME_COUNT - 1));
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override protected int getLightColor(float partialTick) { return 15728880; }

    @Override public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        var eye = camera.getPosition();
        var towardCamera = eye.subtract(x, y, z);
        var offset = towardCamera.normalize().scale(Math.min(CAMERA_CLEARANCE, towardCamera.length() * 0.25));
        // Preserve impact placement and clearance, using vanilla's single-quad renderer.
        renderRotatedQuad(buffer, new Quaternionf(camera.rotation()),
                (float)(x + offset.x - eye.x), (float)(y + offset.y - eye.y),
                (float)(z + offset.z - eye.z), partialTick);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd) {
            return new EchoImpactRingParticle(level, x, y, z, sprites);
        }
    }
}
