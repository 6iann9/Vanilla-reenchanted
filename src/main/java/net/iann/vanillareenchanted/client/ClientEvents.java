package net.iann.vanillareenchanted.client;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.registry.ModMenus;
import net.iann.vanillareenchanted.screen.ResearchTableScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(
        modid = VanillaReenchanted.MODID,
        value = Dist.CLIENT
)
public class ClientEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
                ModMenus.RESEARCH_TABLE_MENU.get(),
                ResearchTableScreen::new
        );
    }
}