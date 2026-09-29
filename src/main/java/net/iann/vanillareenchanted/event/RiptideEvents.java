package net.iann.vanillareenchanted.event;

import java.util.Comparator;
import net.iann.vanillareenchanted.enchantment.RiptideCooldown;
import net.iann.vanillareenchanted.enchantment.RiptideSpinAccess;
import net.iann.vanillareenchanted.network.SyncRiptidePayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RiptideEvents {
    public static final float IMPACT_DAMAGE = 12.0F;
    public static final double RECOIL_SPEED = .5, RECOIL_LIFT = .5;
    public static RiptideCooldown data(Player player) {
        return ((RiptideCooldown.Access)player.getCooldowns()).vr$riptideCooldown();
    }
    public static float cooldownPercent(Player player, ItemStack stack, float partialTick) {
        return enchanted(stack) ? data(player).fraction(partialTick) : 0;
    }
    public static boolean enchanted(ItemStack stack) {
        return stack.getEnchantments().entrySet().stream()
                .anyMatch(e -> e.getKey().is(Enchantments.RIPTIDE) && e.getIntValue() > 0);
    }
    public static void start(Player player) {
        var state = data(player);
        state.start();
        state.previousBox = player.getBoundingBox();
        state.direction = player.getDeltaMovement().lengthSqr() > 1.0E-6
                ? player.getDeltaMovement().normalize() : player.getLookAngle();
        sync(player);
    }
    private static void sync(Player player) {
        if (player instanceof ServerPlayer server && !(player instanceof FakePlayer)) {
            var state = data(player);
            PacketDistributor.sendToPlayer(server, new SyncRiptidePayload(state.remaining, state.waiting, state.spinning));
        }
    }
    private static void stop(Player player) {
        data(player).spinning = false;
        ((RiptideSpinAccess)player).vr$stopSpin();
        sync(player);
    }
    public static void collide(Player player, AABB before, AABB after) {
        var state = data(player);
        var spin = (RiptideSpinAccess)player;
        if (player.level().isClientSide) {
            // Damage and recoil belong to the server; do not predict a second impact.
            if (spin.vr$spinTicks() <= 0) stop(player);
            return;
        }
        AABB previous = state.previousBox == null ? before : state.previousBox;
        Vec3 from = previous.getCenter(), to = after.getCenter();
        Vec3 motion = to.subtract(from);
        if (motion.lengthSqr() > 1.0E-6) state.direction = motion.normalize();
        state.previousBox = after;
        double halfWidth = after.getXsize() / 2, halfHeight = after.getYsize() / 2;
        var target = player.level().getEntitiesOfClass(LivingEntity.class, previous.minmax(after).inflate(.05),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.isAttackable()
                        && !e.isPassengerOfSameVehicle(player) && !player.isAlliedTo(e)
                        && (!(e instanceof Player other) || player.canHarmPlayer(other))
                        && player.hasLineOfSight(e)
                        && (e.getBoundingBox().inflate(halfWidth, halfHeight, halfWidth).contains(from)
                            || e.getBoundingBox().inflate(halfWidth, halfHeight, halfWidth).clip(from, to).isPresent()))
                .stream().min(Comparator.comparingDouble(e -> e.getBoundingBox()
                        .inflate(halfWidth, halfHeight, halfWidth).clip(from, to).orElse(from).distanceToSqr(from))).orElse(null);
        if (target != null) {
            // Ability damage bypasses melee charge, critical hits and attack-damage bonuses.
            // The target's normal damage handling still applies armor, shields and resistance.
            target.hurt(player.damageSources().playerAttack(player), IMPACT_DAMAGE);
            Vec3 recoil = state.direction.scale(-RECOIL_SPEED).add(0, RECOIL_LIFT, 0);
            stop(player);
            player.setDeltaMovement(recoil);
            player.hasImpulse = true;
            player.hurtMarked = true;
            if (player instanceof ServerPlayer server && !(player instanceof FakePlayer))
                server.connection.send(new ClientboundSetEntityMotionPacket(player));
        } else if (spin.vr$spinTicks() <= 0 || player.horizontalCollision) {
            stop(player);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void incoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !data(player).spinning
                || ((RiptideSpinAccess)player).vr$spinTicks() <= 0) return;
        var source = event.getSource();
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && (source.is(DamageTypeTags.IS_PROJECTILE)
                    || source.getDirectEntity() instanceof LivingEntity && !source.is(DamageTypeTags.IS_EXPLOSION)))
            event.setCanceled(true);
    }
    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var state = data(player);
        if (state.remaining <= 0) return;
        if (state.spinning && (!player.isAlive() || ((RiptideSpinAccess)player).vr$spinTicks() <= 0)) stop(player);
        boolean wasWaiting = state.waiting, wasWater = state.water;
        int oldRemaining = state.remaining;
        state.water = player.isInWaterOrBubble();
        state.tick(player.onGround(), state.water);
        if (wasWaiting != state.waiting || wasWater != state.water || oldRemaining > 0 && state.remaining == 0
                || player.tickCount % 20 == 0) sync(player);
    }
    private RiptideEvents() {}
}
