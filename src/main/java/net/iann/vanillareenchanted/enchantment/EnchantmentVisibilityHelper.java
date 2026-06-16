package net.iann.vanillareenchanted.enchantment;

import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EnchantmentVisibilityHelper {

    public static List<Holder<Enchantment>> getVisibleEnchantments(Player player, ItemStack filterStack) {
        Registry<Enchantment> enchantmentRegistry =
                player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);

        List<Holder<Enchantment>> visibleEnchantments = new ArrayList<>();

        for (Holder.Reference<Enchantment> enchantmentHolder : enchantmentRegistry.holders().toList()) {
            Optional<ResourceLocation> enchantmentId = getEnchantmentId(enchantmentHolder);

            if (enchantmentId.isEmpty()) {
                continue;
            }

            if (shouldHideTreasureEnchant(enchantmentHolder, knowledge, enchantmentId.get())) {
                continue;
            }

            if (!filterStack.isEmpty() && !canApplyToItem(enchantmentHolder, filterStack)) {
                continue;
            }

            visibleEnchantments.add(enchantmentHolder);
        }

        visibleEnchantments.sort(Comparator.comparing(holder ->
                getEnchantmentId(holder).map(ResourceLocation::toString).orElse("")
        ));

        return visibleEnchantments;
    }

    private static boolean shouldHideTreasureEnchant(
            Holder<Enchantment> enchantmentHolder,
            PlayerKnowledgeData knowledge,
            ResourceLocation enchantmentId
    ) {
        boolean isTreasure = enchantmentHolder.is(EnchantmentTags.TREASURE);

        if (!isTreasure) {
            return false;
        }

        return !knowledge.hasUnlocked(enchantmentId);
    }

    private static boolean canApplyToItem(Holder<Enchantment> enchantmentHolder, ItemStack stack) {
        return stack.supportsEnchantment(enchantmentHolder);
    }

    private static Optional<ResourceLocation> getEnchantmentId(Holder<Enchantment> enchantmentHolder) {
        return enchantmentHolder.unwrapKey().map(key -> key.location());
    }
}
