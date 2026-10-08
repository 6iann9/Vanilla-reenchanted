package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.client.UpdraftClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncUpdraftPayload(int stage, int time) implements CustomPacketPayload {
    public static final Type<SyncUpdraftPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "updraft_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncUpdraftPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncUpdraftPayload::stage, ByteBufCodecs.VAR_INT, SyncUpdraftPayload::time, SyncUpdraftPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncUpdraftPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> UpdraftClient.receive(payload));
    }
}
