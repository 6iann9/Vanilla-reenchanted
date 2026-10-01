package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.iann.vanillareenchanted.enchantment.EchoingEdge;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class EchoingEdgeAttackMixin {
    @WrapMethod(method="attack")
    private void vr$echoAttack(Entity target, Operation<Void> original) {
        EchoingEdge.attack((Player)(Object)this, target, () -> original.call(target));
    }

    @WrapOperation(method="attack", at=@At(value="INVOKE", target="Lnet/neoforged/neoforge/common/CommonHooks;fireCriticalHit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;ZF)Lnet/neoforged/neoforge/event/entity/player/CriticalHitEvent;"))
    private CriticalHitEvent vr$crit(Player player, Entity target, boolean vanilla, float multiplier, Operation<CriticalHitEvent> original) {
        var event = original.call(player, target, vanilla, multiplier);
        if (EchoingEdge.current() != null) EchoingEdge.current().critical = event.isCriticalHit();
        return event;
    }

    @WrapOperation(method="attack", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean vr$primary(Entity target, DamageSource source, float damage, Operation<Boolean> original) {
        try { return original.call(target, source, EchoingEdge.beginHit(target, damage)); }
        finally { if (EchoingEdge.current() != null) EchoingEdge.current().activeTarget = null; }
    }

    @WrapOperation(method="attack", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean vr$secondary(LivingEntity target, DamageSource source, float damage, Operation<Boolean> original) {
        try { return original.call(target, source, EchoingEdge.beginHit(target, damage)); }
        finally { if (EchoingEdge.current() != null) EchoingEdge.current().activeTarget = null; }
    }
}
