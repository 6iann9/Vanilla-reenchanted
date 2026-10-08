package net.iann.vanillareenchanted.enchantment;

import java.util.Map;
import java.util.WeakHashMap;
import net.iann.vanillareenchanted.network.SyncUpdraftPayload;
import net.iann.vanillareenchanted.network.UpdraftEffectPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Updraft {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "updraft");
    public static final int CHECK_TICKS = 10, CHARGE_TICKS = 60, COOLDOWN_TICKS = 140;
    public static final int IDLE = 0, CHECKING = 1, CHARGING = 2, AIRBORNE = 3, COOLDOWN = 4;
    public static final int START = 0, CANCEL = 1, RELEASE = 2;
    public static final double STATIONARY_EPSILON = 0.002;
    private static final String WAITING = "iannvanillareenchanted.UpdraftWaiting";
    private static final String READY_AT = "iannvanillareenchanted.UpdraftReadyAt";
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();

    private static final class State {
        int stage, since;
        boolean latched, leftGround, sent;
        long readyAt;
        Vec3 previous, anchor;
        ItemStack chest;
        State(ServerPlayer player) {
            previous = player.position();
            // Latch an actual start request, not the crouch flag already accompanying that request.
            readyAt = player.getPersistentData().getLong(READY_AT);
            stage = player.getPersistentData().getBoolean(WAITING) ? AIRBORNE
                    : readyAt > player.level().getGameTime() ? COOLDOWN : IDLE;
            leftGround = stage == AIRBORNE;
        }
    }

    public static int level(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).getEnchantments().entrySet().stream()
                .filter(e -> e.getKey().unwrapKey().map(key -> key.location().equals(ID)).orElse(false))
                .mapToInt(e -> e.getIntValue()).max().orElse(0);
    }

    public static boolean grounded(Player player) {
        return player.isAlive() && !player.isSpectator() && player.onGround() && !player.isPassenger()
                && !player.isInWaterOrBubble() && !player.isInLava() && !player.isFallFlying()
                && !player.getAbilities().flying && !player.isSleeping() && level(player) > 0;
    }

    public static boolean stationary(Vec3 a, Vec3 b) {
        return Math.abs(a.x - b.x) <= STATIONARY_EPSILON && Math.abs(a.z - b.z) <= STATIONARY_EPSILON
                && Math.abs(a.y - b.y) <= 0.01;
    }

    public static void input(ServerPlayer player, int action) {
        State s = STATES.computeIfAbsent(player, State::new);
        if (action == START) {
            if (s.latched) return;
            s.latched = true;
            if (s.stage != IDLE || !grounded(player) || !player.isShiftKeyDown()) {
                sync(player, s);
                return;
            }
            s.chest = player.getItemBySlot(EquipmentSlot.CHEST);
            s.anchor = player.position();
            s.stage = CHECKING;
            s.since = player.tickCount;
            sync(player, s);
        } else if (action == CANCEL || action == RELEASE) {
            if (action == RELEASE && s.stage == CHARGING && player.tickCount - s.since >= CHARGE_TICKS
                    && !player.isShiftKeyDown() && valid(player, s)) {
                launch(player, s);
            } else if (s.stage == CHECKING || s.stage == CHARGING) cancel(player, s);
        }
    }

    private static boolean valid(ServerPlayer player, State s) {
        return grounded(player) && player.getItemBySlot(EquipmentSlot.CHEST) == s.chest
                // Give walking friction half a second to settle before enforcing stillness.
                && (s.stage == CHECKING || (stationary(player.position(), s.previous)
                && player.position().distanceToSqr(s.anchor) <= 0.0001));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        State s = STATES.computeIfAbsent(player, State::new);
        if (!player.isShiftKeyDown()) s.latched = false;
        if (!s.sent) sync(player, s);
        if (s.stage == CHECKING || s.stage == CHARGING) {
            if (!valid(player, s)) cancel(player, s);
            else if (!player.isShiftKeyDown()) {
                // The explicit release packet decides whether to launch. Never launch from a lost crouch flag.
                if (player.tickCount - s.since > CHARGE_TICKS + 40) cancel(player, s);
            } else if (s.stage == CHECKING && player.tickCount - s.since >= CHECK_TICKS) {
                if (!stationary(player.position(), s.previous)) {
                    cancel(player, s);
                    s.previous = player.position();
                    return;
                }
                s.anchor = player.position();
                s.stage = CHARGING;
                s.since = player.tickCount;
                sync(player, s);
            } else if (s.stage == CHARGING) {
                if (player.tickCount % 10 == 0 || player.tickCount - s.since == CHARGE_TICKS) sync(player, s);
            }
        } else if (s.stage == AIRBORNE) {
            if (!player.onGround()) s.leftGround = true;
            if (player.onGround() && player.tickCount - s.since > 1
                    && (s.leftGround || player.tickCount - s.since >= 10)) {
                s.stage = COOLDOWN;
                s.readyAt = player.level().getGameTime() + COOLDOWN_TICKS;
                player.getPersistentData().putBoolean(WAITING, false);
                player.getPersistentData().putLong(READY_AT, s.readyAt);
                sync(player, s);
            }
        } else if (s.stage == COOLDOWN && player.level().getGameTime() >= s.readyAt) {
            s.stage = IDLE;
            sync(player, s);
        }
        s.previous = player.position();
    }

    private static void cancel(ServerPlayer player, State s) {
        s.stage = IDLE;
        s.chest = null;
        sync(player, s);
    }

    private static void launch(ServerPlayer player, State s) {
        double height = 20 + 5 * (Math.clamp(level(player), 1, 3) - 1);
        double speed = WindBurst.launchSpeed(height, player.getAttributeValue(Attributes.GRAVITY));
        player.setDeltaMovement(0, speed, 0);
        player.setOnGround(false);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        s.stage = AIRBORNE;
        s.since = player.tickCount;
        s.leftGround = false;
        player.getPersistentData().putBoolean(WAITING, true);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new UpdraftEffectPayload(
                player.getId(), UpdraftEffectPayload.JUMP, player.getX(), player.getY(), player.getZ()));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), player.getSoundSource(), 0.8F, 1.2F);
        sync(player, s);
    }

    private static void sync(ServerPlayer player, State s) {
        int time = s.stage == CHARGING ? Math.min(CHARGE_TICKS, player.tickCount - s.since)
                : s.stage == COOLDOWN ? (int)Math.clamp(s.readyAt - player.level().getGameTime(), 0L, (long)COOLDOWN_TICKS) : 0;
        PacketDistributor.sendToPlayer(player, new SyncUpdraftPayload(s.stage, time));
        // Repeated active messages refresh visibility for newly tracking players without restarting the effect.
        int effectAction = s.stage == CHARGING
                ? (time >= CHARGE_TICKS ? UpdraftEffectPayload.FINISH : UpdraftEffectPayload.START)
                : UpdraftEffectPayload.STOP;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new UpdraftEffectPayload(player.getId(), effectAction));
        s.sent = true;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { STATES.remove(event.getEntity()); }

    private Updraft() {}
}
