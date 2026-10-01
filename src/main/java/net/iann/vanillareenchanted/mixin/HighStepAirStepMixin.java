package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.event.HighStepEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class HighStepAirStepMixin {
    // Extend only the step solver's eligibility check, not the entity's actual grounded state.
    // Entity.move uses this solver for both local movement and server vehicle validation.
    @Redirect(method = "collide", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;onGround()Z"))
    private boolean vr$allowDescendingStep(Entity entity, Vec3 movement) {
        return entity.onGround() || entity instanceof AbstractHorse horse
                && HighStepEvents.canAirStep(horse, movement);
    }
}
