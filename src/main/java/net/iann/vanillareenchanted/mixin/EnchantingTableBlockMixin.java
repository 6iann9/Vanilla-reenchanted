package net.iann.vanillareenchanted.mixin;

import net.iann.vanillareenchanted.library.LibraryParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin {
    @Inject(method = "getTicker", at = @At("RETURN"), cancellable = true)
    private <T extends BlockEntity> void vr$addLibraryParticles(
            Level level, BlockState state, BlockEntityType<T> type,
            CallbackInfoReturnable<BlockEntityTicker<T>> callback) {
        if (!(level instanceof ServerLevel) || type != BlockEntityType.ENCHANTING_TABLE) {
            return;
        }
        BlockEntityTicker<T> original = callback.getReturnValue();
        callback.setReturnValue((tickLevel, pos, tickState, blockEntity) -> {
            if (original != null) {
                original.tick(tickLevel, pos, tickState, blockEntity);
            }
            LibraryParticles.tick((ServerLevel) tickLevel, pos);
        });
    }
}