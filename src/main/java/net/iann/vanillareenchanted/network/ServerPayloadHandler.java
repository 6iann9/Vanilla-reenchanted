package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.cost.EnchantmentCostCalculator;
import net.iann.vanillareenchanted.enchantment.EnchantedBookResearchHelper;
import net.iann.vanillareenchanted.enchantment.PlayerKnowledgeData;
import net.iann.vanillareenchanted.menu.ResearchTableMenu;
import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public class ServerPayloadHandler {

    public static void handleResearchEnchantment(
            ResearchEnchantmentPayload payload,
            IPayloadContext context
    ) {
        Player player = context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!(serverPlayer.containerMenu instanceof ResearchTableMenu researchTableMenu)) {
            return;
        }

        ResourceLocation enchantmentId = payload.enchantmentId();

        Registry<Enchantment> enchantmentRegistry =
                serverPlayer.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        ResourceKey<Enchantment> enchantmentKey =
                ResourceKey.create(Registries.ENCHANTMENT, enchantmentId);

        Optional<Holder.Reference<Enchantment>> optionalEnchantment =
                enchantmentRegistry.getHolder(enchantmentKey);

        if (optionalEnchantment.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Unknown enchantment: " + enchantmentId)
            );

            return;
        }

        Holder<Enchantment> enchantmentHolder = optionalEnchantment.get();

        PlayerKnowledgeData knowledge =
                serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);

        int currentLevel = knowledge.getLevel(enchantmentId);
        int maxLevel = Math.max(1, enchantmentHolder.value().getMaxLevel());

        if (currentLevel >= maxLevel) {
            serverPlayer.sendSystemMessage(
                    Component.literal("This enchantment is already fully researched.")
            );

            KnowledgeSync.sendToClient(serverPlayer);
            return;
        }

        int targetLevel = currentLevel + 1;

        ItemStack paymentItem = researchTableMenu.getPaymentItem();

        boolean hasMatchingBook =
                EnchantedBookResearchHelper.hasBookForResearchLevel(
                        paymentItem,
                        enchantmentHolder,
                        targetLevel
                );

        boolean isTreasure =
                EnchantedBookResearchHelper.isTreasureEnchantment(enchantmentHolder);

        if (hasMatchingBook) {
            if (!serverPlayer.isCreative()) {
                boolean consumedBook = researchTableMenu.consumePaymentBook();

                if (!consumedBook) {
                    serverPlayer.sendSystemMessage(
                            Component.literal("Could not consume enchanted book.")
                    );

                    return;
                }
            }
        } else {
            if (isTreasure) {
                serverPlayer.sendSystemMessage(
                        Component.literal("Treasure enchantments require a matching enchanted book.")
                );

                return;
            }

            int xpCost = EnchantmentCostCalculator.getResearchXpCost(
                    enchantmentHolder,
                    targetLevel
            );

            if (!serverPlayer.isCreative()) {
                if (serverPlayer.experienceLevel < xpCost) {
                    serverPlayer.sendSystemMessage(
                            Component.literal("Not enough XP levels. Need " + xpCost + ".")
                    );

                    return;
                }

                serverPlayer.giveExperienceLevels(-xpCost);
            }
        }

        knowledge.setLevel(enchantmentId, targetLevel);

        KnowledgeSync.sendToClient(serverPlayer);

        serverPlayer.playNotifySound(
                SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                SoundSource.PLAYERS,
                0.8F,
                1.2F
        );

        if (hasMatchingBook) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Researched " + enchantmentId + " level " + targetLevel + " using a book.")
            );
        } else {
            serverPlayer.sendSystemMessage(
                    Component.literal("Researched " + enchantmentId + " level " + targetLevel)
            );
        }
    }

    public static void handleEnchantItem(
            EnchantItemPayload payload,
            IPayloadContext context
    ) {
        Player player = context.player();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!(serverPlayer.containerMenu instanceof ResearchTableMenu researchTableMenu)) {
            return;
        }

        ResourceLocation enchantmentId = payload.enchantmentId();

        Registry<Enchantment> enchantmentRegistry =
                serverPlayer.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        ResourceKey<Enchantment> enchantmentKey =
                ResourceKey.create(Registries.ENCHANTMENT, enchantmentId);

        Optional<Holder.Reference<Enchantment>> optionalEnchantment =
                enchantmentRegistry.getHolder(enchantmentKey);

        if (optionalEnchantment.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Unknown enchantment: " + enchantmentId)
            );

            return;
        }

        Holder<Enchantment> enchantmentHolder = optionalEnchantment.get();

        ItemStack researchItem = researchTableMenu.getResearchItem();

        if (researchItem.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Insert an item first.")
            );

            return;
        }

        if (!researchItem.supportsEnchantment(enchantmentHolder)) {
            serverPlayer.sendSystemMessage(
                    Component.literal("This enchantment cannot be applied to this item.")
            );

            return;
        }

        if (hasIncompatibleEnchantment(researchItem, enchantmentHolder)) {
            serverPlayer.sendSystemMessage(
                    Component.literal("This item already has an incompatible enchantment.")
            );

            return;
        }

        PlayerKnowledgeData knowledge =
                serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);

        int knowledgeLevel = knowledge.getLevel(enchantmentId);
        int itemLevel = researchItem.getEnchantmentLevel(enchantmentHolder);
        int maxLevel = Math.max(1, enchantmentHolder.value().getMaxLevel());

        if (itemLevel >= maxLevel) {
            serverPlayer.sendSystemMessage(
                    Component.literal("This item already has the maximum level.")
            );

            return;
        }

        if (knowledgeLevel <= itemLevel) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Research this enchantment first.")
            );

            return;
        }

        int targetLevel = itemLevel + 1;

        int lapisCost = EnchantmentCostCalculator.getEnchantLapisCost(
                enchantmentHolder,
                targetLevel
        );

        if (!serverPlayer.isCreative()) {
            boolean consumed = researchTableMenu.consumeLapis(lapisCost);

            if (!consumed) {
                serverPlayer.sendSystemMessage(
                        Component.literal("Not enough lapis. Need " + lapisCost + ".")
                );

                return;
            }
        }

        setEnchantmentLevel(
                researchItem,
                enchantmentHolder,
                targetLevel
        );

        researchTableMenu.markResearchItemChanged();

        serverPlayer.playNotifySound(
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        serverPlayer.sendSystemMessage(
                Component.literal("Applied " + enchantmentId + " level " + targetLevel)
        );
    }

    private static boolean hasIncompatibleEnchantment(
            ItemStack stack,
            Holder<Enchantment> newEnchantment
    ) {
        ItemEnchantments existingEnchantments =
                EnchantmentHelper.getEnchantmentsForCrafting(stack);

        for (Holder<Enchantment> existingEnchantment : existingEnchantments.keySet()) {
            if (existingEnchantment.equals(newEnchantment)) {
                continue;
            }

            if (!Enchantment.areCompatible(existingEnchantment, newEnchantment)) {
                return true;
            }
        }

        return false;
    }

    private static void setEnchantmentLevel(
            ItemStack stack,
            Holder<Enchantment> enchantmentHolder,
            int level
    ) {
        ItemEnchantments.Mutable mutableEnchantments =
                new ItemEnchantments.Mutable(
                        EnchantmentHelper.getEnchantmentsForCrafting(stack)
                );

        mutableEnchantments.set(enchantmentHolder, level);

        EnchantmentHelper.setEnchantments(
                stack,
                mutableEnchantments.toImmutable()
        );
    }
}