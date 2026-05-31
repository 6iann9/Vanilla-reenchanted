package net.iann.vanillareenchanted.registry;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(VanillaReenchanted.MODID);

    public static final DeferredItem<BlockItem> RESEARCH_TABLE =
            ITEMS.registerSimpleBlockItem(ModBlocks.RESEARCH_TABLE);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}