package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.client.BashAnimations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncBashPayload(int playerId, boolean offhand) implements CustomPacketPayload {
    public static final Type<SyncBashPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "bash_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncBashPayload::playerId, ByteBufCodecs.BOOL, SyncBashPayload::offhand, SyncBashPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncBashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BashAnimations.receive(payload));
    }
}
