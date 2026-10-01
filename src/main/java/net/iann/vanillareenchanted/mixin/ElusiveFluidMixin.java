package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.Elusive;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ElusiveFluidMixin {
    @Inject(method="canStandOnFluid", at=@At("HEAD"), cancellable=true)
    private void vr$waterRunning(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof AbstractHorse horse && Elusive.canStandOnWater(horse, fluid)) {
            cir.setReturnValue(true);
        }
    }
}
