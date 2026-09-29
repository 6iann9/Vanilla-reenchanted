package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.WindUp;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class WindUpMovementMixin {
    // Only movement checks ignore this use action; the charge and animation stay active.
    @Redirect(method = {"aiStep", "canStartSprinting"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean vr$noChargeSlowdown(LocalPlayer player) {
        boolean using = player.isUsingItem();
        return using && !(player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && WindUp.level(player.getUseItem()) > 0);
    }
}