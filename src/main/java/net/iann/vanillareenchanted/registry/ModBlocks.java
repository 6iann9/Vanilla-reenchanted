package net.iann.vanillareenchanted.registry;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.block.ResearchTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(VanillaReenchanted.MODID);

    public static final DeferredBlock<Block> RESEARCH_TABLE =
            BLOCKS.register(
                    "research_table",
                    () -> new ResearchTableBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .strength(2.5F)
                                    .requiresCorrectToolForDrops()
                    )
            );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}