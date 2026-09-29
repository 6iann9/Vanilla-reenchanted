package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.iann.vanillareenchanted.enchantment.LoyaltyReturn;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Vanilla has an inventory insertion in both the base pickup and the returning-trident fallback.
@Mixin({AbstractArrow.class, ThrownTrident.class})
public abstract class LoyaltyReturnMixin {
    @WrapOperation(method = "tryPickup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean vr$returnedStack(Inventory inventory, ItemStack stack, Operation<Boolean> original) {
        if ((Object)this instanceof ThrownTrident trident)
            LoyaltyReturn.retrieve(trident, inventory.player, stack);
        // stack is a pickup copy: a full inventory cannot grant a bonus to any held trident.
        return original.call(inventory, stack);
    }
    @Inject(method = "tryPickup", at = @At("RETURN"))
    private void vr$creativeReturn(Player player, CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValue() || player.level().isClientSide
                || !((Object)this instanceof ThrownTrident trident)
                || trident.pickup != AbstractArrow.Pickup.CREATIVE_ONLY || trident.getOwner() != player) return;
        // Creative throwing retains the original item instead of inserting the returning copy.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, trident.getPickupItemStackOrigin())) {
                LoyaltyReturn.retrieve(trident, player, stack);
                break;
            }
        }
    }
}
