package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.iann.vanillareenchanted.enchantment.Channeling;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class ChannelingEntityFireMixin {
    // Skip ignition only: retain lightning damage and any pre-existing burning duration.
    @WrapOperation(method = "thunderHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;setRemainingFireTicks(I)V"))
    private void vr$noChannelingFireTicks(Entity entity, int ticks, Operation<Void> original,
                                        @Local(argsOnly = true) LightningBolt lightning) {
        if (!lightning.getPersistentData().getBoolean(Channeling.FIRELESS)) original.call(entity, ticks);
    }

    @WrapOperation(method = "thunderHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void vr$noChannelingIgnition(Entity entity, float seconds, Operation<Void> original,
                                       @Local(argsOnly = true) LightningBolt lightning) {
        if (!lightning.getPersistentData().getBoolean(Channeling.FIRELESS)) original.call(entity, seconds);
    }
}
