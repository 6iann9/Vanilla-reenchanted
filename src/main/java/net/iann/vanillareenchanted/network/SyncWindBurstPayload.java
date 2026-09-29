package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.enchantment.WindBurst;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncWindBurstPayload(boolean waiting, int remaining, boolean recoverAttack) implements CustomPacketPayload {
    public static final Type<SyncWindBurstPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "wind_burst"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWindBurstPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncWindBurstPayload::waiting, ByteBufCodecs.VAR_INT, SyncWindBurstPayload::remaining,
            ByteBufCodecs.BOOL, SyncWindBurstPayload::recoverAttack, SyncWindBurstPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncWindBurstPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WindBurst.receive(context.player(), payload.waiting(), payload.remaining(), payload.recoverAttack()));
    }
}
