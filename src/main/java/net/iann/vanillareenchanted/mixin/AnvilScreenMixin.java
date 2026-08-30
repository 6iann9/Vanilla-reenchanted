package net.iann.vanillareenchanted.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends AbstractContainerScreen<AnvilMenu> {

    protected AnvilScreenMixin(
            AnvilMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
    }

    @Inject(
            method = "renderLabels",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vr$hideFreeAnvilCostLabel(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            CallbackInfo callbackInfo
    ) {
        if (!isAllowedFreeAnvilOperation()) {
            return;
        }

        // Draw only the normal screen title.
        // Do not let vanilla draw "Enchantment Cost".
        guiGraphics.drawString(
                this.font,
                this.title,
                this.titleLabelX,
                this.titleLabelY,
                4210752,
                false
        );

        callbackInfo.cancel();
    }

    private boolean isAllowedFreeAnvilOperation() {
        ItemStack left = this.menu.getSlot(0).getItem();
        ItemStack right = this.menu.getSlot(1).getItem();

        if (left.isEmpty()) {
            return false;
        }

        // Rename-only operation.
        if (right.isEmpty()) {
            return true;
        }

        if (isEnchantedBook(left) || isEnchantedBook(right)) {
            return false;
        }

        boolean leftEnchanted = hasEnchantments(left);
        boolean rightEnchanted = hasEnchantments(right);

        // Enchanted item + enchanted item is blocked.
        return !(leftEnchanted && rightEnchanted);
    }

    private boolean isEnchantedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }

    private boolean hasEnchantments(ItemStack stack) {
        return !stack.getEnchantments().isEmpty();
    }
}