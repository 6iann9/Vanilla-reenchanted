package net.iann.vanillareenchanted.client;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mod.chloeprime.aaaparticles.api.client.EffectRegistry;
import mod.chloeprime.aaaparticles.api.client.effekseer.ParticleEmitter;
import mod.chloeprime.aaaparticles.client.installer.NativePlatform;
import net.iann.vanillareenchanted.network.UpdraftEffectPayload;
import net.iann.vanillareenchanted.client.particle.EffectLighting;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client-only ownership of the continuous charging effects. Gameplay stays on the server. */
@EventBusSubscriber(modid="iannvanillareenchanted", value=Dist.CLIENT)
public final class UpdraftEffects {
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "updraft/updraft_tornado");
    private static final ResourceLocation JUMP_EFFECT = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "updraft/updraft_jump");
    private static final float SCALE = 1.0F;
    private static final Map<Integer, Active> ACTIVE = new HashMap<>();
    private static final List<ParticleEmitter> FINISHING = new ArrayList<>();
    private static ClientLevel world;

    private static final class Active {
        ParticleEmitter emitter;
        boolean loading, failed;
        long refreshed;
        void stop() { if (emitter != null) emitter.stop(); }
    }

    private static void updateWorld(ClientLevel level) {
        if (world == level) return;
        ACTIVE.values().forEach(Active::stop);
        ACTIVE.clear();
        FINISHING.forEach(ParticleEmitter::stop);
        FINISHING.clear();
        world = level;
    }

    public static void receive(UpdraftEffectPayload payload) {
        updateWorld(Minecraft.getInstance().level);
        if (world == null || NativePlatform.isRunningOnUnsupportedPlatform()) return;
        if (payload.action() == UpdraftEffectPayload.JUMP) {
            playJump(payload);
        } else if (payload.action() == UpdraftEffectPayload.FINISH) {
            Active old = ACTIVE.remove(payload.playerId());
            if (old != null && old.emitter != null && old.emitter.exists()) {
                // Trigger 0 stops generation on every tornado node without destroying live particles.
                old.emitter.sendTrigger(0);
                FINISHING.add(old.emitter);
            }
        } else if (payload.action() == UpdraftEffectPayload.STOP) {
            Active old = ACTIVE.remove(payload.playerId());
            if (old != null) old.stop();
        } else if (payload.action() == UpdraftEffectPayload.START) {
            ACTIVE.computeIfAbsent(payload.playerId(), id -> new Active()).refreshed = world.getGameTime();
        }
    }

    private static void playJump(UpdraftEffectPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel requestedWorld = world;
        long requestedAt = world.getGameTime();
        EffectRegistry.load(JUMP_EFFECT).whenComplete((effect, error) -> mc.execute(() -> {
            if (mc.level != requestedWorld || requestedWorld.getGameTime() - requestedAt > 20) return;
            if (error != null) {
                LogUtils.getLogger().warn("Could not load Updraft jump effect", error);
                return;
            }
            ParticleEmitter emitter = effect.play();
            emitter.setPosition((float)payload.x(), (float)payload.y(), (float)payload.z());
            emitter.setScale(SCALE, SCALE, SCALE);
            EffectLighting.track(emitter, requestedWorld, BlockPos.containing(payload.x(), payload.y() + 0.8, payload.z()));
            FINISHING.add(emitter);
        }));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        updateWorld(mc.level);
        if (world == null || mc.isPaused()) return;
        FINISHING.removeIf(emitter -> !emitter.exists());
        var iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Active active = entry.getValue();
            var entity = world.getEntity(entry.getKey());
            // A short lease also cleans up lost tracking, disconnects, and interrupted server state.
            if (world.getGameTime() - active.refreshed > 30 || (entity != null && !entity.isAlive())) {
                active.stop();
                iterator.remove();
                continue;
            }
            if (!(entity instanceof Player player)) continue;
            if (active.emitter != null && active.emitter.exists()) {
                position(active.emitter, player);
            } else if (!active.loading && !active.failed) {
                active.loading = true;
                ClientLevel requestedWorld = world;
                int id = entry.getKey();
                EffectRegistry.load(EFFECT).whenComplete((effect, error) -> mc.execute(() -> {
                    // Loading can finish after release, dimension change, or a new charge.
                    if (mc.level != requestedWorld || ACTIVE.get(id) != active) return;
                    active.loading = false;
                    if (error != null) {
                        active.failed = true;
                        LogUtils.getLogger().warn("Could not load Updraft tornado", error);
                        return;
                    }
                    if (!(requestedWorld.getEntity(id) instanceof Player owner) || !owner.isAlive()) return;
                    active.emitter = effect.play();
                    active.emitter.setScale(SCALE, SCALE, SCALE);
                    position(active.emitter, owner);
                }));
            }
        }
    }

    private static void position(ParticleEmitter emitter, Player player) {
        emitter.setPosition((float)player.getX(), (float)player.getY(), (float)player.getZ());
        EffectLighting.track(emitter, world, BlockPos.containing(player.getX(), player.getY() + 0.8, player.getZ()));
    }

    private UpdraftEffects() {}
}
