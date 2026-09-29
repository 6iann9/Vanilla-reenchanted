package net.iann.vanillareenchanted.event;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class HighStepEvents {
    public static final ResourceLocation HIGH_STEP = ResourceLocation.fromNamespaceAndPath(
            VanillaReenchanted.MODID, "high_step");
    public static final double PLAYER_STEP_HEIGHT = 1.0;
    public static final double HORSE_STEP_HEIGHT = 2.0;

    private static boolean enchanted(ItemStack stack) {
        return stack.getEnchantments().entrySet().stream().anyMatch(entry ->
                entry.getIntValue() > 0 && entry.getKey().unwrapKey()
                        .map(key -> key.location().equals(HIGH_STEP)).orElse(false));
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        double target;
        if (entity instanceof Player player) {
            target = player.isAlive() && player.isSprinting() && !player.isPassenger()
                    && enchanted(player.getItemBySlot(EquipmentSlot.LEGS)) ? PLAYER_STEP_HEIGHT : 0;
        } else if (entity instanceof AbstractHorse horse) {
            target = horse.isAlive() && horse.isVehicle()
                    && enchanted(horse.getItemBySlot(EquipmentSlot.BODY)) ? HORSE_STEP_HEIGHT : 0;
        } else return;
        var step = entity.getAttribute(Attributes.STEP_HEIGHT);
        if (step == null) return;
        // Run on both sides so the locally controlled player's/horse's movement responds immediately.
        // Calculate a floor while preserving other mods' attribute modifiers and higher step heights.
        double additive = step.getBaseValue();
        double baseMultiplier = 0;
        double totalMultiplier = 1;
        for (var modifier : step.getModifiers()) {
            if (modifier.id().equals(HIGH_STEP)) continue;
            switch (modifier.operation()) {
                case ADD_VALUE -> additive += modifier.amount();
                case ADD_MULTIPLIED_BASE -> baseMultiplier += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> totalMultiplier *= 1 + modifier.amount();
            }
        }
        double multiplier = (1 + baseMultiplier) * totalMultiplier;
        double needed = target > 0 && multiplier > 0 ? Math.max(0, target / multiplier - additive) : 0;
        var current = step.getModifier(HIGH_STEP);
        if (needed <= 0) {
            if (current != null) step.removeModifier(HIGH_STEP);
        } else if (current == null || Math.abs(current.amount() - needed) > 1.0E-7) {
            step.removeModifier(HIGH_STEP);
            step.addTransientModifier(new AttributeModifier(HIGH_STEP, needed, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
