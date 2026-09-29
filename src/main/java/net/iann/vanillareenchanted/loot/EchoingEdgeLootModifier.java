package net.iann.vanillareenchanted.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.iann.vanillareenchanted.enchantment.EchoingEdge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class EchoingEdgeLootModifier extends LootModifier {
    public static final MapCodec<EchoingEdgeLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, EchoingEdgeLootModifier::new));
    public EchoingEdgeLootModifier(LootItemCondition[] conditions) { super(conditions); }
    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var enchantment = context.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, EchoingEdge.ID));
        loot.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, 1 + context.getRandom().nextInt(3))));
        return loot;
    }
}
