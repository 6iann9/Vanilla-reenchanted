package net.iann.vanillareenchanted.enchantment;

import java.util.HashSet;
import java.util.Set;
import net.iann.vanillareenchanted.registry.ModParticles;
import net.iann.vanillareenchanted.registry.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

public final class EchoingEdge {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "echoing_edge");
    public static final int MAX_CHARGES = 3;
    public static final int EXPIRY_TICKS = 20 * 15;
    private static final String TAG = "iannvanillareenchanted.EchoingEdge";
    private static final ThreadLocal<Attack> ATTACK = new ThreadLocal<>();

    public static final class Attack {
        public final Player player;
        public final Entity primary;
        public final ItemStack sword;
        public final int level, charges;
        public boolean critical, sweeping;
        public Entity activeTarget;
        public Vec3 impactPosition;
        public final Set<Entity> hits = new HashSet<>();
        private Attack(Player player, Entity primary) {
            this.player = player; this.primary = primary; this.sword = player.getMainHandItem();
            this.level = level(sword); this.charges = charges(sword, player.level().getGameTime());
        }
    }

    public static int level(ItemStack sword) {
        if (!sword.canPerformAction(ItemAbilities.SWORD_SWEEP)) return 0;
        return sword.getEnchantments().entrySet().stream()
                .filter(e -> e.getKey().unwrapKey().map(key -> key.location().equals(ID)).orElse(false))
                .mapToInt(e -> e.getIntValue()).max().orElse(0);
    }

    public static int charges(ItemStack sword, long time) {
        if (level(sword) <= 0) return 0;
        var state = sword.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(TAG);
        long last = state.getLong("LastHit");
        return !state.contains("LastHit") || time < last || time - last >= EXPIRY_TICKS
                ? 0 : Math.clamp(state.getInt("Charges"), 0, MAX_CHARGES);
    }

    public static void store(ItemStack sword, int charges, long time) {
        var data = sword.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (charges <= 0) data.remove(TAG);
        else {
            var state = new CompoundTag();
            state.putInt("Charges", Math.clamp(charges, 0, MAX_CHARGES));
            state.putLong("LastHit", time);
            data.put(TAG, state);
        }
        if (data.isEmpty()) sword.remove(DataComponents.CUSTOM_DATA);
        else sword.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static Attack current() { return ATTACK.get(); }

    public static void attack(Player player, Entity primary, Runnable action) {
        Attack previous = ATTACK.get();
        Attack attack = !player.level().isClientSide && level(player.getMainHandItem()) > 0 ? new Attack(player, primary) : null;
        if (attack == null) ATTACK.remove(); else ATTACK.set(attack);
        try { action.run(); }
        finally {
            try { if (attack != null) finish(attack); }
            finally { if (previous == null) ATTACK.remove(); else ATTACK.set(previous); }
        }
    }

    public static float beginHit(Entity target, float damage) {
        Attack attack = current();
        if (attack == null) return damage;
        attack.activeTarget = target;
        if (target == attack.primary) {
            // Capture contact before damage/knockback changes the target's position.
            Vec3 eye = attack.player.getEyePosition();
            var bounds = target.getBoundingBox();
            Vec3 center = bounds.getCenter();
            double rayLength = eye.distanceTo(center) + target.getBbWidth() + target.getBbHeight() + 1;
            Vec3 end = eye.add(attack.player.getLookAngle().scale(rayLength));
            Vec3 contact = bounds.clip(eye, end)
                    .orElseGet(() -> bounds.clip(eye, center).orElse(center));
            // Keep the ring just outside the hit surface so the body does not hide it.
            attack.impactPosition = contact.add(eye.subtract(contact).normalize().scale(0.06));
        }
        return target == attack.primary && attack.critical
                ? damage * (1 + attack.charges * 0.05F * (Math.clamp(attack.level, 1, 3) + 1)) : damage;
    }

    private static void finish(Attack attack) {
        if (!(attack.player instanceof ServerPlayer player) || attack.hits.isEmpty()) return;
        long time = player.level().getGameTime();
        if (attack.critical && attack.charges > 0 && attack.hits.contains(attack.primary)) {
            store(attack.sword, 0, time);
            var pos = attack.impactPosition;
            player.serverLevel().sendParticles(ModParticles.ECHO_IMPACT_RING.get(), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            player.serverLevel().playSound(null, pos.x, pos.y, pos.z, ModSounds.ECHOING_EDGE_IMPACT.get(),
                    player.getSoundSource(), 0.7F + 0.1F * attack.charges, 1);
        } else {
            int charges = Math.min(MAX_CHARGES, attack.charges + (attack.sweeping && !attack.critical ? attack.hits.size() : 0));
            if (charges > 0) store(attack.sword, charges, time);
        }
    }

    private EchoingEdge() {}
}
