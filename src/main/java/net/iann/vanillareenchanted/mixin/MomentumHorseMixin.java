package net.iann.vanillareenchanted.mixin;
import net.iann.vanillareenchanted.event.MomentumEvents;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractHorse.class)
public abstract class MomentumHorseMixin {
    @Inject(method="tickRidden", at=@At("RETURN"))
    private void vr$momentum(Player rider, Vec3 input, CallbackInfo ci) {
        MomentumEvents.tickRidden((AbstractHorse)(Object)this, rider, input);
    }
}
