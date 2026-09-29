package net.iann.vanillareenchanted.enchantment;

import net.iann.vanillareenchanted.network.SyncChannelingPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Channeling {
    public static final int COOLDOWN_TICKS = 35 * 20;
    public static final String FIRELESS = "iannvanillareenchanted.ChannelingFireless";
    private static final String READY = "iannvanillareenchanted.ChannelingReady";

    public static boolean enchanted(ItemStack stack) {
        return stack.getEnchantments().entrySet().stream()
                .anyMatch(e -> e.getKey().is(Enchantments.CHANNELING) && e.getIntValue() > 0);
    }
    public static long readyAt(LivingEntity owner) { return owner.getPersistentData().getLong(READY); }
    public static void receive(Player player, long readyAt) { player.getPersistentData().putLong(READY, readyAt); }
    public static float cooldownPercent(Player player, ItemStack stack, float partial) {
        if (!enchanted(stack)) return 0;
        return Math.clamp((readyAt(player) - player.level().getGameTime() - partial) / COOLDOWN_TICKS, 0, 1);
    }
    public static void sync(Player player) {
        if (player instanceof ServerPlayer server && !(player instanceof FakePlayer))
            PacketDistributor.sendToPlayer(server, new SyncChannelingPayload(readyAt(player)));
    }
    public static boolean strike(ServerLevel level, LivingEntity owner, Vec3 origin) {
        if (owner == null || readyAt(owner) > level.getGameTime()) return false;
        var bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return false;
        bolt.moveTo(origin);
        bolt.getPersistentData().putBoolean(FIRELESS, true);
        if (owner instanceof ServerPlayer player) bolt.setCause(player);
        // Reserve the cooldown before adding the entity: spawn hooks can invoke other effects.
        long previous = readyAt(owner);
        owner.getPersistentData().putLong(READY, level.getGameTime() + COOLDOWN_TICKS);
        if (!level.addFreshEntity(bolt)) {
            owner.getPersistentData().putLong(READY, previous);
            return false;
        }
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 5, 1);
        if (owner instanceof Player player) sync(player);
        return true;
    }
    private Channeling() {}
}
