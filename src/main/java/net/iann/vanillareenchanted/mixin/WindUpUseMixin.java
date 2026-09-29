package net.iann.vanillareenchanted.mixin;
import net.iann.vanillareenchanted.client.WindUpClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MultiPlayerGameMode.class)
public abstract class WindUpUseMixin {
    // Vanilla otherwise releases item use whenever right-click is up, even while attack is held.
    @Inject(method="releaseUsingItem", at=@At("HEAD"), cancellable=true)
    private void vr$keepAttackCharge(Player player, CallbackInfo ci) {
        if (WindUpClient.charging()) ci.cancel();
    }
}
