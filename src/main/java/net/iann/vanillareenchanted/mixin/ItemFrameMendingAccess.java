package net.iann.vanillareenchanted.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemFrame.class)
public interface ItemFrameMendingAccess {
    @Accessor("DATA_ITEM")
    static EntityDataAccessor<ItemStack> vr$itemData() {
        throw new AssertionError();
    }
}
