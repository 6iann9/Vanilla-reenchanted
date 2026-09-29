package net.iann.vanillareenchanted.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface WindBurstAttackAccess {
    @Accessor("attackStrengthTicker") int vr$getAttackStrengthTicker();
    @Accessor("attackStrengthTicker") void vr$setAttackStrengthTicker(int ticks);
}
