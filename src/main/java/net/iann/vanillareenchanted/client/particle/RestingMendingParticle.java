package net.iann.vanillareenchanted.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class RestingMendingParticle extends TextureSheetParticle {
    protected RestingMendingParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.lifetime = 35 + this.random.nextInt(20);
        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.xd = (this.random.nextDouble() - 0.5D) * 0.004D;
        this.yd = (this.random.nextDouble() - 0.5D) * 0.002D;
        this.zd = (this.random.nextDouble() - 0.5D) * 0.004D;

        this.quadSize = 0.04F + this.random.nextFloat() * 0.02F;

        this.setColor(1.0F, 1.0F, 1.0F);
        this.setAlpha(0.0F);
        // Each particle keeps one randomly chosen glyph for its lifetime.
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;

        this.move(this.xd, this.yd, this.zd);

        float lifeProgress = (float) this.age / (float) this.lifetime;

        if (lifeProgress < 0.2F) {
            this.setAlpha(lifeProgress / 0.2F * 0.85F);
        } else {
            this.setAlpha((1.0F - lifeProgress) / 0.8F * 0.85F);
        }
    }

    @Override
    protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf rotation,
                                     float x, float y, float z, float partialTicks) {
        // Keep the 5x7 glyph proportional instead of stretching it onto a square.
        float height = this.getQuadSize(partialTicks);
        float width = height * 5.0F / 7.0F;
        int light = this.getLightColor(partialTicks);
        renderGlyphVertex(buffer, rotation, x, y, z, width, -height, getU1(), getV1(), light);
        renderGlyphVertex(buffer, rotation, x, y, z, width, height, getU1(), getV0(), light);
        renderGlyphVertex(buffer, rotation, x, y, z, -width, height, getU0(), getV0(), light);
        renderGlyphVertex(buffer, rotation, x, y, z, -width, -height, getU0(), getV1(), light);
    }

    private void renderGlyphVertex(VertexConsumer buffer, Quaternionf rotation,
                                   float x, float y, float z, float dx, float dy,
                                   float u, float v, int light) {
        Vector3f vertex = new Vector3f(dx, dy, 0.0F).rotate(rotation).add(x, y, z);
        buffer.addVertex(vertex.x(), vertex.y(), vertex.z())
                .setUv(u, v)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new RestingMendingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
