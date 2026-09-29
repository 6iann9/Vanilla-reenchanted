package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.event.WindUpEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WindUpPayload(int action) implements CustomPacketPayload {
    public static final int START = 0, RELEASE = 1, CANCEL = 2;
    public static final Type<WindUpPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "wind_up"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WindUpPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, WindUpPayload::action, WindUpPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(WindUpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action) {
                case START -> WindUpEvents.begin(player);
                case RELEASE -> WindUpEvents.release(player);
                case CANCEL -> WindUpEvents.cancel(player);
                default -> { }
            }
        });
    }
}
