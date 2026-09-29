package net.iann.vanillareenchanted.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ProtectionShieldConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue HP_PER_LEVEL;
    public static final ModConfigSpec.IntValue RECHARGE_DELAY_TICKS;
    public static final ModConfigSpec.DoubleValue HP_PER_SECOND;
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("protectionShield");
        HP_PER_LEVEL = builder.comment("Shield HP per chestplate Protection level. Two HP equal one heart.")
                .defineInRange("hpPerLevel", 5.0, 0.0, 100.0);
        RECHARGE_DELAY_TICKS = builder.comment("Delay after damage or increasing shield capacity. 20 ticks = one second.")
                .defineInRange("rechargeDelayTicks", 160, 0, 12000);
        HP_PER_SECOND = builder.comment("Shield HP regenerated per second after the delay.")
                .defineInRange("hpPerSecond", 1.0, 0.0, 100.0);
        builder.pop();
        SPEC = builder.build();
    }
}
