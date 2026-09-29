package net.iann.vanillareenchanted.enchantment;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record ChannelingEffect() implements EnchantmentEntityEffect {
    public static final MapCodec<ChannelingEffect> CODEC = MapCodec.unit(new ChannelingEffect());
    @Override public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        Channeling.strike(level, item.owner(), origin);
    }
    @Override public MapCodec<ChannelingEffect> codec() { return CODEC; }
}
