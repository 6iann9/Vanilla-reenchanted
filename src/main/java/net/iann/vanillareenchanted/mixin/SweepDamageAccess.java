package net.iann.vanillareenchanted.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Player.class)
public interface SweepDamageAccess {
    @Invoker("getEnchantedDamage")
    float vr$enchantedSweepDamage(Entity target, float damage, DamageSource source);
}
