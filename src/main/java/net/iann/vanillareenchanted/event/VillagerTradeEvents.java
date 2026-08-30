package net.iann.vanillareenchanted.event;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

public class VillagerTradeEvents {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.LIBRARIAN) {
            return;
        }

        List<VillagerTrades.ItemListing> noviceTrades = event.getTrades().get(1);

        if (noviceTrades == null || noviceTrades.isEmpty()) {
            return;
        }

        int before = noviceTrades.size();

        noviceTrades.removeIf(trade ->
                trade instanceof VillagerTrades.EnchantBookForEmeralds
        );

        int removed = before - noviceTrades.size();

        if (removed > 0) {
            VanillaReenchanted.LOGGER.info(
                    "Removed {} novice enchanted book trade(s) from librarian.",
                    removed
            );
        }
    }
}