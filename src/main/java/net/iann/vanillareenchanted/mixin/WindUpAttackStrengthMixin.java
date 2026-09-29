package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.WindUp;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class WindUpAttackStrengthMixin {
    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void vr$windUpAttackStrength(float partialTick, CallbackInfoReturnable<Float> cir) {
        // Wind Up supplies its own charge and recovery timer; skip vanilla damage recharge.
        if (WindUp.level(((Player) (Object) this).getMainHandItem()) > 0) {
            cir.setReturnValue(1.0F);
        }
    }
}