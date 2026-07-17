package net.iann.vanillareenchanted.attachment;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.network.KnowledgeSync;
import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = VanillaReenchanted.MODID)
public class AttachmentEvents {

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);

        KnowledgeSync.sendToClient(serverPlayer);
    }
}