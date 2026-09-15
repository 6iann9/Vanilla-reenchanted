package net.iann.vanillareenchanted.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Inject(
            method = "repairPlayerItems",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vr$disableXpOrbMending(
            ServerPlayer player,
            int experienceAmount,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        // Return the full XP amount unchanged.
        // This prevents XP orbs from repairing Mending items,
        // but still lets the player receive the XP normally.
        callbackInfo.setReturnValue(experienceAmount);
    }
}