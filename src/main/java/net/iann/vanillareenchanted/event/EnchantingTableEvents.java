package net.iann.vanillareenchanted.event;

import net.iann.vanillareenchanted.menu.ResearchTableMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class EnchantingTableEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);

        if (!blockState.is(Blocks.ENCHANTING_TABLE)) {
            return;
        }

        // Stop vanilla enchanting table behavior.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (level.isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.openMenu(
                new ResearchTableMenuProvider(pos),
                buffer -> buffer.writeBlockPos(pos)
        );
    }
}