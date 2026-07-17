package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleSyncPlayerKnowledge(
            SyncPlayerKnowledgePayload payload,
            IPayloadContext context
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        minecraft.player
                .getData(ModAttachments.PLAYER_KNOWLEDGE)
                .deserializeNBT(
                        minecraft.player.registryAccess(),
                        payload.knowledgeTag()
                );
    }
}