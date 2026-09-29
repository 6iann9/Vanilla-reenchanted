package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.iann.vanillareenchanted.enchantment.WindBurst;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Use the vanilla overlay without imposing an item-use lock on the mace. */
@Mixin(GuiGraphics.class)
public abstract class WindBurstOverlayMixin {
    @WrapOperation(method="renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at=@At(value="INVOKE", target="Lnet/minecraft/world/item/ItemCooldowns;getCooldownPercent(Lnet/minecraft/world/item/Item;F)F"))
    private float vr$windBurstOverlay(ItemCooldowns cooldowns, Item item, float partialTick,
                                      Operation<Float> original, @Local(argsOnly=true) ItemStack stack) {
        float vanilla = original.call(cooldowns, item, partialTick);
        var player = Minecraft.getInstance().player;
        return player != null && WindBurst.level(stack, player) > 0
                ? Math.max(vanilla, WindBurst.cooldownFraction(player, partialTick)) : vanilla;
    }
}
