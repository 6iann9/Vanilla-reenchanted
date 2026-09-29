package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.iann.vanillareenchanted.registry.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class SweepSlashMixin {
    @WrapOperation(method="sweepAttack", at=@At(value="INVOKE",
            target="Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int vr$orientedSweep(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                                int count, double forwardX, double dy, double forwardZ, double speed,
                                Operation<Integer> original) {
        // Count zero sends one particle; speed one preserves the supplied attack direction.
        return original.call(level, ModParticles.SWEEP_SLASH.get(), x, y, z, 0, forwardX, 0D, forwardZ, 1D);
    }
}
