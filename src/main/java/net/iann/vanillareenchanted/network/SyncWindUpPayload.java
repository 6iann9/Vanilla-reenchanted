package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.client.WindUpAnimations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authored visual state; never used to calculate damage. */
public record SyncWindUpPayload(int playerId, int action, int elapsed) implements CustomPacketPayload {
    public static final Type<SyncWindUpPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "wind_up_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWindUpPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncWindUpPayload::playerId,
                    ByteBufCodecs.VAR_INT, SyncWindUpPayload::action,
                    ByteBufCodecs.VAR_INT, SyncWindUpPayload::elapsed, SyncWindUpPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncWindUpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WindUpAnimations.receive(payload));
    }
}
