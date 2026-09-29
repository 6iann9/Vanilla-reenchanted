package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.iann.vanillareenchanted.event.ShockwaveEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MaceItem.class)
public abstract class ShockwaveMaceParticlesMixin {
    @WrapOperation(method = "knockback", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private static void vr$replaceSmashParticles(Level level, int event, BlockPos pos, int data,
                                                Operation<Void> original, @Local(argsOnly = true) Player player) {
        if (event != 2013 || ShockwaveEvents.level(player.getMainHandItem()) == 0) {
            original.call(level, event, pos, data);
        }
    }

    @WrapOperation(method = "hurtEnemy", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setSpawnExtraParticlesOnFall(Z)V"))
    private void vr$skipExtraLandingParticles(ServerPlayer player, boolean enabled,
                                             Operation<Void> original, @Local(argsOnly = true) ItemStack stack) {
        if (ShockwaveEvents.level(stack) == 0) original.call(player, enabled);
    }
}
