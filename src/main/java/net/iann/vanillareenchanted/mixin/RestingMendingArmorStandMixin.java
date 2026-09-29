package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.RestingMendingHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStand.class)
public abstract class RestingMendingArmorStandMixin {
    // Save loading populates the armor list directly, preserving its persisted timers.
    @Inject(method = "setItemSlot", at = @At("RETURN"))
    private void vr$beginResting(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            RestingMendingHelper.beginResting((ArmorStand)(Object)this, slot.getName(), stack);
        }
    }
}
