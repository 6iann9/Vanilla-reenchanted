package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.RestingMendingHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrame.class)
public abstract class RestingMendingItemFrameMixin {
    @Unique private boolean vr$loadingRestingItem;

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void vr$beginLoad(CompoundTag tag, CallbackInfo ci) {
        vr$loadingRestingItem = true;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void vr$endLoad(CompoundTag tag, CallbackInfo ci) {
        vr$loadingRestingItem = false;
    }

    @Inject(method = "setItem(Lnet/minecraft/world/item/ItemStack;Z)V", at = @At("RETURN"))
    private void vr$beginResting(ItemStack stack, boolean updateNeighbours, CallbackInfo ci) {
        if (!vr$loadingRestingItem) {
            var frame = (ItemFrame)(Object)this;
            RestingMendingHelper.beginResting(frame, "item", frame.getItem());
        }
    }
}
