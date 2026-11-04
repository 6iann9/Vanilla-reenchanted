package net.iann.vanillareenchanted.client;

import com.mojang.logging.LogUtils;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.fx.IEffectExecutor;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.iann.vanillareenchanted.network.UpdraftEffectPayload;
import net.iann.vanillareenchanted.client.particle.PhotonEffects;
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
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath("vr_effects", "updraft_tornado");
    private static final ResourceLocation JUMP_EFFECT = ResourceLocation.fromNamespaceAndPath("vr_effects", "updraft_jump");
    private static final ResourceLocation ASCENT_EFFECT = ResourceLocation.fromNamespaceAndPath("vr_effects", "updraft_particles");
    private static final Map<Integer, Ascent> ASCENTS = new HashMap<>();
    private static final Map<Integer, Active> ACTIVE = new HashMap<>();
    private static final List<FXRuntime> JUMPS = new ArrayList<>();
    private static final List<FXRuntime> FINISHING_CHARGES = new ArrayList<>();
    private static ClientLevel world;

    private static class Active implements IEffectExecutor {
        final ResourceLocation effect;
        Active() { this(EFFECT); }
        Active(ResourceLocation effect) { this.effect = effect; }
        FXRuntime runtime;
        Player owner;
        ClientLevel level;
        boolean started, finishing;
        long refreshed;
        void stop() { finishing = true; PhotonEffects.stop(runtime); }
        void finish() {
            finishing = true;
            if (runtime != null) {
                PhotonEffects.finish(runtime);
                FINISHING_CHARGES.add(runtime);
            }
        }
        @Override public ClientLevel getLevel() { return level; }
        @Override public void updateFXObjectTick(IFXObject object) { position(1.0F); }
        @Override public void updateFXObjectFrame(IFXObject object, float partialTick) { position(partialTick); }
        void position(float partialTick) {
            if (finishing || runtime == null || owner == null || !owner.isAlive()) return;
            var pos = owner.getPosition(partialTick);
            runtime.root.updatePos(new Vector3f((float)pos.x, (float)pos.y, (float)pos.z));
        }
        void start(Player player) {
            started = true;
            owner = player;
            level = world;
            try {
                var fx = FXHelper.getFX(effect);
                if (fx == null) {
                    LogUtils.getLogger().warn("Missing Photon Updraft effect: {}", effect);
                    return;
                }
                runtime = fx.createRuntime();
                position(1.0F);
                runtime.emit(this);
            } catch (RuntimeException error) {
                stop();
                LogUtils.getLogger().warn("Could not start Photon Updraft effect", error);
            }
        }
    }

    private static final class Ascent extends Active {
        final long requestedAt = world.getGameTime();
        double lastY;
        boolean rose;
        int stalledTicks;
        Ascent() { super(ASCENT_EFFECT); }

        boolean update(Player player) {
            if (!started) {
                lastY = player.getY();
                start(player);
                return runtime != null && !finishing;
            }
            position(1.0F);
            double rise = player.getY() - lastY;
            lastY = player.getY();
            if (rise > 0.00001) {
                rose = true;
                stalledTicks = 0;
            } else {
                stalledTicks++;
            }
            // Allow launch motion to arrive after the effect packet, and tolerate
            // one flat remote interpolation tick without ending the trail early.
            if ((rose && (rise < -0.00001 || stalledTicks >= 2))
                    || (!rose && world.getGameTime() - requestedAt >= 10)) {
                finish();
                return false;
            }
            return true;
        }
    }

    private static void updateWorld(ClientLevel level) {
        if (world == level) return;
        ACTIVE.values().forEach(Active::stop);
        ACTIVE.clear();
        ASCENTS.values().forEach(Active::stop);
        ASCENTS.clear();
        JUMPS.forEach(PhotonEffects::stop);
        JUMPS.clear();
        FINISHING_CHARGES.forEach(PhotonEffects::stop);
        FINISHING_CHARGES.clear();
        world = level;
    }

    public static void receive(UpdraftEffectPayload payload) {
        updateWorld(Minecraft.getInstance().level);
        if (world == null) return;
        if (payload.action() == UpdraftEffectPayload.JUMP) {
            playJump(payload);
            Ascent previous = ASCENTS.put(payload.playerId(), new Ascent());
            if (previous != null) previous.finish();
        } else if (payload.action() == UpdraftEffectPayload.FINISH) {
            Active old = ACTIVE.remove(payload.playerId());
            if (old != null) old.finish();
        } else if (payload.action() == UpdraftEffectPayload.STOP) {
            Active old = ACTIVE.remove(payload.playerId());
            if (old != null) old.stop();
        } else if (payload.action() == UpdraftEffectPayload.START) {
            ACTIVE.computeIfAbsent(payload.playerId(), id -> new Active()).refreshed = world.getGameTime();
        }
    }

    private static void playJump(UpdraftEffectPayload payload) {
        FXRuntime runtime = null;
        try {
            var fx = FXHelper.getFX(JUMP_EFFECT);
            if (fx == null) {
                LogUtils.getLogger().warn("Missing Photon Updraft jump effect: {}", JUMP_EFFECT);
                return;
            }
            runtime = fx.createRuntime();
            // Keep the burst at takeoff rather than following the ascending player.
            runtime.root.updatePos(new Vector3f((float)payload.x(), (float)payload.y(), (float)payload.z()));
            ClientLevel launchWorld = world;
            runtime.emit(() -> launchWorld);
            JUMPS.add(runtime);
        } catch (RuntimeException error) {
            PhotonEffects.stop(runtime);
            LogUtils.getLogger().warn("Could not start Photon Updraft jump effect", error);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        updateWorld(mc.level);
        if (world == null || mc.isPaused()) return;
        var ascents = ASCENTS.entrySet().iterator();
        while (ascents.hasNext()) {
            var entry = ascents.next();
            Ascent ascent = entry.getValue();
            var entity = world.getEntity(entry.getKey());
            if (!(entity instanceof Player player)) {
                if (ascent.started || world.getGameTime() - ascent.requestedAt >= 10) {
                    ascent.finish();
                    ascents.remove();
                }
            } else if (!player.isAlive()) {
                ascent.finish();
                ascents.remove();
            } else if (!ascent.update(player)) {
                ascents.remove();
            }
        }
        var jumps = JUMPS.iterator();
        while (jumps.hasNext()) {
            FXRuntime runtime = jumps.next();
            if (runtime.isFinished() || runtime.getTimelinePlayer().isFinished()) {
                PhotonEffects.finish(runtime);
                FINISHING_CHARGES.add(runtime);
                jumps.remove();
            }
        }
        FINISHING_CHARGES.removeIf(FXRuntime::isFinished);
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
            if (!active.started) active.start(player);
            else active.position(1.0F);
        }
    }

    private UpdraftEffects() {}
}
