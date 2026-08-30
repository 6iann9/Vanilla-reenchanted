package net.iann.vanillareenchanted.menu;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.library.LibraryScanner;
import net.iann.vanillareenchanted.network.LibraryKnowledgeSync;
import net.iann.vanillareenchanted.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResearchTableMenu extends AbstractContainerMenu {

    private static final int ITEM_SLOT_X = 72;
    private static final int ITEM_SLOT_Y = 19;

    private static final int LAPIS_SLOT_X = 170;
    private static final int LAPIS_SLOT_Y = 19;

    private static final int DUPLICATE_BOOK_SLOT_X = 210;
    private static final int DUPLICATE_BOOK_SLOT_Y = 19;

    private static final int INVENTORY_START_X = 59;
    private static final int INVENTORY_START_Y = 196;

    private static final int HOTBAR_START_X = 59;
    private static final int HOTBAR_START_Y = 254;

    private static final int LEFT_ARMOR_X = 37;
    private static final int RIGHT_ARMOR_X = 225;
    private static final int ARMOR_TOP_Y = 196;
    private static final int ARMOR_BOTTOM_Y = 232;

    private List<Holder<Enchantment>> visibleEnchantments = new ArrayList<>();

    private final Map<ResourceLocation, Integer> syncedLibraryLevels = new HashMap<>();

    private final Player player;
    private final Level level;
    private final BlockPos tablePos;

    private Slot helmetSlot;
    private Slot chestplateSlot;
    private Slot leggingsSlot;
    private Slot bootsSlot;

    private final SimpleContainer itemContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ResearchTableMenu.this.onItemSlotChanged();
        }
    };

    private final SimpleContainer lapisContainer = new SimpleContainer(1);

    private final SimpleContainer duplicateBookContainer = new SimpleContainer(1);

    public ResearchTableMenu(
            int containerId,
            Inventory playerInventory
    ) {
        this(
                containerId,
                playerInventory,
                BlockPos.ZERO
        );
    }

    public ResearchTableMenu(
            int containerId,
            Inventory playerInventory,
            RegistryFriendlyByteBuf buffer
    ) {
        this(
                containerId,
                playerInventory,
                buffer.readBlockPos()
        );
    }

    public ResearchTableMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos tablePos
    ) {
        super(ModMenus.RESEARCH_TABLE_MENU.get(), containerId);

        this.player = playerInventory.player;
        this.level = playerInventory.player.level();
        this.tablePos = tablePos;

        this.addSlot(new Slot(
                this.itemContainer,
                0,
                ITEM_SLOT_X,
                ITEM_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidItemSlotStack(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addSlot(new Slot(
                this.lapisContainer,
                0,
                LAPIS_SLOT_X,
                LAPIS_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAPIS_LAZULI);
            }
        });

        this.addSlot(new Slot(
                this.duplicateBookContainer,
                0,
                DUPLICATE_BOOK_SLOT_X,
                DUPLICATE_BOOK_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.ENCHANTED_BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addArmorSlots(playerInventory);

        onItemSlotChanged();
        syncLibraryToClient();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        syncLibraryToClient();
    }

    public List<Holder<Enchantment>> getVisibleEnchantments() {
        return this.visibleEnchantments;
    }

    public ItemStack getResearchItem() {
        return this.itemContainer.getItem(0);
    }

    public ItemStack getLapisItem() {
        return this.lapisContainer.getItem(0);
    }

    public ItemStack getDuplicateBookItem() {
        return this.duplicateBookContainer.getItem(0);
    }

    // Temporary compatibility getter.
    // Older code may still call getPaymentItem().
    public ItemStack getPaymentItem() {
        return getLapisItem();
    }

    public ItemStack getHelmetItem() {
        return this.helmetSlot == null ? ItemStack.EMPTY : this.helmetSlot.getItem();
    }

    public ItemStack getChestplateItem() {
        return this.chestplateSlot == null ? ItemStack.EMPTY : this.chestplateSlot.getItem();
    }

    public ItemStack getLeggingsItem() {
        return this.leggingsSlot == null ? ItemStack.EMPTY : this.leggingsSlot.getItem();
    }

    public ItemStack getBootsItem() {
        return this.bootsSlot == null ? ItemStack.EMPTY : this.bootsSlot.getItem();
    }

    public boolean consumeLapis(int amount) {
        ItemStack lapisStack = this.lapisContainer.getItem(0);

        if (!lapisStack.is(Items.LAPIS_LAZULI)) {
            return false;
        }

        if (lapisStack.getCount() < amount) {
            return false;
        }

        lapisStack.shrink(amount);
        this.lapisContainer.setChanged();
        this.broadcastChanges();

        return true;
    }

    public boolean consumePaymentBook() {
        ItemStack bookStack = this.duplicateBookContainer.getItem(0);

        if (!bookStack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }

        bookStack.shrink(1);
        this.duplicateBookContainer.setChanged();
        this.broadcastChanges();

        return true;
    }

    public void markResearchItemChanged() {
        this.itemContainer.setChanged();
        this.broadcastChanges();
    }

    public void clearSyncedLibraryLevels() {
        this.syncedLibraryLevels.clear();
    }

    public void setSyncedLibraryLevel(
            ResourceLocation enchantmentId,
            int level
    ) {
        this.syncedLibraryLevels.put(enchantmentId, level);
    }

    public void refreshVisibleEnchantments() {
        this.visibleEnchantments = getVisibleEnchantmentsFromLibrary(
                this.itemContainer.getItem(0)
        );
    }

    private void syncLibraryToClient() {
        if (this.level.isClientSide) {
            return;
        }

        if (!(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        LibraryKnowledgeSync.sendToClient(
                serverPlayer,
                this
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot clickedSlot = this.slots.get(index);

        if (clickedSlot == null || !clickedSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack clickedStack = clickedSlot.getItem();
        originalStack = clickedStack.copy();

        // Slot indexes:
        // 0 = item slot
        // 1 = lapis slot
        // 2 = duplicate book slot
        // 3-29 = player inventory
        // 30-38 = hotbar
        // 39-42 = armor slots

        if (index == 0 || index == 1 || index == 2) {
            if (!this.moveItemStackTo(clickedStack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (clickedStack.is(Items.LAPIS_LAZULI)) {
                if (!this.moveItemStackTo(clickedStack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (clickedStack.is(Items.ENCHANTED_BOOK)) {
                if (!this.moveItemStackTo(clickedStack, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isValidItemSlotStack(clickedStack)) {
                if (!this.moveItemStackTo(clickedStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (clickedStack.isEmpty()) {
            clickedSlot.set(ItemStack.EMPTY);
        } else {
            clickedSlot.setChanged();
        }

        return originalStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            this.clearContainer(player, this.itemContainer);
            this.clearContainer(player, this.lapisContainer);
            this.clearContainer(player, this.duplicateBookContainer);
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        INVENTORY_START_X + column * 18,
                        INVENTORY_START_Y + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(
                    inventory,
                    slot,
                    HOTBAR_START_X + slot * 18,
                    HOTBAR_START_Y
            ));
        }
    }

    private void addArmorSlots(Inventory inventory) {
        this.helmetSlot = new ArmorOnlySlot(
                inventory,
                39,
                LEFT_ARMOR_X,
                ARMOR_TOP_Y,
                EquipmentSlot.HEAD
        );
        this.addSlot(this.helmetSlot);

        this.chestplateSlot = new ArmorOnlySlot(
                inventory,
                38,
                LEFT_ARMOR_X,
                ARMOR_BOTTOM_Y,
                EquipmentSlot.CHEST
        );
        this.addSlot(this.chestplateSlot);

        this.leggingsSlot = new ArmorOnlySlot(
                inventory,
                37,
                RIGHT_ARMOR_X,
                ARMOR_TOP_Y,
                EquipmentSlot.LEGS
        );
        this.addSlot(this.leggingsSlot);

        this.bootsSlot = new ArmorOnlySlot(
                inventory,
                36,
                RIGHT_ARMOR_X,
                ARMOR_BOTTOM_Y,
                EquipmentSlot.FEET
        );
        this.addSlot(this.bootsSlot);
    }

    private static class ArmorOnlySlot extends Slot {
        private final EquipmentSlot equipmentSlot;

        public ArmorOnlySlot(
                Inventory inventory,
                int slotIndex,
                int x,
                int y,
                EquipmentSlot equipmentSlot
        ) {
            super(inventory, slotIndex, x, y);
            this.equipmentSlot = equipmentSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!(stack.getItem() instanceof ArmorItem armorItem)) {
                return false;
            }

            return armorItem.getEquipmentSlot() == this.equipmentSlot;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private boolean isValidItemSlotStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }

        if (stack.isEnchantable()) {
            return true;
        }

        return !stack.getEnchantments().isEmpty();
    }

    private void onItemSlotChanged() {
        ItemStack stack = this.itemContainer.getItem(0);

        if (this.player == null) {
            return;
        }

        this.visibleEnchantments = getVisibleEnchantmentsFromLibrary(stack);

        if (!this.player.level().isClientSide) {
            VanillaReenchanted.LOGGER.info(
                    "Research Table item changed: {}",
                    stack.getDisplayName().getString()
            );

            VanillaReenchanted.LOGGER.info("Visible enchantments:");

            for (Holder<Enchantment> enchantmentHolder : this.visibleEnchantments) {
                enchantmentHolder.unwrapKey().ifPresent(key -> {
                    VanillaReenchanted.LOGGER.info("- {}", key.location());
                });
            }
        }

        debugLogLibraryScan();
        syncLibraryToClient();
    }

    private List<Holder<Enchantment>> getVisibleEnchantmentsFromLibrary(ItemStack stack) {
        List<Holder<Enchantment>> result = new ArrayList<>();

        LibraryScanner.LibraryScanResult libraryScanResult = scanLibrary();

        for (Holder<Enchantment> enchantmentHolder : getAllEnchantments()) {
            ResourceLocation enchantmentId = getEnchantmentId(enchantmentHolder);

            if (enchantmentId == null) {
                continue;
            }

            boolean isTreasure = enchantmentHolder.is(EnchantmentTags.TREASURE);

            int libraryLevel;

            if (this.level.isClientSide) {
                libraryLevel = this.syncedLibraryLevels.getOrDefault(enchantmentId, 0);
            } else {
                libraryLevel = libraryScanResult.getMaxLevel(enchantmentId);
            }

            if (isTreasure && libraryLevel <= 0) {
                continue;
            }

            if (!stack.isEmpty() && !stack.supportsEnchantment(enchantmentHolder)) {
                continue;
            }

            result.add(enchantmentHolder);
        }

        return result;
    }

    public int getLibraryLevel(Holder<Enchantment> enchantmentHolder) {
        ResourceLocation enchantmentId = getEnchantmentId(enchantmentHolder);

        if (enchantmentId == null) {
            return 0;
        }

        if (this.level.isClientSide) {
            return this.syncedLibraryLevels.getOrDefault(enchantmentId, 0);
        }

        return scanLibrary().getMaxLevel(enchantmentId);
    }

    public LibraryScanner.LibraryScanResult scanLibrary() {
        if (this.tablePos.equals(BlockPos.ZERO)) {
            return new LibraryScanner.LibraryScanResult();
        }

        return LibraryScanner.scan(
                this.level,
                this.tablePos,
                getAllEnchantments()
        );
    }

    private List<Holder<Enchantment>> getAllEnchantments() {
        return this.player
                .registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .holders()
                .map(holder -> (Holder<Enchantment>) holder)
                .toList();
    }

    private ResourceLocation getEnchantmentId(
            Holder<Enchantment> enchantmentHolder
    ) {
        return enchantmentHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
    }

    private void debugLogLibraryScan() {
        if (this.level.isClientSide) {
            return;
        }

        if (this.tablePos.equals(BlockPos.ZERO)) {
            VanillaReenchanted.LOGGER.info(
                    "Library scan skipped: table position is unknown."
            );
            return;
        }

        LibraryScanner.LibraryScanResult scanResult = scanLibrary();

        VanillaReenchanted.LOGGER.info("----- Research Table Library Scan -----");
        VanillaReenchanted.LOGGER.info("Table position: {}", this.tablePos);
        VanillaReenchanted.LOGGER.info(
                "Normal books found: {}",
                scanResult.getNormalBookSlots().size()
        );
        VanillaReenchanted.LOGGER.info(
                "Enchanted books found: {}",
                scanResult.getEnchantedBookSlots().size()
        );

        for (LibraryScanner.LibraryKnowledgeEntry entry : scanResult.getKnowledgeEntries()) {
            VanillaReenchanted.LOGGER.info(
                    "Knowledge: {} level {} at shelf {} slot {}",
                    entry.enchantmentId(),
                    entry.level(),
                    entry.shelfPos(),
                    entry.slot()
            );
        }

        VanillaReenchanted.LOGGER.info("---------------------------------------");
    }
    public BlockPos getTablePos() {
        return this.tablePos;
    }
}