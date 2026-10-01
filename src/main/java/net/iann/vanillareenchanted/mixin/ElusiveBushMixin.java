package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.Elusive;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ElusiveBushMixin {
    @Inject(method="makeStuckInBlock", at=@At("HEAD"), cancellable=true)
    private void vr$ignoreBerrySlowdown(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        if (state.getBlock() instanceof SweetBerryBushBlock
                && (Object)this instanceof AbstractHorse horse && Elusive.enchanted(horse)) ci.cancel();
    }
}
