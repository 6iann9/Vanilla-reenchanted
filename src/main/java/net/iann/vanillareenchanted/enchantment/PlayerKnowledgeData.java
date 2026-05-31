package net.iann.vanillareenchanted.enchantment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;

public class PlayerKnowledgeData implements INBTSerializable<CompoundTag> {
    private final Map<ResourceLocation, Integer> unlockedEnchantments = new HashMap<>();

    public int getLevel(ResourceLocation enchantmentId) {
        return this.unlockedEnchantments.getOrDefault(enchantmentId, 0);
    }

    public void setLevel(ResourceLocation enchantmentId, int level) {
        if (level <= 0) {
            this.unlockedEnchantments.remove(enchantmentId);
            return;
        }

        this.unlockedEnchantments.put(enchantmentId, level);
    }

    public boolean hasUnlocked(ResourceLocation enchantmentId) {
        return this.getLevel(enchantmentId) > 0;
    }

    public Map<ResourceLocation, Integer> getAll() {
        return Map.copyOf(this.unlockedEnchantments);
    }


    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        for (Map.Entry<ResourceLocation, Integer> entry : this.unlockedEnchantments.entrySet()) {
            tag.putInt(entry.getKey().toString(), entry.getValue());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.unlockedEnchantments.clear();

        for (String key : tag.getAllKeys()) {
            ResourceLocation enchantmentId = ResourceLocation.tryParse(key);

            if (enchantmentId == null) {
                continue;
            }

            int level = tag.getInt(key);

            if (level > 0) {
                this.unlockedEnchantments.put(enchantmentId, level);
            }
        }
    }
}