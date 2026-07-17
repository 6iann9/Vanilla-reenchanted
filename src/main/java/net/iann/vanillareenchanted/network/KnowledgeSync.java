package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.enchantment.PlayerKnowledgeData;
import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class KnowledgeSync {

    public static void sendToClient(ServerPlayer serverPlayer) {
        PlayerKnowledgeData knowledge =
                serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);

        CompoundTag tag = knowledge.serializeNBT(
                serverPlayer.registryAccess()
        );

        PacketDistributor.sendToPlayer(
                serverPlayer,
                new SyncPlayerKnowledgePayload(tag)
        );
    }
}