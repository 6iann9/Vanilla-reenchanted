package net.iann.vanillareenchanted.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface MomentumCollisionAccess {
    @Invoker("collide")
    Vec3 vr$previewCollision(Vec3 movement);
}
