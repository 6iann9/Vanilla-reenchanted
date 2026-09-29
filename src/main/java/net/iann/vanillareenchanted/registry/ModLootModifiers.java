package net.iann.vanillareenchanted.registry;

import com.mojang.serialization.MapCodec;
import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.loot.NormalizeEnchantedBooksLootModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(
                    NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    VanillaReenchanted.MODID
            );

    public static final Supplier<MapCodec<NormalizeEnchantedBooksLootModifier>> NORMALIZE_ENCHANTED_BOOKS =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(
                    "normalize_enchanted_books",
                    () -> NormalizeEnchantedBooksLootModifier.CODEC
            );

    public static final Supplier<MapCodec<net.iann.vanillareenchanted.loot.EchoingEdgeLootModifier>> ECHOING_EDGE =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("echoing_edge", () -> net.iann.vanillareenchanted.loot.EchoingEdgeLootModifier.CODEC);

    public static void register(IEventBus eventBus) {
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(eventBus);
    }
}