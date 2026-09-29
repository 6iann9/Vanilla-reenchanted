package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncProtectionShieldPayload(float current, float capacity) implements CustomPacketPayload {
    public static final Type<SyncProtectionShieldPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VanillaReenchanted.MODID, "protection_shield"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncProtectionShieldPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, SyncProtectionShieldPayload::current,
                    ByteBufCodecs.FLOAT, SyncProtectionShieldPayload::capacity, SyncProtectionShieldPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncProtectionShieldPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> context.player().getData(ModAttachments.PROTECTION_SHIELD)
                .applySnapshot(payload.current(), payload.capacity()));
    }
}
