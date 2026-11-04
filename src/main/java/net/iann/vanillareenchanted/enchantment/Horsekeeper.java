package net.iann.vanillareenchanted.enchantment;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;
import net.iann.vanillareenchanted.network.HorsekeeperEffectPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

    public static void appendTooltip(ItemStack horn, List<Component> tooltip) {
        if (boundHorse(horn) == null) return;
        CompoundTag binding = horn.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(BINDING);
        String name = binding.getString("Name");
        tooltip.add(Component.translatable("tooltip.iannvanillareenchanted.horsekeeper.bound",
                name.isEmpty() ? Component.translatable("entity.minecraft.horse") : Component.literal(name)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.iannvanillareenchanted.horsekeeper.stored",
                Component.translatable(binding.getBoolean("Stored") ? "gui.yes" : "gui.no")).withStyle(ChatFormatting.GRAY));
    }

    private static void updateTooltip(ItemStack horn, String name, boolean stored) {
        CompoundTag binding = horn.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(BINDING);
        if (binding.contains("Stored") && binding.getBoolean("Stored") == stored && binding.getString("Name").equals(name)) return;
        CustomData.update(DataComponents.CUSTOM_DATA, horn, data -> {
            CompoundTag updated = data.getCompound(BINDING);
            updated.putString("Name", name);
            updated.putBoolean("Stored", stored);
            data.put(BINDING, updated);
        });
    }

    private static String horseName(AbstractHorse horse) {
        return horse.hasCustomName() ? horse.getCustomName().getString() : "";
    }

    private static void unbind(ServerPlayer player, ItemStack horn) {
        UUID id = boundHorse(horn);
        if (id == null) return;
        if (HorsekeeperStorage.get(player.server).isStored(id)) {
            message(player, "release_first");
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, horn, data -> data.remove(BINDING));
        message(player, "unbound_success");
    }

    @SubscribeEvent
    public static void crouchUse(PlayerInteractEvent.RightClickItem event) {
        if (!event.getEntity().isShiftKeyDown() || !enchanted(event.getItemStack()) || boundHorse(event.getItemStack()) == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()) unbind(player, event.getItemStack());
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
        if (horse.getUUID().equals(old)) {
            unbind(server, horn);
            return;
        }
        if (old != null && HorsekeeperStorage.get(server.server).horse(old) != null) {
            message(server, "release_first");
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, horn, data -> {
            CompoundTag binding = new CompoundTag();
            binding.putUUID("Horse", horse.getUUID());
            binding.putString("Name", horseName(horse));
            binding.putBoolean("Stored", false);
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
                || player.getBoundingBox().getCenter().distanceToSqr(horse.getBoundingBox().getCenter())
                        > STORE_RANGE * STORE_RANGE) {
            message(player, "not_nearby"); return;
        }
        if (!horse.isTamed() || !player.getUUID().equals(horse.getOwnerUUID())) { message(player, "not_owned"); return; }
        if (horse.isVehicle() || horse.isPassenger() || horse.isLeashed()) { message(player, "occupied"); return; }
        CompoundTag tag = new CompoundTag();
        if (!horse.save(tag)) { message(player, "restore_failed"); return; }
        storage.store(id, tag);
        updateTooltip(horn, horseName(horse), true);
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
        // Refresh legacy horns, renamed horses and copied horns from authoritative storage.
        // Only changed metadata is written, so normal inventory sync handles the tooltip.
        if (player.tickCount % 20 == 0) {
            for (var slot : player.containerMenu.slots) refreshTooltip(player, slot.getItem());
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                refreshTooltip(player, player.getInventory().getItem(slot));
            }
        }
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
        updateTooltip(summon.horn(), horseName(horse), false);
        effect(summon.level(), summon.position(), HorsekeeperEffectPayload.SPAWN);
    }

    private static AbstractHorse restore(ServerLevel level, CompoundTag saved) {
        var entity = EntityType.create(saved, level).orElse(null);
        return entity instanceof AbstractHorse horse && horse.isAlive() ? horse : null;
    }

    private static void refreshTooltip(ServerPlayer player, ItemStack horn) {
        if (!(horn.getItem() instanceof InstrumentItem)) return;
        UUID id = boundHorse(horn);
        if (id == null) return;
        HorsekeeperStorage storage = HorsekeeperStorage.get(player.server);
        if (storage.isStored(id)) {
            updateTooltip(horn, storage.storedName(id, player.registryAccess()), true);
            return;
        }
        String name = horn.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(BINDING).getString("Name");
        for (ServerLevel level : player.server.getAllLevels()) {
            if (level.getEntity(id) instanceof AbstractHorse horse) {
                name = horseName(horse);
                break;
            }
        }
        updateTooltip(horn, name, false);
    }

    private static Vec3 findPosition(ServerPlayer player, AbstractHorse horse) {
        ServerLevel level = player.serverLevel();
        double facing = Math.toRadians(player.getYRot());
        for (double radius : new double[]{2.0, 1.75, 1.5, 1.25, 1.0}) {
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
                || !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(horse, box.deflate(1.0E-5))
                || !level.getEntitiesOfClass(LivingEntity.class, box,
                        entity -> entity.isAlive() && !entity.isSpectator()).isEmpty()) return false;
        // Collision shapes allow harmless grass and flowers. Require ground under the
        // horse's center, without demanding a perfectly level platform at every corner.
        // Use block collisions here so another entity cannot act as the ground.
        AABB support = new AABB(position.x - 0.2, box.minY - 0.08, position.z - 0.2,
                position.x + 0.2, box.minY, position.z + 0.2);
        if (!level.getBlockCollisions(horse, support).iterator().hasNext()) return false;
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
