package net.iann.vanillareenchanted.library;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LibraryScanner {

    // 5x5x5 cube means radius 2 from the table position.
    private static final int SCAN_RADIUS = 5;

    public static LibraryScanResult scan(
            Level level,
            BlockPos tablePos,
            List<Holder<Enchantment>> enchantmentsToCheck
    ) {
        LibraryScanResult result = new LibraryScanResult();

        BlockPos start = tablePos.offset(
                -SCAN_RADIUS,
                -SCAN_RADIUS,
                -SCAN_RADIUS
        );

        BlockPos end = tablePos.offset(
                SCAN_RADIUS,
                SCAN_RADIUS,
                SCAN_RADIUS
        );

        for (BlockPos currentPos : BlockPos.betweenClosed(start, end)) {
            BlockPos shelfPos = currentPos.immutable();

            if (shelfPos.equals(tablePos)) {
                continue;
            }

            if (!isValidShelf(level, shelfPos, tablePos)) {
                continue;
            }

            scanShelf(
                    level,
                    shelfPos,
                    enchantmentsToCheck,
                    result
            );
        }

        result.sortEntries();
        return result;
    }

    private static boolean isValidShelf(
            Level level,
            BlockPos shelfPos,
            BlockPos tablePos
    ) {
        BlockState shelfState = level.getBlockState(shelfPos);

        if (!shelfState.is(Blocks.CHISELED_BOOKSHELF)) {
            return false;
        }

        Direction shelfFacing = shelfState.getValue(HorizontalDirectionalBlock.FACING);

        if (!isShelfFacingTable(shelfPos, shelfFacing, tablePos)) {
            return false;
        }

        return isShelfFrontAccessible(level, shelfPos, shelfFacing);
    }

    private static boolean isShelfFacingTable(
            BlockPos shelfPos,
            Direction shelfFacing,
            BlockPos tablePos
    ) {
        BlockPos frontPos = shelfPos.relative(shelfFacing);

        int oldDistance = shelfPos.distManhattan(tablePos);
        int newDistance = frontPos.distManhattan(tablePos);

        // If moving out of the shelf's front face gets closer to the table,
        // then the shelf is facing generally toward the table.
        return newDistance < oldDistance;
    }

    private static boolean isShelfFrontAccessible(
            Level level,
            BlockPos shelfPos,
            Direction shelfFacing
    ) {
        BlockPos frontPos = shelfPos.relative(shelfFacing);
        BlockState frontState = level.getBlockState(frontPos);

        // If the block in front has a sturdy face against the shelf,
        // treat it as blocking access.
        return !frontState.isFaceSturdy(
                level,
                frontPos,
                shelfFacing.getOpposite()
        );
    }

    private static void scanShelf(
            Level level,
            BlockPos shelfPos,
            List<Holder<Enchantment>> enchantmentsToCheck,
            LibraryScanResult result
    ) {
        BlockEntity blockEntity = level.getBlockEntity(shelfPos);

        if (!(blockEntity instanceof Container container)) {
            return;
        }

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(Items.BOOK)) {
                result.addNormalBookSlot(
                        new LibraryBookSlot(
                                shelfPos,
                                slot,
                                stack.copy()
                        )
                );

                continue;
            }

            if (!stack.is(Items.ENCHANTED_BOOK)) {
                continue;
            }

            result.addEnchantedBookSlot(
                    new LibraryBookSlot(
                            shelfPos,
                            slot,
                            stack.copy()
                    )
            );

            ItemEnchantments storedEnchantments = stack.getOrDefault(
                    DataComponents.STORED_ENCHANTMENTS,
                    ItemEnchantments.EMPTY
            );

            for (Holder<Enchantment> enchantmentHolder : enchantmentsToCheck) {
                int levelOnBook = storedEnchantments.getLevel(enchantmentHolder);

                if (levelOnBook <= 0) {
                    continue;
                }

                ResourceLocation enchantmentId = getEnchantmentId(enchantmentHolder);

                if (enchantmentId == null) {
                    continue;
                }

                result.addKnowledgeEntry(
                        new LibraryKnowledgeEntry(
                                enchantmentId,
                                enchantmentHolder,
                                levelOnBook,
                                shelfPos,
                                slot
                        )
                );
            }
        }
    }

    private static ResourceLocation getEnchantmentId(
            Holder<Enchantment> enchantmentHolder
    ) {
        return enchantmentHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
    }

    public static class LibraryScanResult {
        private final List<LibraryBookSlot> normalBookSlots = new ArrayList<>();
        private final List<LibraryBookSlot> enchantedBookSlots = new ArrayList<>();
        private final List<LibraryKnowledgeEntry> knowledgeEntries = new ArrayList<>();

        public List<LibraryBookSlot> getNormalBookSlots() {
            return normalBookSlots;
        }

        public List<LibraryBookSlot> getEnchantedBookSlots() {
            return enchantedBookSlots;
        }

        public List<LibraryKnowledgeEntry> getKnowledgeEntries() {
            return knowledgeEntries;
        }

        private void addNormalBookSlot(LibraryBookSlot bookSlot) {
            this.normalBookSlots.add(bookSlot);
        }

        private void addEnchantedBookSlot(LibraryBookSlot bookSlot) {
            this.enchantedBookSlots.add(bookSlot);
        }

        private void addKnowledgeEntry(LibraryKnowledgeEntry entry) {
            this.knowledgeEntries.add(entry);
        }

        private void sortEntries() {
            this.knowledgeEntries.sort(
                    Comparator.comparing(entry -> entry.enchantmentId().toString())
            );
        }

        public int getMaxLevel(ResourceLocation enchantmentId) {
            int maxLevel = 0;

            for (LibraryKnowledgeEntry entry : this.knowledgeEntries) {
                if (!entry.enchantmentId().equals(enchantmentId)) {
                    continue;
                }

                maxLevel = Math.max(maxLevel, entry.level());
            }

            return maxLevel;
        }

        public LibraryKnowledgeEntry getHighestEntry(ResourceLocation enchantmentId) {
            LibraryKnowledgeEntry bestEntry = null;

            for (LibraryKnowledgeEntry entry : this.knowledgeEntries) {
                if (!entry.enchantmentId().equals(enchantmentId)) {
                    continue;
                }

                if (bestEntry == null || entry.level() > bestEntry.level()) {
                    bestEntry = entry;
                }
            }

            return bestEntry;
        }

        public LibraryBookSlot getFirstNormalBookSlot() {
            if (this.normalBookSlots.isEmpty()) {
                return null;
            }

            return this.normalBookSlots.get(0);
        }
    }

    public record LibraryBookSlot(
            BlockPos shelfPos,
            int slot,
            ItemStack stack
    ) {
    }

    public record LibraryKnowledgeEntry(
            ResourceLocation enchantmentId,
            Holder<Enchantment> enchantmentHolder,
            int level,
            BlockPos shelfPos,
            int slot
    ) {
    }
}