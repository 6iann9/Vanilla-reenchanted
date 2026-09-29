package net.iann.vanillareenchanted.event;

import net.iann.vanillareenchanted.enchantment.Channeling;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ChannelingEvents {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { Channeling.sync(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { Channeling.sync(event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { Channeling.sync(event.getEntity()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        Channeling.receive(event.getEntity(), Channeling.readyAt(event.getOriginal()));
    }
    private ChannelingEvents() {}
}
