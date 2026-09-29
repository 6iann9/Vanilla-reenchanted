package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.iann.vanillareenchanted.event.RiptideEvents;
import net.iann.vanillareenchanted.enchantment.Channeling;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphics.class)
public abstract class TridentCooldownOverlayMixin {
    // ItemCooldowns only knows the item type, so evaluate the actual stack while drawing it.
    @WrapOperation(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;getCooldownPercent(Lnet/minecraft/world/item/Item;F)F"))
    private float vr$riptideOverlay(ItemCooldowns cooldowns, Item item, float partialTick,
                                   Operation<Float> original, @Local(argsOnly = true) ItemStack stack) {
        float vanilla = original.call(cooldowns, item, partialTick);
        var player = Minecraft.getInstance().player;
        return player == null ? vanilla : Math.max(vanilla, Math.max(
                RiptideEvents.cooldownPercent(player, stack, partialTick), Channeling.cooldownPercent(player, stack, partialTick)));
    }
}
