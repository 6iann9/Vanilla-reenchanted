package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.event.RiptideEvents;
import net.iann.vanillareenchanted.enchantment.RiptideSpinAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class RiptideSpinMixin implements RiptideSpinAccess {
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
