package net.iann.vanillareenchanted.event;

import java.util.Map;
import java.util.WeakHashMap;
import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class SoulSpeedEvents {
    public static final int KILL_BOOST_DURATION_TICKS = 3 * 20;
    private static final ResourceLocation BOOST = ResourceLocation.fromNamespaceAndPath(
            VanillaReenchanted.MODID, "soul_speed_kill_boost");
    private static final ResourceLocation VANILLA_BOOST = ResourceLocation.withDefaultNamespace(
            "enchantment.soul_speed/feet");
    // Server-only, transient state: never carried across logout or death.
    private static final Map<Player, Integer> EXPIRY = new WeakHashMap<>();

    private static int level(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).getEnchantments().entrySet().stream()
                .filter(entry -> entry.getKey().is(Enchantments.SOUL_SPEED))
                .mapToInt(entry -> entry.getIntValue()).max().orElse(0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof Player victim) clear(victim);
        var source = event.getSource().getEntity();
        Player killer = source instanceof Player player ? player
                : event.getEntity().getKillCredit() instanceof Player player ? player : null;
        if (killer == null || killer == event.getEntity() || !killer.isAlive() || level(killer) <= 0) return;
        EXPIRY.put(killer, killer.tickCount + KILL_BOOST_DURATION_TICKS);
        refresh(killer);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            refresh(player);
            tickEffects(player);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.FEET && event.getEntity() instanceof Player player
                && !player.level().isClientSide()) refresh(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }

    private static void tickEffects(Player player) {
        if (!player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(BOOST)
                || player.tickCount % 5 != 0 || !player.onGround()
                || player.isPassenger() || player.getAbilities().flying) return;
        var movement = player.getKnownMovement();
        if (movement.horizontalDistance() < 1.0E-5F) return;
        // Vanilla already emits these effects on soul blocks; avoid duplicate particles/sounds.
        if (player.level().getBlockState(player.getBlockPosBelowThatAffectsMyMovement())
                .is(BlockTags.SOUL_SPEED_BLOCKS)) return;
        var server = (ServerLevel) player.level();
        var random = player.getRandom();
        // Match vanilla's foot-level particle spread, trailing velocity, and five-tick cadence.
        server.sendParticles(ParticleTypes.SOUL,
                player.getX() + (random.nextDouble() - 0.5) * player.getBbWidth(),
                player.getY() + 0.1F,
                player.getZ() + (random.nextDouble() - 0.5) * player.getBbWidth(),
                0, movement.x * -0.2F, 0.1F, movement.z * -0.2F, 1.0);
        if (!player.isSilent() && random.nextFloat() < 0.35F) {
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SOUL_ESCAPE, player.getSoundSource(), 0.6F,
                    0.6F + random.nextFloat() * 0.4F);
        }
    }

    private static void clear(Player player) {
        EXPIRY.remove(player);
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(BOOST);
    }

    private static void refresh(Player player) {
        Integer expires = EXPIRY.get(player);
        if (expires == null) return;
        int enchantmentLevel = level(player);
        if (!player.isAlive() || player.tickCount >= expires || enchantmentLevel <= 0) {
            clear(player);
            return;
        }
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        // Let vanilla own its modifier. Our bonus fills in only when vanilla is inactive.
        if (player.isPassenger() || player.getAbilities().flying || speed.hasModifier(VANILLA_BOOST)) {
            speed.removeModifier(BOOST);
            return;
        }
        double amount = 0.0405F + 0.0105F * (enchantmentLevel - 1);
        var current = speed.getModifier(BOOST);
        if (current == null || Double.compare(current.amount(), amount) != 0) {
            speed.removeModifier(BOOST);
            speed.addTransientModifier(new AttributeModifier(BOOST, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
