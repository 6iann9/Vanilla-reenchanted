package net.iann.vanillareenchanted.enchantment;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;

/** The charges belong to the retrieved trident, with a fixed expiry from retrieval. */
public final class LoyaltyReturn {
    public interface HitTracking { boolean vr$loyaltyHit(); }
    public static final int MAX_CHARGES = 3, DURATION_TICKS = 15 * 20;
    private static final String TAG = "iannvanillareenchanted.LoyaltyReturn";

    public static int level(ItemStack stack) {
        if (!(stack.getItem() instanceof TridentItem)) return 0;
        return stack.getEnchantments().entrySet().stream()
                .filter(e -> e.getKey().is(Enchantments.LOYALTY))
                .mapToInt(e -> e.getIntValue()).max().orElse(0);
    }
    public static int charges(ItemStack stack, long time) {
        if (level(stack) <= 0) return 0;
        var state = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(TAG);
        long retrieved = state.getLong("Retrieved");
        return !state.contains("Retrieved") || time < retrieved || time - retrieved >= DURATION_TICKS
                ? 0 : Math.clamp(state.getInt("Charges"), 0, MAX_CHARGES);
    }
    public static float multiplier(ItemStack stack) {
        return 1.0F + 0.1F * Math.clamp(level(stack), 0, 3);
    }
    public static void retrieve(ThrownTrident projectile, Player player, ItemStack stack) {
        if (player.level().isClientSide || projectile.getOwner() != player || level(stack) <= 0
                || !((HitTracking)projectile).vr$loyaltyHit()) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var state = new CompoundTag();
        state.putInt("Charges", MAX_CHARGES);
        state.putLong("Retrieved", player.level().getGameTime());
        data.put(TAG, state);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
    public static void consume(ItemStack stack, long time) {
        int remaining = charges(stack, time) - 1;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (remaining <= 0) data.remove(TAG);
        else data.getCompound(TAG).putInt("Charges", remaining);
        // Do not update Retrieved: hits must not extend the 15-second window.
        if (data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
    private LoyaltyReturn() {}
}
