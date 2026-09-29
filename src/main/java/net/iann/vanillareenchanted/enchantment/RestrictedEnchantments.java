package net.iann.vanillareenchanted.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Exclude selected enchantments from research; existing items retain vanilla behavior. */
public final class RestrictedEnchantments {
    private static final Set<ResourceLocation> IDS = Stream.of(
            "fire_protection", "blast_protection", "projectile_protection",
            "smite", "bane_of_arthropods", "impaling", "breach", "fire_aspect", "flame", "thorns")
            .map(ResourceLocation::withDefaultNamespace).collect(Collectors.toUnmodifiableSet());

    private RestrictedEnchantments() {}

    public static boolean isRestricted(ResourceLocation id) { return IDS.contains(id); }

    public static boolean isRestricted(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(key -> isRestricted(key.location())).orElse(false);
    }

}
