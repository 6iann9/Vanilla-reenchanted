package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.event.RiptideEvents;
import net.iann.vanillareenchanted.enchantment.RiptideSpinAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncRiptidePayload(int remaining, boolean waiting, boolean spinning) implements CustomPacketPayload {
    public static final Type<SyncRiptidePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "riptide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRiptidePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncRiptidePayload::remaining, ByteBufCodecs.BOOL, SyncRiptidePayload::waiting,
            ByteBufCodecs.BOOL, SyncRiptidePayload::spinning, SyncRiptidePayload::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncRiptidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var state = RiptideEvents.data(context.player());
            boolean wasSpinning = state.spinning;
            state.remaining = Math.clamp(payload.remaining(), 0, net.iann.vanillareenchanted.enchantment.RiptideCooldown.DURATION);
            state.waiting = payload.waiting();
            state.spinning = payload.spinning();
            if (wasSpinning && !payload.spinning()) ((RiptideSpinAccess)context.player()).vr$stopSpin();
        });
    }
}
