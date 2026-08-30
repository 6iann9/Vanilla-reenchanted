package net.iann.vanillareenchanted.enchantment;

import net.iann.vanillareenchanted.cost.EnchantmentCostCalculator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class DuplicateBookDiscountHelper {

    public static int getDuplicateBookLevel(
            ItemStack duplicateBook,
            Holder<Enchantment> enchantmentHolder
    ) {
        if (!duplicateBook.is(Items.ENCHANTED_BOOK)) {
            return 0;
        }

        ItemEnchantments storedEnchantments = duplicateBook.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );

        return storedEnchantments.getLevel(enchantmentHolder);
    }

    public static boolean hasMatchingDuplicateBook(
            ItemStack duplicateBook,
            Holder<Enchantment> enchantmentHolder
    ) {
        return getDuplicateBookLevel(
                duplicateBook,
                enchantmentHolder
        ) > 0;
    }

    public static int getDiscountedResearchXpLevelCost(
            int baseLevelCost,
            int targetLevel,
            ItemStack duplicateBook,
            Holder<Enchantment> enchantmentHolder
    ) {
        int duplicateBookLevel = getDuplicateBookLevel(
                duplicateBook,
                enchantmentHolder
        );

        if (duplicateBookLevel <= 0) {
            return baseLevelCost;
        }

        if (duplicateBookLevel >= targetLevel) {
            return 0;
        }

        long baseRawXpCost = getTotalXpForLevel(baseLevelCost);

        long duplicateRawXpWorth = getCumulativeResearchRawXpWorth(
                enchantmentHolder,
                duplicateBookLevel
        );

        long targetRawXpWorth = getCumulativeResearchRawXpWorth(
                enchantmentHolder,
                targetLevel
        );

        if (targetRawXpWorth <= 0) {
            return baseLevelCost;
        }

        long discountRawXp = baseRawXpCost * duplicateRawXpWorth / targetRawXpWorth;

        long finalRawXpCost = Math.max(
                0,
                baseRawXpCost - discountRawXp
        );

        if (finalRawXpCost <= 0) {
            return 0;
        }

        return Math.max(
                1,
                getLevelCostForRawXpRoundedUp(finalRawXpCost)
        );
    }

    public static int getDiscountedEnchantLapisCost(
            int baseLapisCost,
            int targetLevel,
            ItemStack duplicateBook,
            Holder<Enchantment> enchantmentHolder
    ) {
        int duplicateBookLevel = getDuplicateBookLevel(
                duplicateBook,
                enchantmentHolder
        );

        if (duplicateBookLevel <= 0) {
            return baseLapisCost;
        }

        if (duplicateBookLevel >= targetLevel) {
            return 0;
        }

        int duplicateLapisWorth = getCumulativeEnchantLapisWorth(
                enchantmentHolder,
                duplicateBookLevel
        );

        int targetLapisWorth = getCumulativeEnchantLapisWorth(
                enchantmentHolder,
                targetLevel
        );

        if (targetLapisWorth <= 0) {
            return baseLapisCost;
        }

        int discount = baseLapisCost * duplicateLapisWorth / targetLapisWorth;

        int finalCost = baseLapisCost - discount;

        return Math.max(
                1,
                finalCost
        );
    }

    private static long getCumulativeResearchRawXpWorth(
            Holder<Enchantment> enchantmentHolder,
            int level
    ) {
        long totalRawXpWorth = 0;

        for (int currentLevel = 1; currentLevel <= level; currentLevel++) {
            int levelCost = EnchantmentCostCalculator.getResearchXpCost(
                    enchantmentHolder,
                    currentLevel
            );

            totalRawXpWorth += getTotalXpForLevel(levelCost);
        }

        return totalRawXpWorth;
    }

    private static int getCumulativeEnchantLapisWorth(
            Holder<Enchantment> enchantmentHolder,
            int level
    ) {
        int totalLapisWorth = 0;

        for (int currentLevel = 1; currentLevel <= level; currentLevel++) {
            totalLapisWorth += EnchantmentCostCalculator.getEnchantLapisCost(
                    enchantmentHolder,
                    currentLevel
            );
        }

        return totalLapisWorth;
    }

    private static long getTotalXpForLevel(int level) {
        if (level <= 0) {
            return 0;
        }

        if (level <= 16) {
            return (long) level * level + 6L * level;
        }

        if (level <= 31) {
            return Math.round(
                    2.5D * level * level
                            - 40.5D * level
                            + 360.0D
            );
        }

        return Math.round(
                4.5D * level * level
                        - 162.5D * level
                        + 2220.0D
        );
    }

    private static int getLevelCostForRawXpRoundedUp(long rawXp) {
        if (rawXp <= 0) {
            return 0;
        }

        int level = 0;

        while (getTotalXpForLevel(level) < rawXp) {
            level++;
        }

        return level;
    }
}