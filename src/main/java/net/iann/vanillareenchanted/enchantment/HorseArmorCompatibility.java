package net.iann.vanillareenchanted.enchantment;

import net.iann.vanillareenchanted.event.HighStepEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public final class HorseArmorCompatibility {
    private static final TagKey<Item> HORSE_ARMOR = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "horse_armor"));
    private static final TagKey<Item> COMMON_HORSE_ARMOR = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "armors/horse"));

    public static boolean isHorseArmor(ItemStack stack) {
        return isHorseArmor(stack.getItemHolder());
    }

    private static boolean isHorseArmor(Holder<Item> item) {
        return item.value() instanceof AnimalArmorItem armor
                && armor.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN
                || item.is(HORSE_ARMOR) || item.is(COMMON_HORSE_ARMOR);
    }

    public static boolean supports(ItemStack stack, Holder<Enchantment> enchantment) {
        if (!isHorseArmor(stack)) return false;
        if (enchantment.unwrapKey().map(key -> key.location().equals(HighStepEvents.HIGH_STEP)).orElse(false)) return true;
        // Read the enchantment's declared targets, not another stack's compatibility hook (no recursion).
        return enchantment.value().definition().supportedItems().stream()
                .anyMatch(HorseArmorCompatibility::isHorseArmor);
    }
}
