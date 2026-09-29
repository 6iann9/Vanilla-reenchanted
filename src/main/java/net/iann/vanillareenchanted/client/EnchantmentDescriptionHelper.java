package net.iann.vanillareenchanted.client;

import net.minecraft.client.resources.language.I18n;
import net.iann.vanillareenchanted.config.ProtectionShieldConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Shared descriptions for the bookmark and optional Enchantment Descriptions integration. */
public final class EnchantmentDescriptionHelper {
    private EnchantmentDescriptionHelper() {
    }

    @Nullable
    public static MutableComponent getOverride(@Nullable ResourceLocation enchantmentId) {
        if (enchantmentId == null) return null;
        String suffix = enchantmentId.getNamespace() + "." + enchantmentId.getPath().replace('/', '.');
        String overrideKey = "tooltip.iannvanillareenchanted.enchantment." + suffix + ".desc";
        if (!I18n.exists(overrideKey)) return null;
        if (enchantmentId.equals(ResourceLocation.withDefaultNamespace("protection"))) {
            // Connected clients use the server's synchronized settings; previews can use defaults.
            boolean loaded = ProtectionShieldConfig.SPEC.isLoaded();
            double hp = loaded ? ProtectionShieldConfig.HP_PER_LEVEL.get() : ProtectionShieldConfig.HP_PER_LEVEL.getDefault();
            double regen = loaded ? ProtectionShieldConfig.HP_PER_SECOND.get() : ProtectionShieldConfig.HP_PER_SECOND.getDefault();
            int delay = loaded ? ProtectionShieldConfig.RECHARGE_DELAY_TICKS.get() : ProtectionShieldConfig.RECHARGE_DELAY_TICKS.getDefault();
            return Component.translatable(overrideKey, hp, regen, delay / 20.0D);
        }
        return Component.translatable(overrideKey);
    }

    public static Component getDescription(@Nullable ResourceLocation enchantmentId) {
        MutableComponent override = getOverride(enchantmentId);
        if (override != null) return override;
        if (enchantmentId != null) {
            String suffix = enchantmentId.getNamespace() + "." + enchantmentId.getPath().replace('/', '.');
            String descriptionKey = "enchantment." + suffix + ".desc";
            if (I18n.exists(descriptionKey)) return Component.translatable(descriptionKey);
        }
        return Component.translatable("tooltip.iannvanillareenchanted.description_unavailable");
    }
}