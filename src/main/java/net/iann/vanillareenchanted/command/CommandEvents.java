package net.iann.vanillareenchanted.command;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = VanillaReenchanted.MODID)
public class CommandEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        VRCommands.register(event.getDispatcher());
    }
}