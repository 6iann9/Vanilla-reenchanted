package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.client.EnchantmentDescriptionHelper;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional client integration; EnchDesc still controls activation, formatting and placement. */
@Pseudo
@Mixin(targets = "net.darkhax.enchdesc.common.impl.EnchdescMod", remap = false)
public abstract class EnchantmentDescriptionsMixin {
    // The full descriptor selects the enchantment-aware overload in EnchDesc 21.1.9.
    // Optional injection so removing EnchDesc, or a changed API, cannot prevent game startup.
    @Inject(method = "getDescription(Lnet/minecraft/core/Holder;Lnet/minecraft/resources/ResourceLocation;I)Lnet/minecraft/network/chat/MutableComponent;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void vr$overrideReworkedDescription(Holder<Enchantment> enchantment, ResourceLocation id, int level,
                                               CallbackInfoReturnable<MutableComponent> callback) {
        MutableComponent replacement = EnchantmentDescriptionHelper.getOverride(id);
        if (replacement != null) callback.setReturnValue(replacement);
    }
}
