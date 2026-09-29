package net.iann.vanillareenchanted.event;

import java.util.*;
import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.enchantment.WindUp;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** An instantaneous, ground-bound cone emitted by a mace smash or aimed Wind Up ground strike. */
public final class ShockwaveEvents {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(VanillaReenchanted.MODID, "shockwave");
    public static final double RANGE = 6;
    public static final double CONE_DEGREES = 60;
    public static final double MAX_TERRAIN_STEP = 1.0;
    private static final double TERRAIN_SAMPLE_SPACING = 0.25;
    public static final float DAMAGE_PER_LEVEL = 0.25F;
    public static final float GROUND_SLAM_MIN_PITCH = 60;
    private static final double COS_HALF_ANGLE = Math.cos(Math.toRadians(CONE_DEGREES / 2));
    private static final ThreadLocal<Boolean> APPLYING_WAVE = ThreadLocal.withInitial(() -> false);

    private static final class Wave {
        final UUID primary;
        final Vec3 origin, forward;
        final float damage;
        GroundAttack groundAttack;

        Wave(ServerPlayer attacker, LivingEntity primary, Vec3 origin, Vec3 forward, float damage) {
            this.primary = primary == null ? null : primary.getUUID();
            this.origin = origin;
            this.forward = forward;
            this.damage = damage;
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (APPLYING_WAVE.get() || !(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getDirectEntity() instanceof ServerPlayer player)
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || event.getOriginalDamage() <= 0 || !Float.isFinite(event.getOriginalDamage())
                || event.getBlockedDamage() >= event.getOriginalDamage()
                || !(player.getMainHandItem().getItem() instanceof MaceItem) || !MaceItem.canSmashAttack(player) || !isFalling(player)) return;
        int enchantmentLevel = level(player.getMainHandItem());
        if (enchantmentLevel <= 0) return;
        LivingEntity primary = event.getEntity();
        Vec3 forward = primary.position().subtract(player.position()).multiply(1, 0, 1);
        if (forward.lengthSqr() < 0.0001) forward = player.getLookAngle().multiply(1, 0, 1);
        if (forward.lengthSqr() < 0.0001) return;
        Vec3 origin = ground(level, primary.position(), player);
        if (origin == null) return;
        // Use the hit before the primary target's armor: each secondary target applies its own defenses once.
        float damage = event.getOriginalDamage() * DAMAGE_PER_LEVEL * Math.min(3, enchantmentLevel);
        emitCone(level, player, new Wave(player, primary, origin, forward.normalize(), damage));
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.4F, 0.65F);
    }

    private record GroundAttack(ItemStack weapon, float baseDamage, float height, float scale) {
        float damage(ServerLevel level, ServerPlayer player, LivingEntity target) {
            var source = player.damageSources().playerAttack(player);
            // Same mace smash curve; ground strikes have no primary enemy to critically hit.
            float smash = height <= 3 ? 4 * height : height <= 8 ? 12 + 2 * (height - 3) : 22 + height - 8;
            return (EnchantmentHelper.modifyDamage(level, weapon, target, source, baseDamage) + smash
                    + EnchantmentHelper.modifyFallBasedDamage(level, weapon, target, source, 0) * height) * scale;
        }
    }

    public static int level(ItemStack stack) {
        return stack.getEnchantments().entrySet().stream()
                .filter(entry -> entry.getKey().unwrapKey().map(key -> key.location().equals(ID)).orElse(false))
                .mapToInt(entry -> entry.getIntValue()).max().orElse(0);
    }

    private static boolean isFalling(ServerPlayer player) {
        // ServerPlayer delta movement can be stale; use movement reported by the client.
        return !player.onGround() && player.getKnownMovement().y < 0;
    }

