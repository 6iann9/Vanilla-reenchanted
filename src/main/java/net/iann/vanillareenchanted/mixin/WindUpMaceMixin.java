package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.WindUp;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MaceItem.class)
public abstract class WindUpMaceMixin extends Item {
    private WindUpMaceMixin(Properties properties) { super(properties); }
    @Override public UseAnim getUseAnimation(ItemStack stack) {
        return WindUp.level(stack) > 0 ? UseAnim.NONE : super.getUseAnimation(stack);
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return WindUp.level(stack) > 0 ? WindUp.USE_DURATION : super.getUseDuration(stack, entity);
    }
    @Redirect(method = "canSmashAttack", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;fallDistance:F"))
    private static float vr$effectiveHeight(LivingEntity entity) { return WindUp.effectiveFallDistance(entity); }
    @Redirect(method = "getAttackDamageBonus", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;fallDistance:F"))
    private float vr$damageHeight(LivingEntity entity) { return WindUp.effectiveFallDistance(entity); }
    @Redirect(method = "hurtEnemy", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/level/ServerPlayer;fallDistance:F"))
    private float vr$smashSoundHeight(ServerPlayer player) { return WindUp.effectiveFallDistance(player); }
    @Redirect(method = "getKnockbackPower", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Player;fallDistance:F"))
    private static float vr$knockbackHeight(Player player) { return WindUp.effectiveFallDistance(player); }
}
