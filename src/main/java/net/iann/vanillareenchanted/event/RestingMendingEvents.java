package net.iann.vanillareenchanted.event;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.iann.vanillareenchanted.registry.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class RestingMendingEvents {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int TICKS_PER_REPAIR = 20 * 10;
    private static final long MAX_CATCH_UP_TICKS = 24000L * 3L;

    // Spread values are half-extents in blocks: 0.45 means +/-0.45 (0.9 total).
    private static final int ARMOR_PARTICLES_PER_SLOT = 3;
    private static final double ARMOR_SPREAD_X = 0.50D;
    private static final double ARMOR_SPREAD_Y = 0.16D;
    private static final double ARMOR_SPREAD_Z = 0.50D;
    private static final double ARMOR_Y_OFFSET = 0.0D;

    // Slot centers as fractions of the stand's height, measured from its feet.
    private static final double ARMOR_HEAD_HEIGHT = 0.90D;
    private static final double ARMOR_CHEST_HEIGHT = 0.63D;
    private static final double ARMOR_LEGS_HEIGHT = 0.34D;
    private static final double ARMOR_FEET_HEIGHT = 0.14D;

    private static final int FRAME_PARTICLE_COUNT = 1;
    // Width/height follow the frame's plane, including floor and ceiling frames.
    private static final double FRAME_SPREAD_WIDTH = 0.32D;
    private static final double FRAME_SPREAD_HEIGHT = 0.32D;
    private static final double FRAME_SPREAD_DEPTH = 0.025D;
    private static final double FRAME_HORIZONTAL_OFFSET = 0.0D;
    private static final double FRAME_VERTICAL_OFFSET = 0.0D;
    // Positive values move particles outward from the frame, away from the wall.
    private static final double FRAME_FRONT_OFFSET = 0.08D;

    private static final String LAST_RESTING_MENDING_CHECK_TAG =
            VanillaReenchanted.MODID + ".LastRestingMendingCheck";

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        if (entity instanceof ArmorStand armorStand) {
            handleArmorStand(level, armorStand);
            return;
        }

        if (entity instanceof ItemFrame itemFrame) {
            handleItemFrame(level, itemFrame);
        }
    }

    private static void handleArmorStand(ServerLevel level, ArmorStand armorStand) {
        handleArmorStandSlot(level, armorStand, EquipmentSlot.HEAD, ARMOR_HEAD_HEIGHT);
        handleArmorStandSlot(level, armorStand, EquipmentSlot.CHEST, ARMOR_CHEST_HEIGHT);
        handleArmorStandSlot(level, armorStand, EquipmentSlot.LEGS, ARMOR_LEGS_HEIGHT);
        handleArmorStandSlot(level, armorStand, EquipmentSlot.FEET, ARMOR_FEET_HEIGHT);
    }

    private static void handleArmorStandSlot(ServerLevel level, ArmorStand armorStand, EquipmentSlot slot, double heightPercent) {
        ItemStack stack = armorStand.getItemBySlot(slot);

        if (stack.isEmpty()) {
            return;
        }

        tickRestingMendingItem(level, stack);

        // Show ongoing repair, including the wait between durability gains.
        if (stack.isDamageableItem() && stack.isDamaged() && hasMending(stack)) {
            spawnRestingMendingParticlesAtArmorSlot(level, armorStand, heightPercent);
        }
    }

    private static void handleItemFrame(ServerLevel level, ItemFrame itemFrame) {
        ItemStack stack = itemFrame.getItem();

        if (stack.isEmpty()) {
            return;
        }

        tickRestingMendingItem(level, stack);

        // Show ongoing repair, including the wait between durability gains.
        if (stack.isDamageableItem() && stack.isDamaged() && hasMending(stack)) {
            spawnRestingMendingParticlesAtItemFrame(level, itemFrame);
        }
    }

    private static void tickRestingMendingItem(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (!stack.isDamageableItem()) {
            clearLastCheck(stack);
            return;
        }

        if (!hasMending(stack)) {
            clearLastCheck(stack);
            return;
        }

        long currentTime = level.getGameTime();

        if (!stack.isDamaged()) {
            rememberCheckTime(stack, currentTime);
            return;
        }

        long lastCheckTime = getLastCheckTime(stack);

        if (lastCheckTime <= 0L || lastCheckTime > currentTime) {
            rememberCheckTime(stack, currentTime);
            return;
        }

        long elapsedTicks = currentTime - lastCheckTime;
        long cappedElapsedTicks = Math.min(elapsedTicks, MAX_CATCH_UP_TICKS);

        int durabilityToRepair = (int) (cappedElapsedTicks / TICKS_PER_REPAIR);

        if (durabilityToRepair <= 0) {
            return;
        }

        int newDamageValue = Math.max(0, stack.getDamageValue() - durabilityToRepair);
        stack.setDamageValue(newDamageValue);

        long usedTicks = (long) durabilityToRepair * TICKS_PER_REPAIR;
        rememberCheckTime(stack, lastCheckTime + usedTicks);
    }

    private static boolean hasMending(ItemStack stack) {
        return stack.getEnchantments().keySet().stream()
                .anyMatch(enchantmentHolder -> enchantmentHolder.is(Enchantments.MENDING));
    }

    private static long getLastCheckTime(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (!tag.contains(LAST_RESTING_MENDING_CHECK_TAG)) {
            return -1L;
        }

        return tag.getLong(LAST_RESTING_MENDING_CHECK_TAG);
    }

    private static void rememberCheckTime(ItemStack stack, long gameTime) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        tag.putLong(LAST_RESTING_MENDING_CHECK_TAG, gameTime);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearLastCheck(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        if (!tag.contains(LAST_RESTING_MENDING_CHECK_TAG)) {
            return;
        }

        tag.remove(LAST_RESTING_MENDING_CHECK_TAG);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void spawnRestingMendingParticlesAtArmorSlot(ServerLevel level, ArmorStand armorStand, double heightPercent) {
        RandomSource random = level.random;
        double baseY = armorStand.getY() + armorStand.getBbHeight() * heightPercent + ARMOR_Y_OFFSET;
        for (int i = 0; i < ARMOR_PARTICLES_PER_SLOT; i++) {
            spawnRestingMendingParticle(level,
                    armorStand.getX() + randomOffset(random, ARMOR_SPREAD_X),
                    baseY + randomOffset(random, ARMOR_SPREAD_Y),
                    armorStand.getZ() + randomOffset(random, ARMOR_SPREAD_Z));
        }
    }

    private static void spawnRestingMendingParticlesAtItemFrame(ServerLevel level, ItemFrame frame) {
        RandomSource random = level.random;
        Direction facing = frame.getDirection();
        Vec3 normal = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vec3 up = facing.getAxis() == Direction.Axis.Y
                ? new Vec3(0.0D, 0.0D, 1.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = up.cross(normal);
        // The bounding box already supplies the frame center; adding half-height shifts it up.
        Vec3 center = frame.getBoundingBox().getCenter()
                .add(normal.scale(FRAME_FRONT_OFFSET))
                .add(right.scale(FRAME_HORIZONTAL_OFFSET))
                .add(up.scale(FRAME_VERTICAL_OFFSET));
        for (int i = 0; i < FRAME_PARTICLE_COUNT; i++) {
            Vec3 position = center
                    .add(right.scale(randomOffset(random, FRAME_SPREAD_WIDTH)))
                    .add(up.scale(randomOffset(random, FRAME_SPREAD_HEIGHT)))
                    .add(normal.scale(randomOffset(random, FRAME_SPREAD_DEPTH)));
            spawnRestingMendingParticle(level, position.x, position.y, position.z);
        }
    }

    private static double randomOffset(RandomSource random, double halfExtent) {
        return (random.nextDouble() * 2.0D - 1.0D) * halfExtent;
    }

    private static void spawnRestingMendingParticle(ServerLevel level, double x, double y, double z) {
        level.sendParticles(
                ModParticles.RESTING_MENDING.get(),
                x,
                y,
                z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }
}