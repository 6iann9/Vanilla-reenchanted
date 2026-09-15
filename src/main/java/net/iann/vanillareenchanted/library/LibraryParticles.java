package net.iann.vanillareenchanted.library;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

import java.util.List;

public final class LibraryParticles {
    private static final int EMISSION_INTERVAL_TICKS = 40;
    private static final int MAX_PARTICLES_PER_BURST = 1;
    private static final double VIEW_DISTANCE = 32.0D;
    private static final double SHELF_FRONT_OFFSET = 0.51D;
    private static final double SHELF_SPREAD = 0.3D;

    private LibraryParticles() {
    }

    public static void tick(ServerLevel level, BlockPos tablePos) {
        // Stagger tables and avoid scanning libraries when nobody can see them.
        if (Math.floorMod(level.getGameTime() + tablePos.asLong(), EMISSION_INTERVAL_TICKS) != 0
                || !level.hasNearbyAlivePlayer(tablePos.getX() + 0.5D,
                tablePos.getY() + 0.5D, tablePos.getZ() + 0.5D, VIEW_DISTANCE)) {
            return;
        }

        // Check actual server inventories; ordinary books and empty shelves emit nothing.
        List<BlockPos> shelves = LibraryScanner.scan(level, tablePos, List.of())
                .getEnchantedBookSlots().stream()
                .map(LibraryScanner.LibraryBookSlot::shelfPos)
                .distinct().toList();
        if (shelves.isEmpty()) {
            return;
        }

        int start = level.random.nextInt(shelves.size());
        for (int i = 0; i < Math.min(shelves.size(), MAX_PARTICLES_PER_BURST); i++) {
            BlockPos shelfPos = shelves.get((start + i) % shelves.size());
            Direction facing = level.getBlockState(shelfPos).getValue(HorizontalDirectionalBlock.FACING);
            Direction side = facing.getClockWise();
            double sideways = (level.random.nextDouble() * 2.0D - 1.0D) * SHELF_SPREAD;
            double sourceX = shelfPos.getX() + 0.5D + facing.getStepX() * SHELF_FRONT_OFFSET
                    + side.getStepX() * sideways;
            double sourceY = shelfPos.getY() + 0.5D
                    + (level.random.nextDouble() * 2.0D - 1.0D) * SHELF_SPREAD;
            double sourceZ = shelfPos.getZ() + 0.5D + facing.getStepZ() * SHELF_FRONT_OFFSET
                    + side.getStepZ() * sideways;

            // ENCHANT interprets velocity as the source offset, then arcs toward the book.
            // Match vanilla's table anchor; count 0 sends one particle with exact offsets.
            double anchorX = tablePos.getX() + 0.5D;
            double anchorY = tablePos.getY() + 2.0D;
            double anchorZ = tablePos.getZ() + 0.5D;
            level.sendParticles(ParticleTypes.ENCHANT, anchorX, anchorY, anchorZ, 0,
                    sourceX - anchorX, sourceY - anchorY, sourceZ - anchorZ, 1.0D);
        }
    }
}