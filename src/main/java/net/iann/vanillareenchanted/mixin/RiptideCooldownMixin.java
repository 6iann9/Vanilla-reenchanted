package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.RiptideCooldown;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemCooldowns.class)
public abstract class RiptideCooldownMixin implements RiptideCooldown.Access {
    @Unique private final RiptideCooldown vr$riptide = new RiptideCooldown();
    public RiptideCooldown vr$riptideCooldown() { return vr$riptide; }
}
