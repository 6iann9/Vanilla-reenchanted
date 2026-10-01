package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkEvents {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VanillaReenchanted.MODID);
        registrar.playToServer(SweepMissPayload.TYPE, SweepMissPayload.STREAM_CODEC, SweepMissPayload::handle);
        registrar.playToClient(SyncChannelingPayload.TYPE, SyncChannelingPayload.STREAM_CODEC, SyncChannelingPayload::handle);
        registrar.playToClient(SyncRiptidePayload.TYPE, SyncRiptidePayload.STREAM_CODEC, SyncRiptidePayload::handle);
        registrar.playToClient(SyncWindBurstPayload.TYPE, SyncWindBurstPayload.STREAM_CODEC, SyncWindBurstPayload::handle);
        registrar.playToClient(SyncWindUpCooldownPayload.TYPE, SyncWindUpCooldownPayload.STREAM_CODEC, SyncWindUpCooldownPayload::handle);
        registrar.playToClient(SyncWindUpPayload.TYPE, SyncWindUpPayload.STREAM_CODEC, SyncWindUpPayload::handle);
        registrar.playToServer(WindUpPayload.TYPE, WindUpPayload.STREAM_CODEC, WindUpPayload::handle);
        registrar.playToClient(SyncMomentumPayload.TYPE, SyncMomentumPayload.STREAM_CODEC, SyncMomentumPayload::handle);

        registrar.playToClient(SyncProtectionShieldPayload.TYPE, SyncProtectionShieldPayload.STREAM_CODEC,
                SyncProtectionShieldPayload::handle);

        registrar.playToClient(
                SyncPlayerKnowledgePayload.TYPE,
                SyncPlayerKnowledgePayload.STREAM_CODEC,
                ClientPayloadHandler::handleSyncPlayerKnowledge
        );
        registrar.playToServer(
                ResearchEnchantmentPayload.TYPE,
                ResearchEnchantmentPayload.STREAM_CODEC,
                ServerPayloadHandler::handleResearchEnchantment
        );
        registrar.playToServer(
                EnchantItemPayload.TYPE,
                EnchantItemPayload.STREAM_CODEC,
                ServerPayloadHandler::handleEnchantItem
        );
        registrar.playToClient(
                SyncLibraryKnowledgePayload.TYPE,
                SyncLibraryKnowledgePayload.STREAM_CODEC,
                ClientPayloadHandler::handleSyncLibraryKnowledge
        );
    }
}
