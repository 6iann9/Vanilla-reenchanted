package net.iann.vanillareenchanted.enchantment;

import net.iann.vanillareenchanted.network.SyncWindUpCooldownPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import com.google.common.collect.MapMaker;
import java.util.Map;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

public final class WindUp {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "wind_up");
    public static final int CHARGE_TICKS = 25;
    public static final int USE_DURATION = 72000;
    public static final int COOLDOWN_TICKS = 15;
    public static final int EQUIP_COOLDOWN_TICKS = 15;
    private record Cooldown(int endTick, int duration) {}
    // Entity.equals compares numeric IDs, shared by client/server copies in single-player.
    // Weak identity keys isolate those instances and release state when players unload.
    private static final Map<Player, Cooldown> COOLDOWNS =
            new MapMaker().weakKeys().makeMap();

    public static void startCooldown(Player player, int ticks) {
        applyCooldown(player, ticks, ticks);
        syncCooldown(player);
    }

    public static void applyCooldown(Player player, int remaining, int duration) {
        if (remaining <= 0) COOLDOWNS.remove(player);
        else COOLDOWNS.put(player, new Cooldown(player.tickCount + remaining, Math.max(remaining, duration)));
    }

    public static int remainingCooldown(Player player) {
        Cooldown cooldown = COOLDOWNS.get(player);
        return cooldown == null ? 0 : Math.max(0, cooldown.endTick - player.tickCount);
    }

    public static boolean isCoolingDown(Player player) {
        return remainingCooldown(player) > 0;
    }

    public static float cooldownProgress(Player player, float partialTick) {
        Cooldown cooldown = COOLDOWNS.get(player);
        if (cooldown == null) return 1;
        return Math.clamp(1F - (cooldown.endTick - player.tickCount - partialTick) / cooldown.duration, 0F, 1F);
    }

    /** Send only changes/corrections, not a packet every tick. */
    public static void syncCooldown(Player player) {
        if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
            Cooldown cooldown = COOLDOWNS.get(player);
            PacketDistributor.sendToPlayer(serverPlayer, new SyncWindUpCooldownPayload(
                    remainingCooldown(player), cooldown == null ? 0 : cooldown.duration));
        }
    }

    private record Selection(int slot, Item item, int enchantmentLevel) {}
    private static final Map<Player, Selection> SELECTIONS =
            new MapMaker().weakKeys().makeMap();

    public static void checkEquipCooldown(Player player) {
        ItemStack stack = player.getMainHandItem();
        Selection current = new Selection(player.getInventory().selected, stack.getItem(), level(stack));
        Selection previous = SELECTIONS.put(player, current);
        if (!current.equals(previous) && current.enchantmentLevel() > 0) {
            startCooldown(player, EQUIP_COOLDOWN_TICKS);
        }
    }

    private record Hit(LivingEntity attacker, float bonus) {}
    private static final ThreadLocal<Hit> HIT = new ThreadLocal<>();

    public static int level(ItemStack stack) {
        if (!(stack.getItem() instanceof MaceItem)) return 0;
        return stack.getEnchantments().entrySet().stream()
                .filter(e -> e.getKey().unwrapKey().map(k -> k.location().equals(ID)).orElse(false))
                .mapToInt(e -> e.getIntValue()).max().orElse(0);
    }
    public static float bonusHeight(int level, long ticks) {
        return 2F * Math.min(3, Math.max(0, level)) * Math.min(1F, Math.max(0L, ticks) / (float) CHARGE_TICKS);
    }
    public static boolean isReleasing(LivingEntity entity) {
        Hit hit = HIT.get();
        return hit != null && hit.attacker == entity;
    }
    public static float effectiveFallDistance(LivingEntity entity) {
        Hit hit = HIT.get();
        return entity.fallDistance + (hit != null && hit.attacker == entity ? hit.bonus : 0);
    }
    public static void attack(LivingEntity entity, float bonus, Runnable attack) {
        Hit previous = HIT.get();
        HIT.set(new Hit(entity, bonus));
        try { attack.run(); }
        finally { if (previous == null) HIT.remove(); else HIT.set(previous); }
    }
    private WindUp() {}
}
