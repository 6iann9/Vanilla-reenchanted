package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.client.BashAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class BashArmPoseMixin {
    @Inject(method="getArmPose", at=@At("RETURN"), cancellable=true)
    private static void vr$bashOwnsArmPose(AbstractClientPlayer player, InteractionHand hand,
            CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (cir.getReturnValue() == HumanoidModel.ArmPose.BLOCK
                && BashAnimations.animatingShield(player, player.getItemInHand(hand))) {
            cir.setReturnValue(HumanoidModel.ArmPose.ITEM);
        }
    }
}
