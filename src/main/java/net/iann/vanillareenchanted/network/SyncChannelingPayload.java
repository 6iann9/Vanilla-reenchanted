package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.enchantment.Channeling;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncChannelingPayload(long readyAt) implements CustomPacketPayload {
    public static final Type<SyncChannelingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "channeling"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncChannelingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SyncChannelingPayload::readyAt, SyncChannelingPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncChannelingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Channeling.receive(context.player(), payload.readyAt()));
    }
}
