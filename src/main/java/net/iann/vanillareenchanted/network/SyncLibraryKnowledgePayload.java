package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncLibraryKnowledgePayload(
        CompoundTag libraryTag
) implements CustomPacketPayload {

    public static final Type<SyncLibraryKnowledgePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            VanillaReenchanted.MODID,
                            "sync_library_knowledge"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLibraryKnowledgePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG,
                    SyncLibraryKnowledgePayload::libraryTag,
                    SyncLibraryKnowledgePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}