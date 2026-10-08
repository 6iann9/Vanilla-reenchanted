package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.client.UpdraftEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdraftEffectPayload(int playerId, int action, double x, double y, double z) implements CustomPacketPayload {
    public static final int START = 0, FINISH = 1, STOP = 2, JUMP = 3;
    public UpdraftEffectPayload(int playerId, int action) { this(playerId, action, 0, 0, 0); }
    public static final Type<UpdraftEffectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "updraft_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdraftEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UpdraftEffectPayload::playerId,
            ByteBufCodecs.VAR_INT, UpdraftEffectPayload::action,
            ByteBufCodecs.DOUBLE, UpdraftEffectPayload::x,
            ByteBufCodecs.DOUBLE, UpdraftEffectPayload::y,
            ByteBufCodecs.DOUBLE, UpdraftEffectPayload::z, UpdraftEffectPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(UpdraftEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> UpdraftEffects.receive(payload));
    }
}
