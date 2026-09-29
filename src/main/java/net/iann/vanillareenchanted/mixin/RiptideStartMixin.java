package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.event.RiptideEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class RiptideStartMixin {
    @Inject(method = "startAutoSpinAttack", at = @At("TAIL"))
    private void vr$start(int ticks, float damage, ItemStack stack, CallbackInfo ci) {
        if (RiptideEvents.enchanted(stack)) RiptideEvents.start((Player)(Object)this);
    }
}
