package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.enchantment.Updraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdraftPayload(int action) implements CustomPacketPayload {
    public static final Type<UpdraftPayload> TYPE = new Type<>(Updraft.ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdraftPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, UpdraftPayload::action, UpdraftPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(UpdraftPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) Updraft.input(player, payload.action()); });
    }
}
