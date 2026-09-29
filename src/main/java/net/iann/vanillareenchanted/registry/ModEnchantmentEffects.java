package net.iann.vanillareenchanted.registry;

import com.mojang.serialization.MapCodec;
import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.enchantment.WindBurstEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEnchantmentEffects {
    private static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> EFFECTS =
            DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, VanillaReenchanted.MODID);
    static { EFFECTS.register("wind_burst", () -> WindBurstEffect.CODEC); }
    static { EFFECTS.register("channeling", () -> net.iann.vanillareenchanted.enchantment.ChannelingEffect.CODEC); }
    public static void register(IEventBus bus) { EFFECTS.register(bus); }
}
