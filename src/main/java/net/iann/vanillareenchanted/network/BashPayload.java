package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.enchantment.Bash;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BashPayload() implements CustomPacketPayload {
    public static final BashPayload INSTANCE = new BashPayload();
    public static final Type<BashPayload> TYPE = new Type<>(Bash.ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BashPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(BashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) Bash.begin(player); });
    }
}
