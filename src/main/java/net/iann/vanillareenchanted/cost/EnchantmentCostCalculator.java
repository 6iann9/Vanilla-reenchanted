package net.iann.vanillareenchanted.cost;

import net.iann.vanillareenchanted.config.VRConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentCostCalculator {

    public static int getResearchXpCost(
            Holder<Enchantment> enchantmentHolder,
            int targetLevel
    ) {
        ResourceLocation enchantmentId = getEnchantmentId(enchantmentHolder);

        String formula = getFormulaForEnchantment(
                enchantmentId,
                "research",
                VRConfig.DEFAULT_RESEARCH_XP_FORMULA.get()
        );

        return evaluateFormula(formula, targetLevel);
    }

    public static int getEnchantLapisCost(
            Holder<Enchantment> enchantmentHolder,
            int targetLevel
    ) {
        ResourceLocation enchantmentId = getEnchantmentId(enchantmentHolder);

        String formula = getFormulaForEnchantment(
                enchantmentId,
                "lapis",
                VRConfig.DEFAULT_ENCHANT_LAPIS_FORMULA.get()
        );

        return evaluateFormula(formula, targetLevel);
    }

    private static String getFormulaForEnchantment(
            ResourceLocation enchantmentId,
            String formulaType,
            String defaultFormula
    ) {
        if (enchantmentId == null) {
            return defaultFormula;
        }

        String enchantmentIdText = enchantmentId.toString();

        for (String entry : VRConfig.ENCHANTMENT_FORMULAS.get()) {
            String[] parts = entry.split("\\|");

            if (parts.length < 2) {
                continue;
            }

            String entryEnchantmentId = parts[0].trim();

            if (!entryEnchantmentId.equals(enchantmentIdText)) {
                continue;
            }

            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();

                String prefix = formulaType + "=";

                if (part.startsWith(prefix)) {
                    return part.substring(prefix.length()).trim();
                }
            }
        }

        return defaultFormula;
    }

    private static ResourceLocation getEnchantmentId(
            Holder<Enchantment> enchantmentHolder
    ) {
        return enchantmentHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
    }

    private static int evaluateFormula(String formula, int level) {
        String cleanedFormula = formula.replace(" ", "");

        try {
            return Math.max(1, parseSimpleFormula(cleanedFormula, level));
        } catch (Exception exception) {
            return 1;
        }
    }

    // Simple formula parser.
    // Supports:
    // level*3+2
    // level*5
    // level+10
    // 10
    private static int parseSimpleFormula(String formula, int level) {
        formula = formula.replace("level", String.valueOf(level));

        int result = 0;
        String[] additionParts = formula.split("\\+");

        for (String additionPart : additionParts) {
            int multiplicationResult = 1;
            String[] multiplicationParts = additionPart.split("\\*");

            for (String multiplicationPart : multiplicationParts) {
                multiplicationResult *= Integer.parseInt(multiplicationPart);
            }

            result += multiplicationResult;
        }

        return result;
    }
}