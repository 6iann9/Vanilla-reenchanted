package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.WindBurst;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class WindBurstRecoveryMixin {
    @WrapOperation(method="attack", at=@At(value="INVOKE",
            target="Lnet/neoforged/neoforge/common/CommonHooks;fireCriticalHit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;ZF)Lnet/neoforged/neoforge/event/entity/player/CriticalHitEvent;"))
    private CriticalHitEvent vr$captureCrit(Player player, Entity target, boolean vanillaCritical,
                                           float damageModifier, Operation<CriticalHitEvent> original) {
        var event = original.call(player, target, vanillaCritical, damageModifier);
        WindBurst.recordCriticalAttack(player, event.isCriticalHit());
        return event;
    }

    @Inject(method="attack", at=@At("RETURN"))
    private void vr$windBurstRecovery(Entity target, CallbackInfo ci) {
        WindBurst.finishAttack((Player)(Object)this);
    }
}
