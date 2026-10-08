package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.client.HorsekeeperEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HorsekeeperEffectPayload(int action, double x, double y, double z) implements CustomPacketPayload {
    public static final int DESPAWN = 0, PRESPAWN = 1, SPAWN = 2;
    public static final Type<HorsekeeperEffectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "horsekeeper_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HorsekeeperEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HorsekeeperEffectPayload::action,
            ByteBufCodecs.DOUBLE, HorsekeeperEffectPayload::x,
            ByteBufCodecs.DOUBLE, HorsekeeperEffectPayload::y,
            ByteBufCodecs.DOUBLE, HorsekeeperEffectPayload::z, HorsekeeperEffectPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(HorsekeeperEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> HorsekeeperEffects.receive(payload));
    }
}
