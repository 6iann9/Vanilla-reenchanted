package net.iann.vanillareenchanted.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.iann.vanillareenchanted.client.BashAnimations;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemOverrides.class)
public abstract class BashShieldModelMixin {
    @WrapOperation(method="resolve", at=@At(value="INVOKE",
            target="Lnet/minecraft/client/renderer/item/ItemProperties;getProperty(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/item/ItemPropertyFunction;"))
    private ItemPropertyFunction vr$bashOwnsShieldTransform(ItemStack stack, ResourceLocation property,
            Operation<ItemPropertyFunction> original) {
        ItemPropertyFunction predicate = original.call(stack, property);
        if (predicate == null || !property.equals(ResourceLocation.withDefaultNamespace("blocking"))) return predicate;
        // Only change model selection. The living entity remains genuinely blocking on both sides.
        return (item, level, entity, seed) -> BashAnimations.animatingShield(entity, item)
                ? 0F : predicate.call(item, level, entity, seed);
    }
}
