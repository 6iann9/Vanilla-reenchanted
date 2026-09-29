package net.iann.vanillareenchanted.mixin;
import net.iann.vanillareenchanted.event.MomentumEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class MomentumMovementMixin {
    @Inject(method="handleRelativeFrictionAndCalculateMovement", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V", shift=At.Shift.AFTER))
    private void vr$preserveDownhillSpeed(Vec3 input, float friction, CallbackInfoReturnable<Vec3> ci) {
        if ((Object)this instanceof AbstractHorse horse) MomentumEvents.preserveDownhillSpeed(horse);
    }
}
