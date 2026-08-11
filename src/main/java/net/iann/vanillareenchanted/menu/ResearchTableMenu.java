package net.iann.vanillareenchanted.menu;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.enchantment.EnchantmentVisibilityHelper;
import net.iann.vanillareenchanted.registry.ModMenus;
import net.minecraft.core.Holder;
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

import java.util.ArrayList;
import java.util.List;

public class ResearchTableMenu extends AbstractContainerMenu {
    private List<Holder<Enchantment>> visibleEnchantments = new ArrayList<>();
    private static final int ITEM_SLOT_X = 72;
    private static final int ITEM_SLOT_Y = 19;

    private static final int PAYMENT_SLOT_X = 189;
    private static final int PAYMENT_SLOT_Y = 19;

    private static final int INVENTORY_START_X = 59;
    private static final int INVENTORY_START_Y = 196;

    private static final int HOTBAR_START_X = 59;
    private static final int HOTBAR_START_Y = 254;

    private static final int LEFT_ARMOR_X = 37;
    private static final int RIGHT_ARMOR_X = 225;
    private static final int ARMOR_TOP_Y = 196;
    private static final int ARMOR_BOTTOM_Y = 232;
    private final Player player;

    private final SimpleContainer itemContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ResearchTableMenu.this.onItemSlotChanged();
        }
    };

    private final SimpleContainer paymentContainer = new SimpleContainer(1);

    public ResearchTableMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.RESEARCH_TABLE_MENU.get(), containerId);
        this.player = playerInventory.player;

        // Slot 0: enchantable item slot
        this.addSlot(new Slot(this.itemContainer, 0, ITEM_SLOT_X, ITEM_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidItemSlotStack(stack);
            }
        });

        // Slot 1: lapis / enchanted book slot
        this.addSlot(new Slot(this.paymentContainer, 0, PAYMENT_SLOT_X, PAYMENT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isPaymentItem(stack);
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addArmorSlots(playerInventory);

        this.onItemSlotChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public List<Holder<Enchantment>> getVisibleEnchantments() {
        return this.visibleEnchantments;
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
        // 0 = item/encrypted book slot
        // 1 = lapis/enchanted book slot
        // 2-28 = player inventory
        // 29-37 = hotbar
        // 38-41 = armor slots

        if (index == 0 || index == 1) {
            // Move from custom slots back into player inventory/hotbar
            if (!this.moveItemStackTo(clickedStack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (isPaymentItem(clickedStack)) {
                if (!this.moveItemStackTo(clickedStack, 1, 2, false)) {
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
            this.clearContainer(player, this.paymentContainer);
        }
    }

    public ItemStack getResearchItem() {
        return this.itemContainer.getItem(0);
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
        // Minecraft player armor inventory indexes:
        // 39 = helmet
        // 38 = chestplate
        // 37 = leggings
        // 36 = boots

        this.addSlot(new ArmorOnlySlot(inventory, 39, LEFT_ARMOR_X, ARMOR_TOP_Y, EquipmentSlot.HEAD));
        this.addSlot(new ArmorOnlySlot(inventory, 38, LEFT_ARMOR_X, ARMOR_BOTTOM_Y, EquipmentSlot.CHEST));

        this.addSlot(new ArmorOnlySlot(inventory, 37, RIGHT_ARMOR_X, ARMOR_TOP_Y, EquipmentSlot.LEGS));
        this.addSlot(new ArmorOnlySlot(inventory, 36, RIGHT_ARMOR_X, ARMOR_BOTTOM_Y, EquipmentSlot.FEET));
    }

    private static class ArmorOnlySlot extends Slot {
        private final EquipmentSlot equipmentSlot;

        public ArmorOnlySlot(Inventory inventory, int slotIndex, int x, int y, EquipmentSlot equipmentSlot) {
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

        // Later our encrypted book will also be allowed here.
        if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }

        // Accept normal enchantable gear.
        if (stack.isEnchantable()) {
            return true;
        }

        // Also accept gear that already has enchantments.
        return !stack.getEnchantments().isEmpty();
    }

    private boolean isPaymentItem(ItemStack stack) {
        return stack.is(Items.LAPIS_LAZULI)
                || stack.is(Items.ENCHANTED_BOOK);
    }

    private void onItemSlotChanged() {
        ItemStack stack = this.itemContainer.getItem(0);

        if (this.player == null) {
            return;
        }

        this.visibleEnchantments =
                EnchantmentVisibilityHelper.getVisibleEnchantments(this.player, stack);

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
    }
    public ItemStack getPaymentItem() {
        return this.paymentContainer.getItem(0);
    }

    public boolean consumeLapis(int amount) {
        ItemStack paymentStack = this.paymentContainer.getItem(0);

        if (!paymentStack.is(Items.LAPIS_LAZULI)) {
            return false;
        }

        if (paymentStack.getCount() < amount) {
            return false;
        }

        paymentStack.shrink(amount);
        this.paymentContainer.setChanged();
        this.broadcastChanges();

        return true;
    }

    public void markResearchItemChanged() {
        this.itemContainer.setChanged();
        this.broadcastChanges();
    }
    public boolean consumePaymentBook() {
        ItemStack paymentStack = this.paymentContainer.getItem(0);

        if (!paymentStack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }

        paymentStack.shrink(1);
        this.paymentContainer.setChanged();
        this.broadcastChanges();

        return true;
    }

}