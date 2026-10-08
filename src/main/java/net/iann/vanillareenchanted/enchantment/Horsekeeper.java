package net.iann.vanillareenchanted.enchantment;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.iann.vanillareenchanted.network.HorsekeeperEffectPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Horsekeeper {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "horsekeeper");
    private static final String BINDING = "iannvanillareenchanted:horsekeeper";
    private static final double STORE_RANGE = 8.0;
    // The supplied prespawn burst lasts 15 Effekseer frames (one quarter second).
    private static final int PRESPAWN_TICKS = 5;
    private record Summon(UUID horse, ItemStack horn, ServerLevel level, Vec3 position, int due) {}
    private static final Map<ServerPlayer, Summon> PENDING = new WeakHashMap<>();

    public static boolean enchanted(ItemStack stack) {
        return stack.getItem() instanceof InstrumentItem && stack.getEnchantments().entrySet().stream()
                .anyMatch(e -> e.getIntValue() > 0 && e.getKey().unwrapKey()
                        .map(key -> key.location().equals(ID)).orElse(false));
    }

    private static UUID boundHorse(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(BINDING);
        return tag.hasUUID("Horse") ? tag.getUUID("Horse") : null;
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) { bind(event); }

    @SubscribeEvent
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) { bind(event); }

    private static void bind(PlayerInteractEvent event) {
        var target = event instanceof PlayerInteractEvent.EntityInteract specific ? specific.getTarget()
                : ((PlayerInteractEvent.EntityInteractSpecific) event).getTarget();
        Player player = event.getEntity();
        ItemStack horn = event.getItemStack();
        if (!player.isShiftKeyDown() || !enchanted(horn) || !(target instanceof AbstractHorse horse)) return;
        if (event instanceof PlayerInteractEvent.EntityInteract interaction) {
            interaction.setCanceled(true);
            interaction.setCancellationResult(InteractionResult.SUCCESS);
        } else if (event instanceof PlayerInteractEvent.EntityInteractSpecific interaction) {
            interaction.setCanceled(true);
            interaction.setCancellationResult(InteractionResult.SUCCESS);
        }
        if (!(player instanceof ServerPlayer server) || player.isSpectator()) return;
        if (!horse.isAlive() || !horse.isTamed() || !player.getUUID().equals(horse.getOwnerUUID())) {
            message(server, "not_owned");
            return;
        }
        UUID old = boundHorse(horn);
        if (old != null && HorsekeeperStorage.get(server.server).horse(old) != null) {
            message(server, "release_first");
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, horn, data -> {
            CompoundTag binding = new CompoundTag();
            binding.putUUID("Horse", horse.getUUID());
            data.put(BINDING, binding);
        });
        server.displayClientMessage(Component.translatable("message.iannvanillareenchanted.horsekeeper.bound", horse.getDisplayName()), true);
    }

    /** Called only after vanilla successfully begins blowing the horn. */
    public static void blow(ServerPlayer player, ItemStack horn) {
        if (!enchanted(horn) || player.isSpectator() || !player.isAlive() || PENDING.containsKey(player)) return;
        UUID id = boundHorse(horn);
        if (id == null) { message(player, "unbound"); return; }
        HorsekeeperStorage storage = HorsekeeperStorage.get(player.server);
        CompoundTag saved = storage.horse(id);
        if (saved != null) {
            if (PENDING.values().stream().anyMatch(summon -> summon.horse().equals(id))) return;
            // Never create a second live entity with the same identity.
            for (ServerLevel level : player.server.getAllLevels()) {
                if (level.getEntity(id) != null) { message(player, "already_present"); return; }
            }
            AbstractHorse horse = restore(player.serverLevel(), saved);
            if (horse == null) { message(player, "restore_failed"); return; }
            if (!player.getUUID().equals(horse.getOwnerUUID())) { message(player, "not_owned"); return; }
            Vec3 position = findPosition(player, horse);
            if (position == null) { message(player, "no_room"); return; }
            PENDING.put(player, new Summon(id, horn, player.serverLevel(), position, player.tickCount + PRESPAWN_TICKS));
            effect(player.serverLevel(), position, HorsekeeperEffectPayload.PRESPAWN);
            return;
        }
        var entity = player.serverLevel().getEntity(id);
        if (!(entity instanceof AbstractHorse horse) || !horse.isAlive()
                || player.distanceToSqr(horse) > STORE_RANGE * STORE_RANGE || !player.hasLineOfSight(horse)) {
            message(player, "not_nearby"); return;
        }
        if (!horse.isTamed() || !player.getUUID().equals(horse.getOwnerUUID())) { message(player, "not_owned"); return; }
        if (horse.isVehicle() || horse.isPassenger() || horse.isLeashed()) { message(player, "occupied"); return; }
        CompoundTag tag = new CompoundTag();
        if (!horse.save(tag)) { message(player, "restore_failed"); return; }
        storage.store(id, tag);
        effect(player.serverLevel(), horse.position(), HorsekeeperEffectPayload.DESPAWN);
        horse.discard();
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Summon summon = PENDING.get(player);
        if (summon == null) return;
        if (!player.isAlive() || player.isSpectator() || player.serverLevel() != summon.level()
                || (player.getMainHandItem() != summon.horn() && player.getOffhandItem() != summon.horn())
                || !enchanted(summon.horn()) || !summon.horse().equals(boundHorse(summon.horn()))) {
            PENDING.remove(player);
            return;
        }
        if (player.tickCount < summon.due()) return;
        PENDING.remove(player);
        HorsekeeperStorage storage = HorsekeeperStorage.get(player.server);
        CompoundTag saved = storage.horse(summon.horse());
        if (saved == null) return;
        AbstractHorse horse = restore(summon.level(), saved);
        if (horse == null || !player.getUUID().equals(horse.getOwnerUUID())) { message(player, "restore_failed"); return; }
        if (!safePosition(player, horse, summon.position())) { message(player, "no_room"); return; }
        for (ServerLevel level : player.server.getAllLevels()) {
            if (level.getEntity(summon.horse()) != null) { message(player, "already_present"); return; }
        }
        horse.moveTo(summon.position().x, summon.position().y, summon.position().z, player.getYRot(), 0);
        horse.setDeltaMovement(Vec3.ZERO);
        horse.fallDistance = 0;
        horse.setOnGround(true);
        // Keep the stored copy if another mod rejects spawning the entity.
        if (!summon.level().addFreshEntity(horse)) { message(player, "restore_failed"); return; }
        storage.release(summon.horse());
        effect(summon.level(), summon.position(), HorsekeeperEffectPayload.SPAWN);
    }

    private static AbstractHorse restore(ServerLevel level, CompoundTag saved) {
        var entity = EntityType.create(saved, level).orElse(null);
        return entity instanceof AbstractHorse horse && horse.isAlive() ? horse : null;
    }

    private static Vec3 findPosition(ServerPlayer player, AbstractHorse horse) {
        ServerLevel level = player.serverLevel();
        double facing = Math.toRadians(player.getYRot());
        for (double radius : new double[]{1.5, 2.0, 1.0}) {
            for (int index = 0; index < 16; index++) {
                double angle = facing + index * Math.PI / 8.0;
                double x = player.getX() - Math.sin(angle) * radius;
                double z = player.getZ() + Math.cos(angle) * radius;
                for (int offset : new int[]{-1, 0, -2, 1}) {
                    BlockPos support = BlockPos.containing(x, Math.floor(player.getY()) + offset, z);
                    if (!level.hasChunkAt(support)) continue;
                    var shape = level.getBlockState(support).getCollisionShape(level, support);
                    if (shape.isEmpty()) continue;
                    double y = support.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                    Vec3 position = new Vec3(x, y, z);
                    if (safePosition(player, horse, position)) return position;
                }
            }
        }
        return null;
    }

    private static boolean safePosition(ServerPlayer player, AbstractHorse horse, Vec3 position) {
        double distance = position.subtract(player.position()).horizontalDistanceSqr();
        if (distance < 1.0 - 1.0E-6 || distance > 4.0 + 1.0E-6 || Math.abs(position.y - player.getY()) > 1.000001) return false;
        ServerLevel level = player.serverLevel();
        horse.setPos(position);
        AABB box = horse.getBoundingBox();
        if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY - 0.1, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                || !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(horse, box)
                || !level.getEntities(horse, box, entity -> entity.isAlive() && !entity.isSpectator()).isEmpty()) return false;
        // Require support under all four corners, rather than balancing a horse over an edge.
        for (double x : new double[]{box.minX + 0.1, box.maxX - 0.1}) {
            for (double z : new double[]{box.minZ + 0.1, box.maxZ - 0.1}) {
                if (level.noCollision(horse, new AABB(x - 0.01, box.minY - 0.08, z - 0.01,
                        x + 0.01, box.minY, z + 0.01))) return false;
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - 0.08, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                    || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                    || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                    || state.is(Blocks.POWDER_SNOW)) return false;
        }
        return true;
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.iannvanillareenchanted.horsekeeper." + key), true);
    }

    private static void effect(ServerLevel level, Vec3 position, int action) {
        PacketDistributor.sendToPlayersNear(level, null, position.x, position.y, position.z, 64,
                new HorsekeeperEffectPayload(action, position.x, position.y, position.z));
    }

    private Horsekeeper() {}
}
