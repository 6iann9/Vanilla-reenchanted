package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.Channeling;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public abstract class ChannelingLightningMixin {
    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void vr$noChannelingFire(int extraIgnitions, CallbackInfo ci) {
        if (((LightningBolt)(Object)this).getPersistentData().getBoolean(Channeling.FIRELESS)) ci.cancel();
    }
}
