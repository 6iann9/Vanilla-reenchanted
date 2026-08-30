package net.iann.vanillareenchanted.registry;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.menu.ResearchTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    Registries.MENU,
                    VanillaReenchanted.MODID
            );

    public static final Supplier<MenuType<ResearchTableMenu>> RESEARCH_TABLE_MENU =
            MENUS.register(
                    "research_table",
                    () -> IMenuTypeExtension.create(ResearchTableMenu::new)
            );

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}