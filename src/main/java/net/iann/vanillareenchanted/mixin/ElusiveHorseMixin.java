package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.Elusive;
import net.iann.vanillareenchanted.enchantment.ElusiveWaterAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class ElusiveHorseMixin implements ElusiveWaterAccess {
    @Unique private static final EntityDataAccessor<Long> vr$waterDeadline =
            SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.LONG);
    @Unique private static final String vr$waterKey = "iannvanillareenchanted.ElusiveWaterDeadline";

    @Inject(method="defineSynchedData", at=@At("TAIL"))
    private void vr$defineWater(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(vr$waterDeadline, Elusive.READY);
    }

    @Override public long vr$elusiveDeadline() {
        return ((AbstractHorse)(Object)this).getEntityData().get(vr$waterDeadline);
    }

    @Override public void vr$elusiveDeadline(long deadline) {
        ((AbstractHorse)(Object)this).getEntityData().set(vr$waterDeadline, deadline);
    }

    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    private void vr$saveWater(CompoundTag tag, CallbackInfo ci) {
        tag.putLong(vr$waterKey, vr$elusiveDeadline());
    }

    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    private void vr$loadWater(CompoundTag tag, CallbackInfo ci) {
        vr$elusiveDeadline(tag.contains(vr$waterKey) ? tag.getLong(vr$waterKey) : Elusive.READY);
    }
}
