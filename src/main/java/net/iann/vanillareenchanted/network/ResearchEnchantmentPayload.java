package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ResearchEnchantmentPayload(ResourceLocation enchantmentId) implements CustomPacketPayload {

    public static final Type<ResearchEnchantmentPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            VanillaReenchanted.MODID,
                            "research_enchantment"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchEnchantmentPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    ResearchEnchantmentPayload::enchantmentId,
                    ResearchEnchantmentPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}