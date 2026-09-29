package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.event.MomentumEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncMomentumPayload(int horseId, boolean active) implements CustomPacketPayload {
    public static final Type<SyncMomentumPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "momentum_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMomentumPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncMomentumPayload::horseId,
                    ByteBufCodecs.BOOL, SyncMomentumPayload::active, SyncMomentumPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncMomentumPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var rider = context.player();
            if (rider.level().getEntity(payload.horseId()) instanceof AbstractHorse horse
                    && horse.getControllingPassenger() == rider) {
                MomentumEvents.receiveState(horse, rider, payload.active());
            }
        });
    }
}
