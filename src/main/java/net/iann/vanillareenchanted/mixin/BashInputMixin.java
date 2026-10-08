package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.client.BashAnimations;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class BashInputMixin {
    // Vanilla discards attack clicks while using a shield, before its usual attack input event.
    @Inject(method="handleKeybinds", at=@At("HEAD"))
    private void vr$shieldBash(CallbackInfo ci) { BashAnimations.input(); }

    @WrapOperation(method="handleKeybinds", at=@At(value="INVOKE",
            target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
    private void vr$holdShieldDuringBash(MultiPlayerGameMode gameMode, Player player, Operation<Void> original) {
        if (player.isBlocking() && BashAnimations.busy(Minecraft.getInstance().player)) return;
        original.call(gameMode, player);
    }

    @Inject(method="startAttack", at=@At("HEAD"), cancellable=true)
    private void vr$noAttackDuringBash(CallbackInfoReturnable<Boolean> cir) {
        if (BashAnimations.busy(Minecraft.getInstance().player)) cir.setReturnValue(false);
    }

    @Inject(method="continueAttack", at=@At("HEAD"), cancellable=true)
    private void vr$noMiningDuringBash(boolean attacking, CallbackInfo ci) {
        if (BashAnimations.busy(Minecraft.getInstance().player)) ci.cancel();
    }
}
