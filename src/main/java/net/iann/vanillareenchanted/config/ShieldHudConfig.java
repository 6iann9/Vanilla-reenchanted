package net.iann.vanillareenchanted.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ShieldHudConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue AUTO_STACK;
    public static final ModConfigSpec.BooleanValue SHOW_VALUES;
    public static final ModConfigSpec.IntValue X_OFFSET;
    public static final ModConfigSpec.IntValue Y_OFFSET;
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("shieldHud");
        ENABLED = builder.comment("Display the shield bar. Disabling this does not disable shield mechanics.")
                .define("enabled", true);
        AUTO_STACK = builder.comment("Place between the vanilla health and armor rows using shared HUD spacing. Disable for a fixed position with custom HUD mods.")
                .define("autoStack", true);
        SHOW_VALUES = builder.define("showValues", true);
        X_OFFSET = builder.comment("Horizontal offset from screen center, in GUI pixels.")
                .defineInRange("xOffset", -91, -4000, 4000);
        Y_OFFSET = builder.comment("Vertical adjustment; with autoStack disabled, offset from the screen bottom.")
                .defineInRange("yOffset", 0, -4000, 4000);
        builder.pop();
        SPEC = builder.build();
    }
}
