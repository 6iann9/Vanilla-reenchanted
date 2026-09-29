package net.iann.vanillareenchanted.event;

import net.iann.vanillareenchanted.enchantment.WindBurst;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class WindBurstEvents {
    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) WindBurst.tick(player);
    }
}
