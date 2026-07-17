package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncPlayerKnowledgePayload(CompoundTag knowledgeTag) implements CustomPacketPayload {

    public static final Type<SyncPlayerKnowledgePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            VanillaReenchanted.MODID,
                            "sync_player_knowledge"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerKnowledgePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG,
                    SyncPlayerKnowledgePayload::knowledgeTag,
                    SyncPlayerKnowledgePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}