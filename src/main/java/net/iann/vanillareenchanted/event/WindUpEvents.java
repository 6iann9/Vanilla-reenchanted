package net.iann.vanillareenchanted.event;

import net.iann.vanillareenchanted.network.SyncWindUpPayload;
import net.iann.vanillareenchanted.network.WindUpPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.Map;
import java.util.WeakHashMap;
import net.iann.vanillareenchanted.enchantment.WindUp;
import net.iann.vanillareenchanted.enchantment.WindBurst;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class WindUpEvents {
    private record Charge(ItemStack stack, int level, long started, Object dimension) {}
    private static final Map<ServerPlayer, Charge> CHARGES = new WeakHashMap<>();

    public static void begin(ServerPlayer player) {
        WindUp.checkEquipCooldown(player);
        ItemStack stack = player.getMainHandItem();
        int level = WindUp.level(stack);
        if (CHARGES.containsKey(player)) return;
        if (level == 0 || !player.isAlive() || player.isSpectator()
                || player.isUsingItem() || WindUp.isCoolingDown(player) || player.getCooldowns().isOnCooldown(stack.getItem())) {
            reject(player);
            return;
        }
        CHARGES.put(player, new Charge(stack, level, player.serverLevel().getGameTime(), player.level()));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        broadcast(player, WindUpPayload.START, 0);
    }

    private static boolean valid(ServerPlayer player, Charge charge) {
        return player.isAlive() && !player.isSpectator() && player.level() == charge.dimension
                && player.getMainHandItem() == charge.stack && WindUp.level(charge.stack) == charge.level
                && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && !WindUp.isCoolingDown(player) && !player.getCooldowns().isOnCooldown(charge.stack.getItem());
    }

    public static void cancel(ServerPlayer player) {
        if (CHARGES.remove(player) != null) {
            player.stopUsingItem();
            broadcast(player, WindUpPayload.CANCEL, 0);
            reject(player);
        }
    }

    public static void release(ServerPlayer player) {
        Charge charge = CHARGES.remove(player);
        if (charge == null) { reject(player); return; }
        boolean valid = valid(player, charge);
        player.stopUsingItem();
        if (!valid) {
            broadcast(player, WindUpPayload.CANCEL, 0);
            reject(player);
            return;
        }
        broadcast(player, WindUpPayload.RELEASE, 0);
        float bonus = WindUp.bonusHeight(charge.level, player.serverLevel().getGameTime() - charge.started);
        Entity target = findTarget(player);
        int burstBefore = WindBurst.procSequence(player);
        try {
            if (target != null) WindUp.attack(player, bonus, () -> player.attack(target));
            else WindUp.attack(player, bonus, () -> {
                player.resetAttackStrengthTicker();
                if (WindBurst.tryGroundSlam(player, charge.stack)) WindBurst.finishAttack(player);
                else ShockwaveEvents.tryGroundSlam(player, charge.stack);
            });
            player.swing(InteractionHand.MAIN_HAND, true);
        } finally {
            WindUp.startCooldown(player, WindBurst.procSequence(player) != burstBefore
                    ? WindBurst.PROC_RECOVERY_TICKS : WindUp.COOLDOWN_TICKS);
        }
    }

    private static void broadcast(ServerPlayer player, int action, int elapsed) {
        // The owner predicts visuals immediately; spectators receive the authoritative transition.
        if (!(player instanceof FakePlayer)) PacketDistributor.sendToPlayersTrackingEntity(player,
                new SyncWindUpPayload(player.getId(), action, elapsed));
    }

    private static void reject(ServerPlayer player) {
        WindUp.syncCooldown(player);
        if (!(player instanceof FakePlayer)) PacketDistributor.sendToPlayer(player,
                new SyncWindUpPayload(player.getId(), WindUpPayload.CANCEL, 0));
    }

    @SubscribeEvent
    public static void onTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer player) {
            Charge charge = CHARGES.get(player);
            if (charge != null && valid(player, charge)) PacketDistributor.sendToPlayer(viewer,
                    new SyncWindUpPayload(player.getId(), WindUpPayload.START,
                            (int) Math.min(WindUp.CHARGE_TICKS, player.serverLevel().getGameTime() - charge.started)));
        }
    }

    public static Entity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1).scale(player.entityInteractionRange()));
        // Block ray limits the entity ray, preventing attacks through walls.
        end = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
        double nearest = eye.distanceToSqr(end);
        Entity result = null;
        for (Entity candidate : player.level().getEntities(player,
                player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
                entity -> entity.isPickable() && !entity.isSpectator() && entity.isAlive())) {
            if (candidate.getRootVehicle() == player.getRootVehicle()) continue;
            var box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            var hit = box.clip(eye, end);
            double distance = box.contains(eye) ? 0 : hit.map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
            if (distance < nearest && player.canInteractWithEntity(candidate, 0)) {
                nearest = distance;
                result = candidate;
            }
        }
        return result;
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (WindUp.level(event.getEntity().getMainHandItem()) > 0 && !WindUp.isReleasing(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WindUp.checkEquipCooldown(player);
            Charge charge = CHARGES.get(player);
            if (charge != null && !valid(player, charge)) cancel(player);
        }
    }
}
