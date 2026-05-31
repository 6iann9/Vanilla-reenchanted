package net.iann.vanillareenchanted.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class ResearchTableMenuProvider implements MenuProvider {

    @Override
    public Component getDisplayName() {
        return Component.literal("Research Table");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            net.minecraft.world.entity.player.Player player
    ) {
        return new ResearchTableMenu(containerId, playerInventory);
    }
}