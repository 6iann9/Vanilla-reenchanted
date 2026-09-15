package net.iann.vanillareenchanted.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Optional;

public class RestingMendingHelper {

    private static final ResourceLocation MENDING_ID =
            ResourceLocation.withDefaultNamespace("mending");

    private static final String TAG_ROOT = "VanillaReenchanted";
    private static final String TAG_LAST_MENDING_CHECK = "LastRestingMendingCheck";

    private static final int TICKS_PER_REPAIR = 20;
    private static final long MAX_CATCH_UP_TICKS = 24000L * 3L;

    public static void updateRestingMending(
            ItemStack stack,
            long currentGameTime
    ) {
        if (stack.isEmpty()) {
            return;
        }

        if (!stack.isDamaged()) {
            rememberCheckTime(stack, currentGameTime);
            return;
        }

        if (!hasMending(stack)) {
            clearCheckTime(stack);
            return;
        }

        long lastCheckTime = getLastCheckTime(stack);

        if (lastCheckTime <= 0L || lastCheckTime > currentGameTime) {
            rememberCheckTime(stack, currentGameTime);
            return;
        }

        long elapsedTicks = currentGameTime - lastCheckTime;

        if (elapsedTicks <= 0L) {
            return;
        }

        elapsedTicks = Math.min(elapsedTicks, MAX_CATCH_UP_TICKS);

        int repairAmount = (int) (elapsedTicks / TICKS_PER_REPAIR);

        if (repairAmount <= 0) {
            return;
        }

        int currentDamage = stack.getDamageValue();
        int newDamage = Math.max(0, currentDamage - repairAmount);

        stack.setDamageValue(newDamage);

        long usedTicks = (long) repairAmount * TICKS_PER_REPAIR;

        rememberCheckTime(
                stack,
                currentGameTime - Math.max(0L, elapsedTicks - usedTicks)
        );
    }

    private static boolean hasMending(ItemStack stack) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack)
                .entrySet()
                .stream()
                .anyMatch(entry -> {
                    Holder<Enchantment> holder = entry.getKey();

                    Optional<ResourceLocation> enchantmentId =
                            holder.unwrapKey().map(key -> key.location());

                    return enchantmentId
                            .map(MENDING_ID::equals)
                            .orElse(false);
                });
    }

    private static long getLastCheckTime(ItemStack stack) {
        CompoundTag customData = stack.getOrDefault(
                DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY
        ).copyTag();

        if (!customData.contains(TAG_ROOT)) {
            return 0L;
        }

        CompoundTag modTag = customData.getCompound(TAG_ROOT);

        return modTag.getLong(TAG_LAST_MENDING_CHECK);
    }

    private static void rememberCheckTime(
            ItemStack stack,
            long currentGameTime
    ) {
        CompoundTag customData = stack.getOrDefault(
                DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY
        ).copyTag();

        CompoundTag modTag = customData.getCompound(TAG_ROOT);

        modTag.putLong(TAG_LAST_MENDING_CHECK, currentGameTime);
        customData.put(TAG_ROOT, modTag);

        stack.set(
                DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(customData)
        );
    }

    private static void clearCheckTime(ItemStack stack) {
        CompoundTag customData = stack.getOrDefault(
                DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY
        ).copyTag();

        if (!customData.contains(TAG_ROOT)) {
            return;
        }

        CompoundTag modTag = customData.getCompound(TAG_ROOT);
        modTag.remove(TAG_LAST_MENDING_CHECK);

        if (modTag.isEmpty()) {
            customData.remove(TAG_ROOT);
        } else {
            customData.put(TAG_ROOT, modTag);
        }

        stack.set(
                DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(customData)
        );
    }
}