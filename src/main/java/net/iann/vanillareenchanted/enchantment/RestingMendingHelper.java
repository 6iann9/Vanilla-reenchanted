package net.iann.vanillareenchanted.enchantment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

/** Saved display timers allow unloaded-chunk repair without crediting time in inventories. */
public final class RestingMendingHelper {
    public static final int CHECK_INTERVAL_TICKS = 20;
    public static final int FULL_REPAIR_TICKS = 20 * 60 * 60 * 3;
    private static final String TAG = "iannvanillareenchanted.RestingMending.";

    public static boolean isRepairing(ItemStack stack) {
        return stack.isDamageableItem() && stack.isDamaged()
                && stack.getEnchantments().keySet().stream().anyMatch(holder -> holder.is(Enchantments.MENDING));
    }

    /** A newly inserted item starts now, even if it previously rested in this same display. */
    public static void beginResting(Entity display, String slot, ItemStack stack) {
        if (display.level().isClientSide) return;
        display.getPersistentData().remove(TAG + slot);
        updateRestingMending(display, slot, stack, display.level().getGameTime());
    }

    /** Returns true only when durability changed. Entity persistent data survives chunk unloading. */
    public static boolean updateRestingMending(Entity display, String slot, ItemStack stack, long time) {
        if (display.level().isClientSide) return false;
        var data = display.getPersistentData();
        String key = TAG + slot;
        if (!isRepairing(stack)) {
            data.remove(key);
            return false;
        }
        var progress = data.getCompound(key);
        long last = progress.getLong("LastCheck");
        if (!progress.contains("LastCheck") || last < 0 || last > time
                || progress.getInt("Damage") != stack.getDamageValue()
                || progress.contains("MaxDamage") && progress.getInt("MaxDamage") != stack.getMaxDamage()) {
            remember(display, key, time, stack);
            return false;
        }
        long elapsed = time - last;
        // Bound elapsed before multiplication: one full repair period already repairs any item.
        // Integer remainder stores exact fractional durability across checks and save/reloads.
        long fraction = Math.clamp(progress.getLong("Fraction"), 0L, FULL_REPAIR_TICKS - 1L);
        long earned = Math.min(elapsed, FULL_REPAIR_TICKS) * stack.getMaxDamage() + fraction;
        int repair = (int) Math.min(stack.getDamageValue(), earned / FULL_REPAIR_TICKS);
        if (repair > 0) stack.setDamageValue(stack.getDamageValue() - repair);
        if (!stack.isDamaged()) {
            data.remove(key); // Full items cannot bank time against future damage.
        } else {
            progress.putLong("LastCheck", time);
            progress.putLong("Fraction", earned % FULL_REPAIR_TICKS);
            progress.putInt("Damage", stack.getDamageValue());
            progress.putInt("MaxDamage", stack.getMaxDamage());
        }
        return repair > 0;
    }

    private static void remember(Entity display, String key, long time, ItemStack stack) {
        var progress = new CompoundTag();
        progress.putLong("LastCheck", time);
        progress.putInt("Damage", stack.getDamageValue());
        progress.putInt("MaxDamage", stack.getMaxDamage());
        display.getPersistentData().put(key, progress);
    }

    private RestingMendingHelper() {}
}
