package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.HorseArmorCompatibility;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class HorseArmorStackMixin {
    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void vr$horseArmorEnchantable(CallbackInfoReturnable<Boolean> callback) {
        ItemStack stack = (ItemStack) (Object) this;
        if (HorseArmorCompatibility.isHorseArmor(stack)) {
            callback.setReturnValue(!stack.isEnchanted());
        }
    }

    // These NeoForge methods are normally inherited from IItemStackExtension.
    public boolean supportsEnchantment(Holder<Enchantment> enchantment) {
        ItemStack stack = (ItemStack) (Object) this;
        return stack.getItem().supportsEnchantment(stack, enchantment)
                || HorseArmorCompatibility.supports(stack, enchantment);
    }

    public boolean isPrimaryItemFor(Holder<Enchantment> enchantment) {
        ItemStack stack = (ItemStack) (Object) this;
        return stack.getItem().isPrimaryItemFor(stack, enchantment)
                || HorseArmorCompatibility.isHorseArmor(stack) && supportsEnchantment(enchantment);
    }

    public int getEnchantmentValue() {
        ItemStack stack = (ItemStack) (Object) this;
        int value = stack.getItem().getEnchantmentValue(stack);
        // Some custom horse armors deliberately inherit Item's zero value.
        return HorseArmorCompatibility.isHorseArmor(stack) ? Math.max(1, value) : value;
    }
}
