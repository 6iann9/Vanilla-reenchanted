package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.iann.vanillareenchanted.enchantment.LoyaltyReturn;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class LoyaltyAttackMixin {
    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean vr$loyaltyHit(Entity target, DamageSource source, float damage, Operation<Boolean> original) {
        Player player = (Player)(Object)this;
        var stack = player.getMainHandItem();
        long time = player.level().getGameTime();
        boolean boosted = !player.level().isClientSide && !player.isAutoSpinAttack()
                && target instanceof LivingEntity && damage > 0 && LoyaltyReturn.charges(stack, time) > 0;
        boolean hit = original.call(target, source, boosted ? damage * LoyaltyReturn.multiplier(stack) : damage);
        if (boosted && hit) LoyaltyReturn.consume(stack, time);
        return hit;
    }
}
