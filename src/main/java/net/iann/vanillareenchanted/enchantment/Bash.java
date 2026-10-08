package net.iann.vanillareenchanted.enchantment;

import java.util.Map;
import java.util.WeakHashMap;
import net.iann.vanillareenchanted.network.SyncBashPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Bash {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "bash");
    public static final float DAMAGE = 4;
    public static final int COOLDOWN_TICKS = 60, IMPACT_TICKS = 1, ANIMATION_TICKS = 10;
    public static final double KNOCKBACK = 0.9, UPWARD_KNOCKBACK = 0.35;
    private record Strike(ItemStack shield, Item item, InteractionHand hand, int startTick, ResourceLocation dimension) {}
    private static final Map<ServerPlayer, Strike> STRIKES = new WeakHashMap<>();

    public static boolean enchanted(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK) && stack.getEnchantments().entrySet().stream()
                .anyMatch(e -> e.getIntValue() > 0 && e.getKey().unwrapKey()
                        .map(key -> key.location().equals(ID)).orElse(false));
    }

    public static void begin(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !player.isBlocking() || STRIKES.containsKey(player)) return;
        ItemStack shield = player.getUseItem();
        if (!enchanted(shield) || player.getCooldowns().isOnCooldown(shield.getItem())) return;
        InteractionHand hand = player.getUsedItemHand();
        STRIKES.put(player, new Strike(shield, shield.getItem(), hand, player.tickCount, player.level().dimension().location()));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SyncBashPayload(player.getId(), hand == InteractionHand.OFF_HAND));
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (STRIKES.containsKey(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Strike strike = STRIKES.get(player);
        if (strike == null) return;
        int elapsed = player.tickCount - strike.startTick();
        if (!player.isAlive() || player.isSpectator() || elapsed < 0 || elapsed >= ANIMATION_TICKS
                || player.getItemInHand(strike.hand()) != strike.shield()
                || !enchanted(strike.shield()) || !player.level().dimension().location().equals(strike.dimension())) {
            STRIKES.remove(player);
            if (player.getUseItem() == strike.shield()) player.stopUsingItem();
            // Preserve an existing shield-disable cooldown, e.g. an axe hit during Bash.
            if (!player.getCooldowns().isOnCooldown(strike.item())) {
                player.getCooldowns().addCooldown(strike.item(), COOLDOWN_TICKS);
            }
            return;
        }
        if (elapsed == IMPACT_TICKS) hit(player);
    }

    private static void hit(ServerPlayer player) {
        boolean hit = false;
        for (var target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, AimedSweep.bounds(player))) {
            if (target == player || !target.isAlive() || target.isSpectator() || !target.isAttackable()
                    || player.isAlliedTo(target) || target.isPassengerOfSameVehicle(player)
                    || target instanceof ArmorStand stand && stand.isMarker()
                    || target instanceof Player other && !player.canHarmPlayer(other)
                    || player.distanceToSqr(target) >= player.entityInteractionRange() * player.entityInteractionRange()
                    || !player.hasLineOfSight(target)) continue;
            Vec3 previous = target.getDeltaMovement();
            if (!target.hurt(player.damageSources().playerAttack(player), DAMAGE)) continue;
            Vec3 direction = new Vec3(target.getX() - player.getX(), 0, target.getZ() - player.getZ());
            if (direction.lengthSqr() < 1.0E-8) {
                double yaw = Math.toRadians(player.getYRot());
                direction = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            } else direction = direction.normalize();
            // Set the impulse directly: LivingEntity.knockback would scale it by resistance.
            target.setDeltaMovement(previous.x * 0.5 + direction.x * KNOCKBACK,
                    Math.max(previous.y, UPWARD_KNOCKBACK), previous.z * 0.5 + direction.z * KNOCKBACK);
            target.hasImpulse = true;
            target.hurtMarked = true;
            if (target instanceof ServerPlayer victim) victim.connection.send(new ClientboundSetEntityMotionPacket(victim));
            player.setLastHurtMob(target);
            hit = true;
        }
        if (hit) {
            player.causeFoodExhaustion(0.1F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, player.getSoundSource(), 1, 0.9F);
        }
    }

    private Bash() {}
}
