package net.iann.vanillareenchanted.event;

import net.minecraft.core.Direction;
import net.iann.vanillareenchanted.enchantment.RestingMendingHelper;
import net.iann.vanillareenchanted.mixin.ItemFrameMendingAccess;
import net.minecraft.world.phys.Vec3;
import net.iann.vanillareenchanted.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class RestingMendingEvents {

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

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameTime() % RestingMendingHelper.CHECK_INTERVAL_TICKS != 0) {
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

        RestingMendingHelper.updateRestingMending(armorStand, slot.getName(), stack, level.getGameTime());

        // Show ongoing repair, including the wait between durability gains.
        if (RestingMendingHelper.isRepairing(stack)) {
            spawnRestingMendingParticlesAtArmorSlot(level, armorStand, heightPercent);
        }
    }

    private static void handleItemFrame(ServerLevel level, ItemFrame itemFrame) {
        ItemStack stack = itemFrame.getItem();

        if (stack.isEmpty()) {
            return;
        }

        if (RestingMendingHelper.updateRestingMending(itemFrame, "item", stack, level.getGameTime())) {
            // In-place ItemStack changes do not dirty the frame's synced item automatically.
            itemFrame.getEntityData().set(ItemFrameMendingAccess.vr$itemData(), stack, true);
        }

        // Show ongoing repair, including the wait between durability gains.
        if (RestingMendingHelper.isRepairing(stack)) {
            spawnRestingMendingParticlesAtItemFrame(level, itemFrame);
        }
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