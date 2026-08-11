package net.iann.vanillareenchanted.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class EnchantedBookResearchHelper {

    public static boolean isTreasureEnchantment(
            Holder<Enchantment> enchantmentHolder
    ) {
        return enchantmentHolder.is(EnchantmentTags.TREASURE);
    }

    public static int getBookEnchantmentLevel(
            ItemStack stack,
            Holder<Enchantment> enchantmentHolder
    ) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return 0;
        }

        ItemEnchantments storedEnchantments = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );

        return storedEnchantments.getLevel(enchantmentHolder);
    }

    public static boolean hasBookForResearchLevel(
            ItemStack stack,
            Holder<Enchantment> enchantmentHolder,
            int targetLevel
    ) {
        return getBookEnchantmentLevel(stack, enchantmentHolder) >= targetLevel;
    }
}