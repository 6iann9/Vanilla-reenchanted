package net.iann.vanillareenchanted.event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

public class AnvilEvents {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty()) {
            return;
        }

        if (right.isEmpty()) {
            return;
        }

        if (isBlockedAnvilCombination(left, right)) {
            event.setCanceled(true);
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            event.setMaterialCost(0);
        }
    }

    private static boolean isBlockedAnvilCombination(
            ItemStack left,
            ItemStack right
    ) {
        if (isEnchantedBook(left) || isEnchantedBook(right)) {
            return true;
        }

        boolean leftEnchanted = hasEnchantments(left);
        boolean rightEnchanted = hasEnchantments(right);

        return leftEnchanted && rightEnchanted;
    }

    private static boolean isEnchantedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }

    private static boolean hasEnchantments(ItemStack stack) {
        return !stack.getEnchantments().isEmpty();
    }
}