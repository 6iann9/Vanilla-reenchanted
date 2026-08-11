package net.iann.vanillareenchanted.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class VRConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_RESEARCH_XP_FORMULA;
    public static final ModConfigSpec.ConfigValue<String> DEFAULT_ENCHANT_LAPIS_FORMULA;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENCHANTMENT_FORMULAS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("costs");

        DEFAULT_RESEARCH_XP_FORMULA = builder
                .comment(
                        "Default XP level cost formula for researching enchantments.",
                        "Variable available: level",
                        "Example: level * 3 + 2"
                )
                .define("defaultResearchXpFormula", "level * 3 + 2");

        DEFAULT_ENCHANT_LAPIS_FORMULA = builder
                .comment(
                        "Default lapis cost formula for applying/upgrading enchantments.",
                        "Variable available: level",
                        "Example: level * 2 + 1"
                )
                .define("defaultEnchantLapisFormula", "level * 2 + 1");

        ENCHANTMENT_FORMULAS = builder
                .comment(
                        "Per-enchantment formula overrides.",
                        "Format:",
                        "enchantment_id|research=formula|lapis=formula",
                        "Examples:",
                        "minecraft:efficiency|research=level * 3 + 2|lapis=level * 2 + 1",
                        "minecraft:thorns|research=level * 8 + 5|lapis=level * 4 + 2",
                        "If an enchantment is not listed here, the default formulas are used."
                )
                .defineListAllowEmpty(
                        "enchantmentFormulas",
                        List.of(
                                "minecraft:efficiency|research=level * 3 + 2|lapis=level * 2 + 1",
                                "minecraft:thorns|research=level * 8 + 5|lapis=level * 4 + 2"
                        ),
                        () -> "",
                        value -> value instanceof String
                );

        builder.pop();

        SPEC = builder.build();
    }
}