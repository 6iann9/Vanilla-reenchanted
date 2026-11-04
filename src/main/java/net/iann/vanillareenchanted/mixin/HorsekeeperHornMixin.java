package net.iann.vanillareenchanted.mixin;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.iann.vanillareenchanted.enchantment.Horsekeeper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InstrumentItem.class)
public abstract class HorsekeeperHornMixin {
    public int getEnchantmentValue() { return 1; }

    // Horns have no durability, so Item's default enchantability check rejects them.
    public boolean isEnchantable(ItemStack stack) { return stack.getCount() == 1; }

    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void vr$horsekeeperTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag, CallbackInfo callback) {
        Horsekeeper.appendTooltip(stack, tooltip);
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void vr$horsekeeper(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        if (player instanceof ServerPlayer server && callback.getReturnValue().getResult().consumesAction()) {
            Horsekeeper.blow(server, player.getItemInHand(hand));
        }
    }
}
