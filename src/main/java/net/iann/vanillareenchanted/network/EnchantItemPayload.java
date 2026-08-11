package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EnchantItemPayload(ResourceLocation enchantmentId) implements CustomPacketPayload {

    public static final Type<EnchantItemPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            VanillaReenchanted.MODID,
                            "enchant_item"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantItemPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    EnchantItemPayload::enchantmentId,
                    EnchantItemPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}