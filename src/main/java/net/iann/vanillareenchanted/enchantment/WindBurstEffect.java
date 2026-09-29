package net.iann.vanillareenchanted.enchantment;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record WindBurstEffect() implements EnchantmentEntityEffect {
    public static final MapCodec<WindBurstEffect> CODEC = MapCodec.unit(new WindBurstEffect());
    @Override public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        if (entity instanceof ServerPlayer player && item.owner() == player) {
            WindBurst.tryProc(player, item.itemStack(), enchantmentLevel);
        }
    }
    @Override public MapCodec<WindBurstEffect> codec() { return CODEC; }
}
