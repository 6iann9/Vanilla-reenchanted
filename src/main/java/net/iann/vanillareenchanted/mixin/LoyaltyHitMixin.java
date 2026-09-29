package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.iann.vanillareenchanted.enchantment.LoyaltyReturn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class LoyaltyHitMixin implements LoyaltyReturn.HitTracking {
    @Unique private boolean vr$hit;
    public boolean vr$loyaltyHit() { return vr$hit; }
    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean vr$recordHit(Entity target, DamageSource source, float damage, Operation<Boolean> original) {
        boolean hit = original.call(target, source, damage);
        if (hit && damage > 0 && target instanceof LivingEntity && !target.level().isClientSide) vr$hit = true;
        return hit;
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void vr$saveHit(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("iannvanillareenchanted.LoyaltyHit", vr$hit);
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void vr$loadHit(CompoundTag tag, CallbackInfo ci) {
        vr$hit = tag.getBoolean("iannvanillareenchanted.LoyaltyHit");
    }
}
