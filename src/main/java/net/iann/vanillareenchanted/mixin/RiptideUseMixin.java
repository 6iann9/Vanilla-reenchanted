package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.iann.vanillareenchanted.event.RiptideEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public abstract class RiptideUseMixin {
    // Vanilla checks this both when charging starts and when the trident is released.
    // Only bypass those activation checks; actual water state still controls cooldown speed.
    @ModifyExpressionValue(method = {"use", "releaseUsing"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"), require = 2)
    private boolean vr$allowDryRiptide(boolean inWaterOrRain) {
        return true;
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void vr$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) {
        ItemStack stack = player.getItemInHand(hand);
        if (RiptideEvents.enchanted(stack) && (RiptideEvents.data(player).remaining > 0
                || !player.onGround() && !player.isInWaterOrBubble())) {
            ci.setReturnValue(InteractionResultHolder.fail(stack));
        }
    }
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void vr$release(ItemStack stack, Level level, LivingEntity entity, int left, CallbackInfo ci) {
        if (entity instanceof Player player && RiptideEvents.enchanted(stack)
                && RiptideEvents.data(player).remaining > 0) ci.cancel();
    }
}
