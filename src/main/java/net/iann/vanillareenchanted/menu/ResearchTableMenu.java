package net.iann.vanillareenchanted.menu;

import net.iann.vanillareenchanted.registry.ModMenus;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ResearchTableMenu extends AbstractContainerMenu {
    private final SimpleContainer researchContainer = new SimpleContainer(1);

    public ResearchTableMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.RESEARCH_TABLE_MENU.get(), containerId);

        // Research slot
        this.addSlot(new Slot(this.researchContainer, 0, 80, 35));

        // Player inventory
        addPlayerInventory(playerInventory);

        // Hotbar
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;

        Slot clickedSlot = this.slots.get(index);

        if (clickedSlot != null && clickedSlot.hasItem()) {
            ItemStack clickedStack = clickedSlot.getItem();
            originalStack = clickedStack.copy();

            // Slot 0 = Research Table input slot
            if (index == 0) {
                // Move from research slot into player inventory
                if (!this.moveItemStackTo(clickedStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory into research slot
                if (!this.moveItemStackTo(clickedStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (clickedStack.isEmpty()) {
                clickedSlot.set(ItemStack.EMPTY);
            } else {
                clickedSlot.setChanged();
            }
        }

        return originalStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            this.clearContainer(player, this.researchContainer);
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(
                    inventory,
                    slot,
                    8 + slot * 18,
                    142
            ));
        }
    }
}