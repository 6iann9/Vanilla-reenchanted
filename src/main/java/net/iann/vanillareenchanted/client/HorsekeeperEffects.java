package net.iann.vanillareenchanted.client;

import java.util.ArrayList;
import java.util.List;
import mod.chloeprime.aaaparticles.api.client.EffectRegistry;
import mod.chloeprime.aaaparticles.api.client.effekseer.ParticleEmitter;
import mod.chloeprime.aaaparticles.client.installer.NativePlatform;
import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.network.HorsekeeperEffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "iannvanillareenchanted", value = Dist.CLIENT)
public final class HorsekeeperEffects {
    private record Playing(ParticleEmitter emitter, long expires) {}
    private static final List<Playing> PLAYING = new ArrayList<>();
    private static ClientLevel world;

    private static void updateWorld(ClientLevel level) {
        if (world == level) return;
        PLAYING.forEach(effect -> effect.emitter().stop());
        PLAYING.clear();
        world = level;
    }

    public static void receive(HorsekeeperEffectPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        updateWorld(mc.level);
        if (world == null || NativePlatform.isRunningOnUnsupportedPlatform()) return;
        String name = switch (payload.action()) {
            case HorsekeeperEffectPayload.DESPAWN -> "horse_despawn";
            case HorsekeeperEffectPayload.PRESPAWN -> "horse_prespawn";
            case HorsekeeperEffectPayload.SPAWN -> "horse_spawn";
            default -> null;
        };
        if (name == null) return;
        ClientLevel requestedWorld = world;
        long requestedAt = world.getGameTime();
        EffectRegistry.load(ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "horsekeeper/" + name))
                .whenComplete((effect, error) -> mc.execute(() -> {
                    if (mc.level != requestedWorld || requestedWorld.getGameTime() - requestedAt > 20) return;
                    if (error != null) {
                        VanillaReenchanted.LOGGER.warn("Could not load Horsekeeper effect {}", name, error);
                        return;
                    }
                    ParticleEmitter emitter = effect.play();
                    emitter.setPosition((float) payload.x(), (float) payload.y(), (float) payload.z());
                    emitter.setScale(1, 1, 1);
                    // Retire invisible parent nodes as well as visible particles.
                    PLAYING.add(new Playing(emitter, requestedWorld.getGameTime() + 60));
                }));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        updateWorld(mc.level);
        if (world == null || mc.isPaused()) return;
        PLAYING.removeIf(effect -> {
            if (!effect.emitter().exists()) return true;
            if (world.getGameTime() < effect.expires()) return false;
            effect.emitter().stop();
            return true;
        });
    }

    private HorsekeeperEffects() {}
}
