package net.iann.vanillareenchanted.enchantment;

import net.iann.vanillareenchanted.mixin.SweepDamageAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.ItemAbilities;

public final class AimedSweep {
    public static final double FORWARD_OFFSET = 2.0;
    public static final double HORIZONTAL_PADDING = 1.0;
    public static final double VERTICAL_PADDING = 0.25;
    public static final float MIN_ATTACK_STRENGTH = 0.8F;

    public static boolean hasSweepingEdge(Player player) {
        var stack = player.getMainHandItem();
        return stack.canPerformAction(ItemAbilities.SWORD_SWEEP)
                && stack.getEnchantments().entrySet().stream()
                .anyMatch(e -> e.getKey().is(Enchantments.SWEEPING_EDGE) && e.getIntValue() > 0);
    }

    public static boolean canSweep(Player player) {
        return hasSweepingEdge(player) && player.isAlive() && !player.isSpectator()
                && !player.isUsingItem() && !player.isAutoSpinAttack()
                && player.getAttackStrengthScale(0.5F) >= MIN_ATTACK_STRENGTH
                && player.onGround() && !player.isSprinting()
                && player.walkDist - player.walkDistO < player.getSpeed();
    }

    public static AABB bounds(Player player) {
        // Combat Test's expanded player box, moved along pitch as well as yaw.
        return player.getBoundingBox().inflate(HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING)
                .move(player.getLookAngle().scale(FORWARD_OFFSET));
    }

    public static void miss(ServerPlayer player) {
        if (!hasSweepingEdge(player) || !player.isAlive() || player.isSpectator()) return;
        try {
            if (!canSweep(player)) return;
            EchoingEdge.attack(player, null, () -> damage(player));
        } finally {
            // Misses spend the attack charge, including misses below the sweep threshold.
            player.resetAttackStrengthTicker();
        }
    }

    private static void damage(ServerPlayer player) {
        float strength = player.getAttackStrengthScale(0.5F);
        float base = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (0.2F + strength * strength * 0.8F);
        float sweepDamage = 1.0F + (float)player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * base;
        var source = player.damageSources().playerAttack(player);
        var weapon = player.getMainHandItem();
        LivingEntity firstHit = null;
        for (var target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds(player))) {
            if (target == player || !target.isAlive() || target.isSpectator() || !target.isAttackable()
                    || player.isAlliedTo(target) || (target instanceof ArmorStand stand && stand.isMarker())
                    || (target instanceof Player other && !player.canHarmPlayer(other))
                    || player.distanceToSqr(target) >= Mth.square(player.entityInteractionRange())
                    || !player.hasLineOfSight(target)) continue;
            float damage = ((SweepDamageAccess)player).vr$enchantedSweepDamage(target, sweepDamage, source) * strength;
            boolean hit;
            try {
                hit = target.hurt(source, EchoingEdge.beginHit(target, damage));
            } finally {
                if (EchoingEdge.current() != null) EchoingEdge.current().activeTarget = null;
            }
            if (!hit) continue;
            target.knockback(0.4F, Mth.sin(player.getYRot() * Mth.DEG_TO_RAD),
                    -Mth.cos(player.getYRot() * Mth.DEG_TO_RAD));
            EnchantmentHelper.doPostAttackEffects(player.serverLevel(), target, source);
            if (firstHit == null) firstHit = target;
        }
        if (firstHit != null) {
            player.setLastHurtMob(firstHit);
            if (weapon.hurtEnemy(firstHit, player)) weapon.postHurtEnemy(firstHit, player);
            player.causeFoodExhaustion(0.1F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 1.0F);
            player.sweepAttack();
        }
    }

    private AimedSweep() {}
}
