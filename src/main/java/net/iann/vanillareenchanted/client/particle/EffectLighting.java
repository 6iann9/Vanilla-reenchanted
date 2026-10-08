package net.iann.vanillareenchanted.client.particle;

import java.util.HashMap;
import java.util.Map;
import mod.chloeprime.aaaparticles.api.client.effekseer.ParticleEmitter;
import net.iann.vanillareenchanted.mixin.EffectLightTextureAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Supplies vanilla lightmap RGB through AAA dynamic inputs 0, 1, and 2.
 * Requires a matching material and Custom Data 1 set to Dynamic Input.
 * Call track once for a stationary effect, or again when its sampling position moves.
 * Client thread only. This helper owns lighting, not the emitter's lifetime.
 */
@EventBusSubscriber(modid="iannvanillareenchanted", value=Dist.CLIENT)
public final class EffectLighting {
    private static final Map<ParticleEmitter, BlockPos> TRACKED = new HashMap<>();
    private static ClientLevel world;

    public static void track(ParticleEmitter emitter, ClientLevel level, BlockPos position) {
        updateWorld();
        if (level != world || level == null || !emitter.exists()) return;
        BlockPos previous = TRACKED.put(emitter, position.immutable());
        if (!position.equals(previous)) update(emitter, position);
    }

    /** Stop updating lighting without stopping the effect or changing its last RGB values. */
    public static void untrack(ParticleEmitter emitter) {
        TRACKED.remove(emitter);
    }

    private static void updateWorld() {
        ClientLevel current = Minecraft.getInstance().level;
        if (world != current) {
            TRACKED.clear();
            world = current;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        updateWorld();
        if (world == null || Minecraft.getInstance().isPaused()) return;
        var iterator = TRACKED.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!entry.getKey().exists()) iterator.remove();
            else update(entry.getKey(), entry.getValue());
        }
    }

    private static void update(ParticleEmitter emitter, BlockPos position) {
        int block = world.getBrightness(LightLayer.BLOCK, position);
        int sky = world.getBrightness(LightLayer.SKY, position);
        // CPU lightmap includes daylight, weather, dimension, gamma, and night vision.
        // No GPU readback, and no changes to alpha or unrelated effects.
        var lightmap = ((EffectLightTextureAccess)Minecraft.getInstance().gameRenderer.lightTexture()).vr$lightPixels();
        int color = lightmap.getPixelRGBA(block, sky); // NativeImage ABGR
        emitter.setDynamicInput(0, (color & 255) / 255.0F);
        emitter.setDynamicInput(1, ((color >>> 8) & 255) / 255.0F);
        emitter.setDynamicInput(2, ((color >>> 16) & 255) / 255.0F);
    }

    private EffectLighting() {}
}
