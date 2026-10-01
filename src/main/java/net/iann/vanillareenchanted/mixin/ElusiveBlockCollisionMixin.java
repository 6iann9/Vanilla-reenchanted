package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.enchantment.Elusive;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ElusiveBlockCollisionMixin {
    @Inject(method="getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at=@At("HEAD"), cancellable=true)
    private void vr$elusiveCollision(BlockGetter level, BlockPos pos, CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (!(context instanceof EntityCollisionContext entityContext)) return;
        var entity = entityContext.getEntity();
        var state = (BlockState)(Object)this;
        if (state.is(BlockTags.LEAVES) && Elusive.phasesLeaves(entity)) {
            cir.setReturnValue(Shapes.empty());
        } else if (state.getBlock() instanceof LiquidBlock && entity instanceof AbstractHorse horse
                && state.getFluidState().is(FluidTags.WATER) && Elusive.waterAvailable(horse)
                && !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            double height = state.getFluidState().getHeight(level, pos);
            VoxelShape surface = Shapes.box(0, 0, 0, 1, height, 1);
            if (context.isAbove(surface, pos, true)) cir.setReturnValue(surface);
        }
    }
}
