package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.enchantment.AimedSweep;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SweepMissPayload() implements CustomPacketPayload {
    public static final SweepMissPayload INSTANCE = new SweepMissPayload();
    public static final Type<SweepMissPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "sweep_miss"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SweepMissPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SweepMissPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) AimedSweep.miss(player);
        });
    }
}
