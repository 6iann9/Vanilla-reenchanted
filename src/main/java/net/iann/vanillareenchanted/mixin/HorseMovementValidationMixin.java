package net.iann.vanillareenchanted.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Allow ridden horse prediction through the speed and position-error thresholds. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HorseMovementValidationMixin {
    @Shadow public ServerPlayer player;

    @ModifyConstant(method="handleMoveVehicle", constant=@Constant(doubleValue=100.0))
    private double vr$horseSpeedThreshold(double original) {
        return player.getRootVehicle() instanceof AbstractHorse horse
                && horse.getControllingPassenger() == player ? Double.MAX_VALUE : original;
    }

    // The other two 0.0625 constants shrink collision boxes; change only the error threshold.
    @ModifyConstant(method="handleMoveVehicle", constant=@Constant(doubleValue=0.0625, ordinal=1))
    private double vr$horsePositionThreshold(double original) {
        return player.getRootVehicle() instanceof AbstractHorse horse
                && horse.getControllingPassenger() == player ? Double.MAX_VALUE : original;
    }
}
