package net.iann.vanillareenchanted.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    public abstract void setMaximumCost(long value);

    @Inject(
            method = "createResult",
            at = @At("TAIL")
    )
    private void vr$makeAllowedAnvilResultFree(CallbackInfo callbackInfo) {
        if (isAllowedFreeAnvilOperation()) {
            this.setMaximumCost(0);
        }
    }

    @Inject(
            method = "mayPickup",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vr$allowPickupWithZeroCost(
            Player player,
            boolean hasStack,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!hasStack) {
            return;
        }

        if (!isAllowedFreeAnvilOperation()) {
            return;
        }

        callbackInfo.setReturnValue(true);
    }

    @Inject(
            method = "onTake",
            at = @At("HEAD")
    )
    private void vr$keepAllowedAnvilOperationFree(
            Player player,
            ItemStack stack,
            CallbackInfo callbackInfo
    ) {
        if (isAllowedFreeAnvilOperation()) {
            this.setMaximumCost(0);
        }
    }

    private boolean isAllowedFreeAnvilOperation() {
        ItemStack left = getLeftInput();
        ItemStack right = getRightInput();

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

        // Block enchanted item + enchanted item.
        if (leftEnchanted && rightEnchanted) {
            return false;
        }

        // Allow:
        // enchanted item + unenchanted item
        // unenchanted item + unenchanted item
        return true;
    }

    private ItemStack getLeftInput() {
        return ((AnvilMenu) (Object) this)
                .getSlot(0)
                .getItem();
    }

    private ItemStack getRightInput() {
        return ((AnvilMenu) (Object) this)
                .getSlot(1)
                .getItem();
    }

    private boolean isEnchantedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }

    private boolean hasEnchantments(ItemStack stack) {
        return !stack.getEnchantments().isEmpty();
    }

    @Inject(
            method = "setItemName",
            at = @At("TAIL")
    )
    private void vr$keepRenameCostFree(
            String itemName,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (isAllowedFreeAnvilOperation()) {
            this.setMaximumCost(0);
        }
    }
}