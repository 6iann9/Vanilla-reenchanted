package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkEvents {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VanillaReenchanted.MODID);

        registrar.playToClient(
                SyncPlayerKnowledgePayload.TYPE,
                SyncPlayerKnowledgePayload.STREAM_CODEC,
                ClientPayloadHandler::handleSyncPlayerKnowledge
        );
    }
}