    public static boolean tryGroundSlam(ServerPlayer player, ItemStack weapon) {
        int enchantmentLevel = level(weapon);
        if (enchantmentLevel <= 0 || !WindUp.isReleasing(player) || WindUp.level(weapon) <= 0
                || !player.isAlive() || player.isSpectator() || !isFalling(player)
                || player.getXRot() < GROUND_SLAM_MIN_PITCH) return false;
        var server = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        var hit = server.clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(player.blockInteractionRange())),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != net.minecraft.core.Direction.UP
                || hit.getLocation().y > player.getY() + 0.01) return false;
        // Yaw stays well-defined even when looking directly down.
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        var wave = new Wave(player, null, hit.getLocation(), forward, 0);
        wave.groundAttack = new GroundAttack(weapon.copy(), (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                WindUp.effectiveFallDistance(player), DAMAGE_PER_LEVEL * Math.min(3, enchantmentLevel));
        emitCone(server, player, wave);
        weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        server.playSound(null, hit.getBlockPos(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1, 1);
        return true;
    }

    private static void emitCone(ServerLevel level, ServerPlayer attacker, Wave wave) {
        for (double radius = 1; radius <= RANGE; radius++) emitParticles(level, attacker, wave, radius);
        damageCone(level, attacker, wave);
    }

    private static void emitParticles(ServerLevel level, ServerPlayer attacker, Wave wave, double radius) {
        for (int i = 0; i <= 8; i++) {
            double angle = Math.toRadians(-CONE_DEGREES / 2 + CONE_DEGREES * i / 8);
            Vec3 direction = new Vec3(wave.forward.x * Math.cos(angle) - wave.forward.z * Math.sin(angle), 0,
                    wave.forward.x * Math.sin(angle) + wave.forward.z * Math.cos(angle));
            Vec3 surface = followGround(level, attacker, wave.origin, wave.origin.add(direction.scale(radius)));
            if (surface == null) continue;
            var state = level.getBlockState(BlockPos.containing(surface.add(0, -0.05, 0)));
            level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, state), surface.x, surface.y + 0.08, surface.z,
                    5, 0.12, 0.05, 0.12, 0.2);
        }
    }

    private static void damageCone(ServerLevel level, ServerPlayer attacker, Wave wave) {
        var bounds = new AABB(wave.origin.x - RANGE, wave.origin.y - RANGE * 2 - 2, wave.origin.z - RANGE,
                wave.origin.x + RANGE, wave.origin.y + RANGE * 2 + 3, wave.origin.z + RANGE);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, bounds, target -> target.isAlive() && !target.isSpectator() && target.onGround())) {
            if (target == attacker || target.getUUID().equals(wave.primary)
                    || attacker.isAlliedTo(target) || target.isAlliedTo(attacker)
                    || target instanceof ServerPlayer other && !attacker.canHarmPlayer(other)) continue;
            Vec3 offset = target.position().subtract(wave.origin).multiply(1, 0, 1);
            double distance = offset.length();
            if (distance < 0.01 || distance > RANGE
                    || offset.scale(1 / distance).dot(wave.forward) < COS_HALF_ANGLE) continue;
            Vec3 surface = followGround(level, attacker, wave.origin, target.position());
            if (surface == null || Math.abs(target.getY() - surface.y) > 1) continue;
            APPLYING_WAVE.set(true);
            try {
                float damage = wave.groundAttack == null ? wave.damage : wave.groundAttack.damage(level, attacker, target);
                if (Float.isFinite(damage) && damage > 0) target.hurt(attacker.damageSources().playerAttack(attacker), damage);
            } finally {
                APPLYING_WAVE.remove();
            }
        }
    }

    private static Vec3 ground(ServerLevel level, Vec3 point, ServerPlayer attacker) {
        if (!level.hasChunkAt(BlockPos.containing(point))) return null;
        var hit = level.clip(new ClipContext(point.add(0, 1, 0), point.add(0, -2, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == net.minecraft.core.Direction.UP
                ? hit.getLocation() : null;
    }

    /** Follow successive ground heights rather than drawing a straight ray through the hillside. */
    private static Vec3 followGround(ServerLevel level, ServerPlayer attacker, Vec3 from, Vec3 to) {
        Vec3 horizontal = to.subtract(from).multiply(1, 0, 1);
        int steps = Math.max(1, (int)Math.ceil(horizontal.length() / TERRAIN_SAMPLE_SPACING));
        Vec3 previous = from;
        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.add(horizontal.scale((double)i / steps)).with(net.minecraft.core.Direction.Axis.Y, previous.y);
            if (!level.hasChunkAt(BlockPos.containing(point))) return null;
            var hit = level.clip(new ClipContext(point.add(0, MAX_TERRAIN_STEP + 0.01, 0),
                    point.add(0, -MAX_TERRAIN_STEP - 0.01, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != net.minecraft.core.Direction.UP) return null;
            Vec3 next = hit.getLocation();
            if (Math.abs(next.y - previous.y) > MAX_TERRAIN_STEP + 0.001) return null;
            // Check just above each step, allowing stairs but not tunneling through a wall.
            double clearance = Math.max(previous.y, next.y) + 0.05;
            if (level.clip(new ClipContext(new Vec3(previous.x, clearance, previous.z),
                    new Vec3(next.x, clearance, next.z), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, attacker)).getType() != HitResult.Type.MISS) return null;
            previous = next;
        }
        return previous;
    }

    private ShockwaveEvents() {}
}
