package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.iann.vanillareenchanted.client.WindUpClient;
import net.iann.vanillareenchanted.enchantment.WindUp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Reuse vanilla positioning, sprites, visibility settings and HUD-layer events. */
@Mixin(Gui.class)
public abstract class WindUpIndicatorMixin {
    @WrapOperation(method = {"renderCrosshair", "renderItemHotbar"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private float vr$windUpProgress(LocalPlayer player, float partialTick, Operation<Float> original) {
        if (WindUp.level(player.getMainHandItem()) > 0) {
            return WindUpClient.indicatorProgress(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        }
        return original.call(player, partialTick);
    }
}
