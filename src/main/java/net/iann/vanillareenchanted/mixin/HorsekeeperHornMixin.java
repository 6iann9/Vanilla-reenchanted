package net.iann.vanillareenchanted.mixin;

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

    @Inject(method = "use", at = @At("RETURN"))
    private void vr$horsekeeper(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        if (player instanceof ServerPlayer server && callback.getReturnValue().getResult().consumesAction()) {
            Horsekeeper.blow(server, player.getItemInHand(hand));
        }
    }
}
