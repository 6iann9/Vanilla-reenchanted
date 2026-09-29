package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.enchantment.WindUp;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncWindUpCooldownPayload(int remaining, int duration) implements CustomPacketPayload {
    public static final Type<SyncWindUpCooldownPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "wind_up_cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWindUpCooldownPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncWindUpCooldownPayload::remaining,
                    ByteBufCodecs.VAR_INT, SyncWindUpCooldownPayload::duration, SyncWindUpCooldownPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncWindUpCooldownPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WindUp.applyCooldown(context.player(), payload.remaining(), payload.duration()));
    }
}
