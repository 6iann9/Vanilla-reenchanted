package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.event.MomentumEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MomentumVehicleCorrectionMixin {
    @Inject(method = "handleMoveVehicle", at = @At("RETURN"))
    private void vr$discardRejectedMomentum(ClientboundMoveVehiclePacket packet, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getRootVehicle() instanceof AbstractHorse horse
                && horse.isControlledByLocalInstance()) {
            MomentumEvents.onVehicleCorrection(horse);
        }
    }
}
