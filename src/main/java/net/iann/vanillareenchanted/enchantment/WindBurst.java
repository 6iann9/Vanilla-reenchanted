package net.iann.vanillareenchanted.enchantment;

import com.google.common.collect.MapMaker;
import java.util.Map;
import java.util.Optional;
import net.iann.vanillareenchanted.mixin.WindBurstAttackAccess;
import net.iann.vanillareenchanted.network.SyncWindBurstPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Player-specific burst lockout, independent of both item use and Wind Up recovery. */
public final class WindBurst {
    public static final int GROUND_COOLDOWN_TICKS = 100;
    public static final int PROC_RECOVERY_TICKS = 5;
    public static final double BASE_LAUNCH_HEIGHT = 6;
    public static final double HEIGHT_PER_LEVEL = 2;
    public static final float GROUND_SLAM_MIN_PITCH = 60;
    private static final int BLOCKED_TAKEOFF_GRACE_TICKS = 10;
    private static final Map<Player, State> STATES = new MapMaker().weakKeys().makeMap();

    private static final class State {
        boolean waitingForLanding;
        boolean airborneSeen;
        boolean recoveryPending;
        Boolean criticalAttack;
        int procTick;
        int cooldownEnd;
        int sequence;
        double launchY;
    }

    public static int level(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof MaceItem)) return 0;
        return player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.WIND_BURST)
                .map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack)).orElse(0);
    }

    public static boolean isReady(Player player) {
        State state = STATES.get(player);
        return state == null || !state.waitingForLanding && player.tickCount >= state.cooldownEnd;
    }

    public static boolean waitingForLanding(Player player) {
        State state = STATES.get(player);
        return state != null && state.waitingForLanding;
    }

    public static int procSequence(Player player) {
        State state = STATES.get(player);
        return state == null ? 0 : state.sequence;
    }

    public static float cooldownFraction(Player player, float partialTick) {
        State state = STATES.get(player);
        if (state == null) return 0;
        if (state.waitingForLanding) return 1;
        return Math.clamp((state.cooldownEnd - player.tickCount - partialTick) / GROUND_COOLDOWN_TICKS, 0F, 1F);
    }

    /** Crit-like movement requirements, allowing sprinting, sampled before mace hit logic changes motion. */
    public static boolean canCrit(Player player) {
        return player.getAttackStrengthScale(0.5F) > 0.9F && player.fallDistance > 0
                && !player.onGround() && player.getKnownMovement().y < 0
                && !player.onClimbable() && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger() && !player.isFallFlying()
                && !player.getAbilities().flying;
    }

    public static void recordCriticalAttack(Player player, boolean critical) {
        if (level(player.getMainHandItem(), player) > 0) {
            STATES.computeIfAbsent(player, ignored -> new State()).criticalAttack = (critical || player.isSprinting()) && canCrit(player);
        }
    }

    public static boolean tryGroundSlam(ServerPlayer player, ItemStack weapon) {
        if (!WindUp.isReleasing(player) || WindUp.level(weapon) == 0
                || player.getXRot() < GROUND_SLAM_MIN_PITCH || !canCrit(player)) return false;
        Vec3 eye = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(eye,
                eye.add(player.getViewVector(1).scale(player.blockInteractionRange())),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP
                || hit.getLocation().y > player.getY() + 0.01) return false;
        if (!tryProc(player, weapon, level(weapon, player), hit.getLocation())) return false;
        weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.serverLevel().playSound(null, hit.getBlockPos(), SoundEvents.MACE_SMASH_GROUND,
                player.getSoundSource(), 1, 1);
        return true;
    }

    public static boolean tryProc(ServerPlayer player, ItemStack weapon, int enchantmentLevel) {
        return tryProc(player, weapon, enchantmentLevel, player.position());
    }

    private static boolean tryProc(ServerPlayer player, ItemStack weapon, int enchantmentLevel, Vec3 origin) {
        State previous = STATES.get(player);
        boolean critical = previous != null && previous.criticalAttack != null ? previous.criticalAttack : canCrit(player);
        if (enchantmentLevel <= 0 || !(weapon.getItem() instanceof MaceItem) || !player.isAlive()
                || player.isSpectator() || !critical || !isReady(player)) return false;

        State state = STATES.computeIfAbsent(player, ignored -> new State());
        state.waitingForLanding = true;
        state.airborneSeen = !player.onGround();
        state.procTick = player.tickCount;
        state.launchY = player.getY();
        state.sequence++;
        state.recoveryPending = true;
        var movement = player.getDeltaMovement();
        int cappedLevel = Math.clamp(enchantmentLevel, 1, 3);
        // Preserve gusts and block interactions with gentler knockback to surrounding entities.
        float nearbyKnockback = switch (cappedLevel) { case 1 -> 0.6F; case 2 -> 0.875F; default -> 1.1F; };
        player.serverLevel().explode(null, null,
                new SimpleExplosionDamageCalculator(true, false, Optional.of(nearbyKnockback),
                        player.registryAccess().registryOrThrow(Registries.BLOCK).getTag(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(tag -> (net.minecraft.core.HolderSet<net.minecraft.world.level.block.Block>) tag)),
                origin.x, origin.y, origin.z, 3.5F, false, Level.ExplosionInteraction.TRIGGER,
                ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.WIND_CHARGE_BURST);
        double speed = launchSpeed(BASE_LAUNCH_HEIGHT + HEIGHT_PER_LEVEL * (cappedLevel - 1), player.getAttributeValue(Attributes.GRAVITY));
        player.setDeltaMovement(movement.x, speed, movement.z);
        player.setOnGround(false);
        player.hasImpulse = true;
        player.hurtMarked = true;
        if (!(player instanceof FakePlayer)) player.connection.send(new ClientboundSetEntityMotionPacket(player));
        sync(player, false);
        return true;
    }

    /** Solve vanilla vertical drag/gravity for an approximate unobstructed ascent height. */
    public static double launchSpeed(double height, double gravity) {
        gravity = Math.max(0, gravity);
        double low = 0, high = Math.max(1, height);
        for (int i = 0; i < 35; i++) {
            double speed = (low + high) / 2;
            double rise = 0, velocity = speed;
            for (int tick = 0; tick < 2000 && velocity > 0.000001; tick++) {
                rise += velocity;
                velocity = (velocity - gravity) * 0.98;
            }
            if (rise < height) low = speed;
            else high = speed;
        }
        return (low + high) / 2;
    }

    public static void tick(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null) return;
        if (!player.isAlive()) { STATES.remove(player); sync(player, false); return; }
        if (!state.waitingForLanding) return;
        if (!player.onGround() && Math.abs(player.getY() - state.launchY) > 0.05) state.airborneSeen = true;
        // A low ceiling may prevent takeoff entirely. Still require ground, after a short grace period.
        if (player.onGround() && player.tickCount > state.procTick + 1
                && (state.airborneSeen || player.tickCount - state.procTick >= BLOCKED_TAKEOFF_GRACE_TICKS)) {
            state.waitingForLanding = false;
            state.cooldownEnd = player.tickCount + GROUND_COOLDOWN_TICKS;
            sync(player, false);
        }
    }

    /** Player.attack resets its timer at the end, so apply this only after that reset. */
    public static void finishAttack(Player player) {
        State state = STATES.get(player);
        if (state == null) return;
        state.criticalAttack = null;
        if (!state.recoveryPending || player.level().isClientSide) return;
        state.recoveryPending = false;
        accelerateAttackRecovery(player);
        if (player instanceof ServerPlayer serverPlayer) sync(serverPlayer, true);
    }

    public static void accelerateAttackRecovery(Player player) {
        WindBurstAttackAccess access = (WindBurstAttackAccess) player;
        int recovered = Math.max(0, (int) Math.ceil(player.getCurrentItemAttackStrengthDelay()) - PROC_RECOVERY_TICKS);
        access.vr$setAttackStrengthTicker(Math.max(access.vr$getAttackStrengthTicker(), recovered));
    }

    private static void sync(ServerPlayer player, boolean recoverAttack) {
        if (player instanceof FakePlayer) return;
        State state = STATES.get(player);
        PacketDistributor.sendToPlayer(player, new SyncWindBurstPayload(
                state != null && state.waitingForLanding,
                state == null ? 0 : Math.max(0, state.cooldownEnd - player.tickCount), recoverAttack));
    }

    public static void receive(Player player, boolean waiting, int remaining, boolean recoverAttack) {
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        state.waitingForLanding = waiting;
        state.cooldownEnd = player.tickCount + remaining;
        if (recoverAttack && level(player.getMainHandItem(), player) > 0) accelerateAttackRecovery(player);
    }

    private WindBurst() {}
}
