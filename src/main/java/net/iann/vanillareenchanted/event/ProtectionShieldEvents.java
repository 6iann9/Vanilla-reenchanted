package net.iann.vanillareenchanted.event;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.config.ProtectionShieldConfig;
import net.iann.vanillareenchanted.enchantment.ProtectionShieldData;
import net.iann.vanillareenchanted.network.SyncProtectionShieldPayload;
import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ProtectionShieldEvents {
    public static final TagKey<DamageType> BYPASSES_SHIELD = TagKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(VanillaReenchanted.MODID, "bypasses_protection_shield"));

    private static int delay() { return ProtectionShieldConfig.RECHARGE_DELAY_TICKS.get(); }

    private static ProtectionShieldData refresh(LivingEntity entity) {
        int level = entity.getItemBySlot(EquipmentSlot.CHEST).getEnchantments().entrySet().stream()
                .filter(entry -> entry.getKey().is(Enchantments.PROTECTION))
                .mapToInt(entry -> entry.getIntValue()).max().orElse(0);
        ProtectionShieldData data = entity.getData(ModAttachments.PROTECTION_SHIELD);
        data.updateCapacity(entity.isAlive() ? (float) (level * ProtectionShieldConfig.HP_PER_LEVEL.get()) : 0, delay());
        return data;
    }

    private static void sync(LivingEntity entity, ProtectionShieldData data) {
        if (entity instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new SyncProtectionShieldPayload(data.current(), data.capacity()));
        }
        data.markSynced();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getNewDamage() <= 0) return;
        ProtectionShieldData data = refresh(entity);
        // Damage that bypasses the pool still interrupts recharge.
        data.interruptRecharge(delay());
        if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !event.getSource().is(BYPASSES_SHIELD)) {
            // NeoForge fires Pre after armor/resistance and BEFORE vanilla absorption.
            event.setNewDamage(data.absorb(event.getNewDamage()));
        }
        if (data.isDirty()) sync(entity, data);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
        // Avoid creating attachments on every unenchanted mob just to tick an empty pool.
        if (!entity.hasData(ModAttachments.PROTECTION_SHIELD)) return;
        ProtectionShieldData data = entity.tickCount % 20 == 0
                ? refresh(entity) : entity.getData(ModAttachments.PROTECTION_SHIELD);
        if (entity.isAlive()) data.tick(ProtectionShieldConfig.HP_PER_SECOND.get().floatValue() / 20.0F);
        if (data.isDirty() && entity.tickCount % 5 == 0) sync(entity, data);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.CHEST) return;
        ProtectionShieldData data = refresh(event.getEntity());
        if (data.isDirty()) sync(event.getEntity(), data);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        ProtectionShieldData data = event.getEntity().getData(ModAttachments.PROTECTION_SHIELD);
        data.updateCapacity(0, 0);
        sync(event.getEntity(), data);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity(), refresh(event.getEntity()));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity(), refresh(event.getEntity()));
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity(), refresh(event.getEntity()));
    }
}
