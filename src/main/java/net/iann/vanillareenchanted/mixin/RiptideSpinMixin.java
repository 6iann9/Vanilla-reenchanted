package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.event.RiptideEvents;
import net.iann.vanillareenchanted.enchantment.RiptideSpinAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class RiptideSpinMixin implements RiptideSpinAccess {
    @ModifyArg(method = {"travel", "handleRelativeFrictionAndCalculateMovement"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
            index = 1)
    private Vec3 vr$reduceDashDescent(Vec3 movement) {
        if (movement.y < 0.0 && autoSpinAttackTicks > 0
                && (Object)this instanceof Player player && RiptideEvents.data(player).spinning) {
            // Reduce actual descent once per movement, without compounding a velocity multiplier
            // every tick. Vanilla still resolves collisions and tracks the distance fallen.
            return new Vec3(movement.x, movement.y * 0.2, movement.z);
        }
        return movement;
    }

    @Shadow protected int autoSpinAttackTicks;
    @Shadow protected float autoSpinAttackDmg;
    @Shadow protected ItemStack autoSpinAttackItemStack;
    @Shadow protected abstract void setLivingEntityFlag(int flag, boolean value);
    public int vr$spinTicks() { return autoSpinAttackTicks; }
    public void vr$stopSpin() {
        autoSpinAttackTicks = 0;
        autoSpinAttackDmg = 0;
        autoSpinAttackItemStack = null;
        setLivingEntityFlag(4, false);
    }
    @Inject(method = "checkAutoSpinAttack", at = @At("HEAD"), cancellable = true)
    private void vr$collision(AABB before, AABB after, CallbackInfo ci) {
        if ((Object)this instanceof Player player && RiptideEvents.data(player).spinning) {
            RiptideEvents.collide(player, before, after);
            ci.cancel();
        }
    }
}
