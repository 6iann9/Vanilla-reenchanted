package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.AimedSweep;
import net.iann.vanillareenchanted.network.SweepMissPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class SweepMissInputMixin {
    @Inject(method = "startAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"))
    private void vr$sweepMiss(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        // This branch only runs for an uncancelled miss, before vanilla resets the charge.
        if (player != null && AimedSweep.hasSweepingEdge(player)) {
            PacketDistributor.sendToServer(SweepMissPayload.INSTANCE);
        }
    }
}
