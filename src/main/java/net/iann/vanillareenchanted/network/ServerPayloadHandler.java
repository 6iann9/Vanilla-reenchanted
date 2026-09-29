package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.enchantment.RestrictedEnchantments;

import net.iann.vanillareenchanted.cost.EnchantmentCostCalculator;
import net.iann.vanillareenchanted.cost.EnvironmentCostModifier;
import net.iann.vanillareenchanted.enchantment.DuplicateBookDiscountHelper;
import net.iann.vanillareenchanted.library.LibraryScanner;
import net.iann.vanillareenchanted.menu.ResearchTableMenu;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public class ServerPayloadHandler {

    public static void handleResearchEnchantment(
            ResearchEnchantmentPayload payload,
            IPayloadContext context
    ) {
        Player player = context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!(serverPlayer.containerMenu instanceof ResearchTableMenu researchTableMenu)) {
            return;
        }

        ResourceLocation enchantmentId = payload.enchantmentId();
        if (RestrictedEnchantments.isRestricted(enchantmentId)) return;

        Registry<Enchantment> enchantmentRegistry =
                serverPlayer.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        ResourceKey<Enchantment> enchantmentKey =
                ResourceKey.create(Registries.ENCHANTMENT, enchantmentId);

        Optional<Holder.Reference<Enchantment>> optionalEnchantment =
                enchantmentRegistry.getHolder(enchantmentKey);

        if (optionalEnchantment.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Unknown enchantment: " + enchantmentId)
            );

            return;
        }

        Holder<Enchantment> enchantmentHolder = optionalEnchantment.get();

        LibraryScanner.LibraryScanResult scanResult =
                researchTableMenu.scanLibrary();

        int currentLevel = scanResult.getMaxLevel(enchantmentId);
        int maxLevel = Math.max(1, enchantmentHolder.value().getMaxLevel());

        if (currentLevel >= maxLevel) {
            serverPlayer.sendSystemMessage(
                    Component.literal("This enchantment is already fully researched in the library.")
            );

            return;
        }

        boolean isTreasure = enchantmentHolder.is(EnchantmentTags.TREASURE);

        if (currentLevel <= 0 && isTreasure) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Treasure enchantments must be found before they can be upgraded.")
            );

            return;
        }

        int targetLevel = currentLevel + 1;

        boolean hasBookTarget;

        if (targetLevel == 1) {
            hasBookTarget = scanResult.getFirstNormalBookSlot() != null;

            if (!hasBookTarget) {
                serverPlayer.sendSystemMessage(
                        Component.literal("Place a normal book in a valid chiseled bookshelf first.")
                );

                return;
            }
        } else {
            hasBookTarget = scanResult.getHighestEntry(enchantmentId) != null;

            if (!hasBookTarget) {
                serverPlayer.sendSystemMessage(
                        Component.literal("The previous level must exist in the library first.")
                );

                return;
            }
        }

        int baseXpCost = EnchantmentCostCalculator.getResearchXpCost(
                enchantmentHolder,
                targetLevel
        );

        ItemStack duplicateBook = researchTableMenu.getDuplicateBookItem();

        int costAfterDuplicateBook = DuplicateBookDiscountHelper.getDiscountedResearchXpLevelCost(
                baseXpCost,
                targetLevel,
                duplicateBook,
                enchantmentHolder
        );

        int xpCost = EnvironmentCostModifier.applyCandleResearchDiscount(
                serverPlayer.level(),
                researchTableMenu.getTablePos(),
                costAfterDuplicateBook
        );

        boolean shouldConsumeDuplicateBook =
                DuplicateBookDiscountHelper.hasMatchingDuplicateBook(
                        duplicateBook,
                        enchantmentHolder
                );

        if (!serverPlayer.isCreative()
                && xpCost > 0
                && serverPlayer.experienceLevel < xpCost) {

            serverPlayer.sendSystemMessage(
                    Component.literal("Not enough XP levels. Need " + xpCost + ".")
            );

            return;
        }

        boolean updatedBook;

        if (targetLevel == 1) {
            LibraryScanner.LibraryBookSlot normalBookSlot =
                    scanResult.getFirstNormalBookSlot();

            updatedBook = convertNormalBookToEnchantedBook(
                    serverPlayer.level(),
                    normalBookSlot,
                    enchantmentHolder,
                    targetLevel
            );
        } else {
            LibraryScanner.LibraryKnowledgeEntry highestEntry =
                    scanResult.getHighestEntry(enchantmentId);

            updatedBook = upgradeExistingLibraryBook(
                    serverPlayer.level(),
                    highestEntry,
                    enchantmentHolder,
                    targetLevel
            );
        }

        if (!updatedBook) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Could not update the library book.")
            );

            return;
        }

        if (!serverPlayer.isCreative()) {
            if (xpCost > 0) {
                serverPlayer.giveExperienceLevels(-xpCost);
            }

            if (shouldConsumeDuplicateBook) {
                researchTableMenu.consumePaymentBook();
            }
        }

        researchTableMenu.refreshVisibleEnchantments();
        researchTableMenu.broadcastChanges();

        LibraryKnowledgeSync.sendToClient(
                serverPlayer,
                researchTableMenu
        );

        serverPlayer.playNotifySound(
                SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                SoundSource.PLAYERS,
                0.8F,
                1.2F
        );

        if (targetLevel == 1) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Created " + enchantmentId + " level " + targetLevel + " in the library.")
            );
        } else {
            serverPlayer.sendSystemMessage(
                    Component.literal("Upgraded " + enchantmentId + " to level " + targetLevel + " in the library.")
            );
        }
    }

    public static void handleEnchantItem(
            EnchantItemPayload payload,
            IPayloadContext context
    ) {
        Player player = context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!(serverPlayer.containerMenu instanceof ResearchTableMenu researchTableMenu)) {
            return;
        }

        ResourceLocation enchantmentId = payload.enchantmentId();
        if (RestrictedEnchantments.isRestricted(enchantmentId)) return;

        Registry<Enchantment> enchantmentRegistry =
                serverPlayer.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        ResourceKey<Enchantment> enchantmentKey =
                ResourceKey.create(Registries.ENCHANTMENT, enchantmentId);

        Optional<Holder.Reference<Enchantment>> optionalEnchantment =
                enchantmentRegistry.getHolder(enchantmentKey);

        if (optionalEnchantment.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Unknown enchantment: " + enchantmentId)
            );

            return;
        }

        Holder<Enchantment> enchantmentHolder = optionalEnchantment.get();

        ItemStack researchItem = researchTableMenu.getResearchItem();

        if (researchItem.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Insert an item first.")
            );

            return;
        }

        if (!researchItem.supportsEnchantment(enchantmentHolder)) {
            serverPlayer.sendSystemMessage(
                    Component.literal("This enchantment cannot be applied to this item.")
            );

            return;
        }

        if (hasIncompatibleEnchantment(researchItem, enchantmentHolder)) {
            serverPlayer.sendSystemMessage(
                    Component.literal("This item already has an incompatible enchantment.")
            );

            return;
        }

        int libraryLevel =
                researchTableMenu.scanLibrary().getMaxLevel(enchantmentId);

        int itemLevel = researchItem.getEnchantmentLevel(enchantmentHolder);
        int maxLevel = Math.max(1, enchantmentHolder.value().getMaxLevel());

        if (itemLevel >= maxLevel) {
            serverPlayer.sendSystemMessage(
                    Component.literal("This item already has the maximum level.")
            );

            return;
        }

        if (libraryLevel <= itemLevel) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Your library does not know a high enough level.")
            );

            return;
        }

        int targetLevel = itemLevel + 1;

        int baseLapisCost = EnchantmentCostCalculator.getEnchantLapisCost(
                enchantmentHolder,
                targetLevel
        );

        ItemStack duplicateBook = researchTableMenu.getDuplicateBookItem();

        int costAfterDuplicateBook = DuplicateBookDiscountHelper.getDiscountedEnchantLapisCost(
                baseLapisCost,
                targetLevel,
                duplicateBook,
                enchantmentHolder
        );

        int lapisCost = EnvironmentCostModifier.applyMobHeadEnchantDiscount(
                serverPlayer.level(),
                researchTableMenu.getTablePos(),
                costAfterDuplicateBook
        );

        boolean shouldConsumeDuplicateBook =
                DuplicateBookDiscountHelper.hasMatchingDuplicateBook(
                        duplicateBook,
                        enchantmentHolder
                );

        if (!serverPlayer.isCreative() && lapisCost > 0) {
            boolean consumed = researchTableMenu.consumeLapis(lapisCost);

            if (!consumed) {
                serverPlayer.sendSystemMessage(
                        Component.literal("Not enough lapis. Need " + lapisCost + ".")
                );

                return;
            }
        }

        setEnchantmentLevel(
                researchItem,
                enchantmentHolder,
                targetLevel
        );

        researchTableMenu.markResearchItemChanged();

        if (!serverPlayer.isCreative() && shouldConsumeDuplicateBook) {
            researchTableMenu.consumePaymentBook();
        }

        serverPlayer.playNotifySound(
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        serverPlayer.sendSystemMessage(
                Component.literal("Applied " + enchantmentId + " level " + targetLevel)
        );
    }

    private static boolean convertNormalBookToEnchantedBook(
            Level level,
            LibraryScanner.LibraryBookSlot normalBookSlot,
            Holder<Enchantment> enchantmentHolder,
            int targetLevel
    ) {
        BlockEntity blockEntity = level.getBlockEntity(normalBookSlot.shelfPos());

        if (!(blockEntity instanceof Container container)) {
            return false;
        }

        ItemStack currentStack = container.getItem(normalBookSlot.slot());

        if (!currentStack.is(Items.BOOK)) {
            return false;
        }

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);

        setStoredBookEnchantmentLevel(
                enchantedBook,
                enchantmentHolder,
                targetLevel
        );

        container.setItem(
                normalBookSlot.slot(),
                enchantedBook
        );

        markShelfChanged(
                level,
                normalBookSlot.shelfPos(),
                container
        );

        return true;
    }

    private static boolean upgradeExistingLibraryBook(
            Level level,
            LibraryScanner.LibraryKnowledgeEntry knowledgeEntry,
            Holder<Enchantment> enchantmentHolder,
            int targetLevel
    ) {
        BlockEntity blockEntity = level.getBlockEntity(knowledgeEntry.shelfPos());

        if (!(blockEntity instanceof Container container)) {
            return false;
        }

        ItemStack currentStack = container.getItem(knowledgeEntry.slot());

        if (!currentStack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }

        ItemStack updatedBook = currentStack.copy();

        setStoredBookEnchantmentLevel(
                updatedBook,
                enchantmentHolder,
                targetLevel
        );

        container.setItem(
                knowledgeEntry.slot(),
                updatedBook
        );

        markShelfChanged(
                level,
                knowledgeEntry.shelfPos(),
                container
        );

        return true;
    }

    private static void setStoredBookEnchantmentLevel(
            ItemStack bookStack,
            Holder<Enchantment> enchantmentHolder,
            int level
    ) {
        ItemEnchantments storedEnchantments = bookStack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );

        ItemEnchantments.Mutable mutableEnchantments =
                new ItemEnchantments.Mutable(storedEnchantments);

        mutableEnchantments.set(
                enchantmentHolder,
                level
        );

        bookStack.set(
                DataComponents.STORED_ENCHANTMENTS,
                mutableEnchantments.toImmutable()
        );
    }

    private static void markShelfChanged(
            Level level,
            net.minecraft.core.BlockPos shelfPos,
            Container container
    ) {
        container.setChanged();

        BlockState blockState = level.getBlockState(shelfPos);

        level.sendBlockUpdated(
                shelfPos,
                blockState,
                blockState,
                3
        );
    }

    private static boolean hasIncompatibleEnchantment(
            ItemStack stack,
            Holder<Enchantment> newEnchantment
    ) {
        ItemEnchantments existingEnchantments =
                EnchantmentHelper.getEnchantmentsForCrafting(stack);

        for (Holder<Enchantment> existingEnchantment : existingEnchantments.keySet()) {
            if (existingEnchantment.equals(newEnchantment)) {
                continue;
            }

            if (!Enchantment.areCompatible(existingEnchantment, newEnchantment)) {
                return true;
            }
        }

        return false;
    }

    private static void setEnchantmentLevel(
            ItemStack stack,
            Holder<Enchantment> enchantmentHolder,
            int level
    ) {
        ItemEnchantments.Mutable mutableEnchantments =
                new ItemEnchantments.Mutable(
                        EnchantmentHelper.getEnchantmentsForCrafting(stack)
                );

        mutableEnchantments.set(
                enchantmentHolder,
                level
        );

        EnchantmentHelper.setEnchantments(
                stack,
                mutableEnchantments.toImmutable()
        );
    }
}