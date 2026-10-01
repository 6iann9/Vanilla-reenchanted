package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.iann.vanillareenchanted.registry.ModParticles;
import net.iann.vanillareenchanted.enchantment.AimedSweep;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class SweepSlashMixin {
    @WrapOperation(method="attack", at=@At(value="INVOKE",
            target="Lnet/neoforged/neoforge/common/CommonHooks;fireSweepAttack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Z)Lnet/neoforged/neoforge/event/entity/player/SweepAttackEvent;"))
    private SweepAttackEvent vr$sweepingEdgeRequired(Player player, Entity target, boolean vanilla,
            Operation<SweepAttackEvent> original, @Local CriticalHitEvent critical) {
        // Change only sweeping eligibility, not normal attack damage or the critical-hit threshold.
        boolean sweep = AimedSweep.canSweep(player)
                && !(critical.isCriticalHit() && critical.disableSweep());
        return original.call(player, target, sweep);
    }

    @WrapOperation(method="attack", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/item/ItemStack;getSweepHitBox(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/AABB;"))
    private AABB vr$aimedSweepBox(ItemStack stack, Player player, Entity target, Operation<AABB> original) {
        return AimedSweep.hasSweepingEdge(player) ? AimedSweep.bounds(player) : original.call(stack, player, target);
    }

    @WrapOperation(method="sweepAttack", at=@At(value="INVOKE",
            target="Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int vr$orientedSweep(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                                int count, double forwardX, double dy, double forwardZ, double speed,
                                Operation<Integer> original) {
        // Count zero sends one particle; speed one preserves the supplied attack direction.
        Player player = (Player)(Object)this;
        var direction = player.getLookAngle();
        return original.call(level, ModParticles.SWEEP_SLASH.get(),
                player.getX() + direction.x, player.getY(0.5) + direction.y, player.getZ() + direction.z,
                0, direction.x, direction.y, direction.z, 1D);
    }
}
