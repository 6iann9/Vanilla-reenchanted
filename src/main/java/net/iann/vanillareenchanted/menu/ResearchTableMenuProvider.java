package net.iann.vanillareenchanted.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class ResearchTableMenuProvider implements MenuProvider {

    private final BlockPos tablePos;

    public ResearchTableMenuProvider(BlockPos tablePos) {
        this.tablePos = tablePos;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Research Table");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new ResearchTableMenu(
                containerId,
                playerInventory,
                this.tablePos
        );
    }
}