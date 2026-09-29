package net.iann.vanillareenchanted.event;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.iann.vanillareenchanted.enchantment.MomentumState;
import net.iann.vanillareenchanted.registry.ModSounds;
import net.iann.vanillareenchanted.network.SyncMomentumPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.iann.vanillareenchanted.mixin.MomentumCollisionAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class MomentumEvents {
    public static final ResourceLocation MOMENTUM = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "momentum");
    private static final ResourceLocation SPEED = ResourceLocation.withDefaultNamespace("effect.speed");
    public static final double MAX_DOWNHILL_DROP = 1.25;
    public static final int MAX_DOWNHILL_TICKS = 8;
    public static final int MOVEMENT_GRACE_TICKS = 10;
    public static final int COLLISION_RECOVERY_TICKS = 10;
    private static final Map<AbstractHorse, RideState> STATES = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final class RideState {
        final MomentumState charge = new MomentumState();
        UUID rider;
        Vec3 lastPosition;
        int idleTicks = MOVEMENT_GRACE_TICKS;
        double groundSpeed;
        boolean preserveSpeed;
        int recoveryTicks;
        int airborneTicks;
        boolean synced;
        boolean authoritativeActive;
    }

    private static int level(AbstractHorse horse) {
        return horse.getItemBySlot(EquipmentSlot.BODY).getEnchantments().entrySet().stream()
                .filter(e -> e.getKey().unwrapKey().map(k -> k.location().equals(MOMENTUM)).orElse(false))
                .mapToInt(e -> e.getIntValue()).max().orElse(0);
    }

    private static void clear(AbstractHorse horse) {
        RideState previous = STATES.remove(horse);
        if (!horse.level().isClientSide() && previous != null
                && horse.getControllingPassenger() instanceof ServerPlayer rider) {
            PacketDistributor.sendToPlayer(rider, new SyncMomentumPayload(horse.getId(), false));
        }
        horse.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(MOMENTUM);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof AbstractHorse horse
                && (!horse.isAlive() || !(horse.getControllingPassenger() instanceof Player) || level(horse) == 0)) {
            clear(horse);
        }
    }

    // Called after vanilla updates the horse's heading, before ridden movement.
    public static void tickRidden(AbstractHorse horse, Player rider, Vec3 input) {
        if (horse.level().isClientSide() && !horse.isControlledByLocalInstance()) return;
        int level = level(horse);
        if (level == 0 || !horse.isAlive()) { clear(horse); return; }
        RideState state = STATES.computeIfAbsent(horse, ignored -> new RideState());
        if (!rider.getUUID().equals(state.rider)) {
            state.charge.reset(); state.groundSpeed = 0; state.lastPosition = null;
            state.idleTicks = MOVEMENT_GRACE_TICKS;
            state.rider = rider.getUUID();
            state.synced = false;
            state.authoritativeActive = false;
        }
        boolean moving = state.lastPosition != null && horse.position().subtract(state.lastPosition).horizontalDistanceSqr() > 1.0E-6;
        state.lastPosition = horse.position();
        // Vehicle position packets need not arrive once per server tick. A brief gap must not
        // erase two seconds of charge. A held wall still cancels after the short movement grace period.
        state.idleTicks = moving ? 0 : Math.min(MOVEMENT_GRACE_TICKS, state.idleTicks + 1);
        boolean recovering = state.recoveryTicks > 0;
        if (recovering) state.recoveryTicks--;
        boolean eligible = input.z > 0 && Math.abs(input.x) < 0.1
                && (recovering || state.idleTicks < MOVEMENT_GRACE_TICKS)
                && !horse.isInWaterOrBubble() && !horse.isInLava();
        boolean wasActive = state.charge.active();
        boolean active;
        if (horse.level().isClientSide()) {
            // Speed and proc audio share the server's state, rather than two independent timers.
            active = state.authoritativeActive;
        } else {
            active = state.charge.update(horse.getYRot(), eligible);
            if (!state.synced || active != wasActive) {
                if (rider instanceof ServerPlayer serverRider) {
                    PacketDistributor.sendToPlayer(serverRider, new SyncMomentumPayload(horse.getId(), active));
                }
                state.synced = true;
            }
            if (active && !wasActive && !horse.isSilent() && horse.level() instanceof ServerLevel server) {
                server.playSound(rider, horse.getX(), horse.getY(), horse.getZ(),
                        ModSounds.MOMENTUM_PROC.get(), horse.getSoundSource(), 1.0F, 1.0F);
            }
        }
        // Downhill preservation is independent of straight-line charge and steering.
        state.preserveSpeed = !recovering && input.z > 0 && !horse.isInWaterOrBubble() && !horse.isInLava();
        if (!state.preserveSpeed) state.groundSpeed = 0;
        var speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        var potion = speed.getModifier(SPEED);
        double potionBonus = potion == null ? 0 : Math.max(0, potion.amount());
        double amount = active ? MomentumState.additionalMultiplier(level, potionBonus) : 0;
        var existing = speed.getModifier(MOMENTUM);
        if (amount <= 1.0E-7) {
            if (existing != null) speed.removeModifier(MOMENTUM);
        } else if (existing == null || Math.abs(existing.amount() - amount) > 1.0E-7) {
            speed.removeModifier(MOMENTUM);
            speed.addTransientModifier(new AttributeModifier(MOMENTUM, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (active && horse.onGround() && horse.tickCount % 5 == 0 && horse.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, horse.getX(), horse.getY() + 0.1, horse.getZ(),
                    1, 0.15, 0.02, 0.15, 0.005);
        }
    }

    public static void receiveState(AbstractHorse horse, Player rider, boolean active) {
        if (!horse.level().isClientSide()) return;
        RideState state = STATES.computeIfAbsent(horse, ignored -> new RideState());
        boolean proc = active && (!rider.getUUID().equals(state.rider) || !state.authoritativeActive);
        state.rider = rider.getUUID();
        state.authoritativeActive = active;
        if (proc && !horse.isSilent()) {
            horse.level().playLocalSound(horse.getX(), horse.getY(), horse.getZ(),
                    ModSounds.MOMENTUM_PROC.get(), horse.getSoundSource(), 1.0F, 1.0F, false);
        }
    }
    public static boolean canAirStep(AbstractHorse horse, Vec3 movement) {
        return horse.isAlive() && !horse.onGround() && movement.y <= 0
                && movement.horizontalDistanceSqr() > 1.0E-8
                && horse.getControllingPassenger() instanceof Player rider && rider.zza > 0
                && !horse.isInWaterOrBubble() && !horse.isInLava() && level(horse) > 0;
    }

    // Called after vanilla acceleration, before collision-aware movement. Never changes vertical velocity.
    public static void preserveDownhillSpeed(AbstractHorse horse) {
        RideState state = STATES.get(horse);
        if (state == null || !state.preserveSpeed || !horse.isControlledByLocalInstance()) return;
        Vec3 movement = horse.getDeltaMovement();
        if (horse.onGround()) {
            state.airborneTicks = 0;
            state.groundSpeed = horse.isJumping() || horse.horizontalCollision || horse.hurtTime > 0 ? 0
                    : Math.min(movement.horizontalDistance(), horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.5);
            return;
        }
        state.airborneTicks++;
        if (horse.isJumping() || movement.y > 0 || horse.horizontalCollision || horse.hurtTime > 0
                || horse.isInWaterOrBubble() || horse.isInLava() || state.airborneTicks > MAX_DOWNHILL_TICKS
                || horse.level().noCollision(horse, horse.getBoundingBox().expandTowards(0, -MAX_DOWNHILL_DROP, 0))) {
            state.groundSpeed = 0;
            return;
        }
        double horizontal = movement.horizontalDistance();
        if (state.groundSpeed > 0 && horizontal > 1.0E-6) {
            double scale = state.groundSpeed / horizontal;
            Vec3 preserved = new Vec3(movement.x * scale, movement.y, movement.z * scale);
            // Preview the same collision solver used by local movement and server validation.
            var path = horse.getBoundingBox().deflate(1.0E-4)
                    .expandTowards(preserved.x, 0, preserved.z);
            Vec3 resolved = ((MomentumCollisionAccess) horse).vr$previewCollision(preserved);
            boolean safeStep = canAirStep(horse, preserved)
                    && Math.abs(resolved.x - preserved.x) < 1.0E-6
                    && Math.abs(resolved.z - preserved.z) < 1.0E-6
                    && resolved.y > preserved.y;
            if (!horse.level().noCollision(horse, path) && !safeStep) {
                state.groundSpeed = 0;
                state.recoveryTicks = COLLISION_RECOVERY_TICKS;
                return;
            }
            horse.setDeltaMovement(preserved);
        }
    }
    /** Discard rejected horizontal movement instead of replaying it after a server correction. */
    public static void onVehicleCorrection(AbstractHorse horse) {
        RideState state = STATES.get(horse);
        if (state == null || level(horse) == 0) return;
        state.groundSpeed = 0;
        state.airborneTicks = 0;
        state.preserveSpeed = false;
        state.recoveryTicks = COLLISION_RECOVERY_TICKS;
        // Recovery discards downhill carry, not the earned speed bonus. Steering and
        // forward-input checks continue normally during the brief recovery window.
        state.lastPosition = horse.position();
        state.idleTicks = 0;
        Vec3 movement = horse.getDeltaMovement();
        horse.setDeltaMovement(0, movement.y, 0);
    }
}
