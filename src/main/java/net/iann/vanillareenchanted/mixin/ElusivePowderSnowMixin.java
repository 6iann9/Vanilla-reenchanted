package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.Elusive;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class ElusivePowderSnowMixin {
    @Inject(method="canEntityWalkOnPowderSnow", at=@At("HEAD"), cancellable=true)
    private static void vr$elusiveSnowWalking(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof AbstractHorse horse && Elusive.enchanted(horse)) cir.setReturnValue(true);
    }
}
