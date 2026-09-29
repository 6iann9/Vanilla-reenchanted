package net.iann.vanillareenchanted.loot;


import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.iann.vanillareenchanted.registry.ModLootModifiers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.ArrayList;
import java.util.List;

public class NormalizeEnchantedBooksLootModifier extends LootModifier {

    public static final MapCodec<NormalizeEnchantedBooksLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    LootModifier.codecStart(instance).apply(
                            instance,
                            NormalizeEnchantedBooksLootModifier::new
                    )
            );

    public NormalizeEnchantedBooksLootModifier(
            LootItemCondition[] conditions
    ) {
        super(conditions);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.NORMALIZE_ENCHANTED_BOOKS.get();
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        RandomSource random = context.getRandom();

        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);

            if (!stack.is(Items.ENCHANTED_BOOK)) {
                continue;
            }

            ItemStack normalizedBook = normalizeBook(stack, random);

            generatedLoot.set(i, normalizedBook);
        }

        return generatedLoot;
    }

    private ItemStack normalizeBook(
            ItemStack originalBook,
            RandomSource random
    ) {
        ItemEnchantments storedEnchantments = originalBook.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );

        if (storedEnchantments.entrySet().size() <= 1) {
            return originalBook;
        }

        List<EnchantmentEntry> allEntries = new ArrayList<>();
        List<EnchantmentEntry> treasureEntries = new ArrayList<>();

        for (var entry : storedEnchantments.entrySet()) {
            Holder<Enchantment> enchantmentHolder = entry.getKey();
            int level = entry.getIntValue();

            EnchantmentEntry enchantmentEntry = new EnchantmentEntry(
                    enchantmentHolder,
                    level
            );

            allEntries.add(enchantmentEntry);

            if (enchantmentHolder.is(EnchantmentTags.TREASURE)) {
                treasureEntries.add(enchantmentEntry);
            }
        }

        List<EnchantmentEntry> choices =
                treasureEntries.isEmpty()
                        ? allEntries
                        : treasureEntries;

        EnchantmentEntry chosenEntry = choices.get(
                random.nextInt(choices.size())
        );

        ItemStack normalizedBook = originalBook.copy();

        ItemEnchantments.Mutable mutableEnchantments =
                new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

        mutableEnchantments.set(
                chosenEntry.enchantmentHolder(),
                chosenEntry.level()
        );

        normalizedBook.set(
                DataComponents.STORED_ENCHANTMENTS,
                mutableEnchantments.toImmutable()
        );

        return normalizedBook;
    }

    private record EnchantmentEntry(
            Holder<Enchantment> enchantmentHolder,
            int level
    ) {
    }
}