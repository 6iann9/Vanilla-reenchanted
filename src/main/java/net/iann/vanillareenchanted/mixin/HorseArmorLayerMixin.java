package net.iann.vanillareenchanted.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HorseArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.AnimalArmorItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseArmorLayer.class)
public abstract class HorseArmorLayerMixin {
    @Shadow @Final private HorseModel<Horse> model;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/horse/Horse;FFFFFF)V",
            at = @At("RETURN"))
    private void vr$renderArmorGlint(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            Horse horse, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callback) {
        var stack = horse.getBodyArmorItem();
        if (stack.hasFoil() && stack.getItem() instanceof AnimalArmorItem armor
                && armor.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN) {
            // Horse armor uses entityCutoutNoCull without the player armor depth offset.
            // Its glint must also omit that offset, or the equal-depth test rejects the overlay.
            model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityGlintDirect()),
                    packedLight, OverlayTexture.NO_OVERLAY);
        }
    }
}
