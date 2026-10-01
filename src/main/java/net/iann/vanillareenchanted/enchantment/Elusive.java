package net.iann.vanillareenchanted.enchantment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class Elusive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "elusive");
    public static final TagKey<DamageType> IMMUNE_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "elusive_immune"));
    public static final int WATER_RUN_TICKS = 60;
    public static final long READY = -1;

    public static boolean enchanted(AbstractHorse horse) {
        return horse.isAlive() && horse.getItemBySlot(EquipmentSlot.BODY).getEnchantments().entrySet().stream()
                .anyMatch(e -> e.getIntValue() > 0
                        && e.getKey().unwrapKey().map(key -> key.location().equals(ID)).orElse(false));
    }

    public static boolean mounted(AbstractHorse horse) {
        return horse.getControllingPassenger() instanceof Player && enchanted(horse);
    }

    public static boolean phasesLeaves(Entity entity) {
        return entity instanceof AbstractHorse horse && mounted(horse)
                || entity instanceof Player && entity.getVehicle() instanceof AbstractHorse riddenHorse && mounted(riddenHorse);
    }

    public static boolean waterAvailable(AbstractHorse horse) {
        if (!mounted(horse)) return false;
        long deadline = ((ElusiveWaterAccess)horse).vr$elusiveDeadline();
        return deadline == READY || horse.level().getGameTime() < deadline;
    }

    public static boolean canStandOnWater(AbstractHorse horse, FluidState fluid) {
        if (!fluid.is(FluidTags.WATER) || !waterAvailable(horse)) return false;
        // Once submerged, keep normal swimming physics; do not lift the horse from underwater.
        BlockPos pos = horse.blockPosition();
        FluidState current = horse.level().getFluidState(pos);
        return !current.is(FluidTags.WATER)
                || horse.getY() >= pos.getY() + current.getHeight(horse.level(), pos) - 0.03;
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse && enchanted(horse)
                && event.getSource().is(IMMUNE_DAMAGE)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide) return;
        var access = (ElusiveWaterAccess)horse;
        long deadline = access.vr$elusiveDeadline();
        if (deadline == READY && !mounted(horse)) return;
        AABB feet = horse.getBoundingBox();
        AABB support = new AABB(feet.minX + 0.01, feet.minY - 0.04, feet.minZ + 0.01,
                feet.maxX - 0.01, feet.minY + 0.001, feet.maxZ - 0.01);
        boolean water = false;
        boolean ground = false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(support.minX, support.minY, support.minZ),
                BlockPos.containing(support.maxX, support.maxY, support.maxZ))) {
            var state = horse.level().getBlockState(pos);
            var fluid = state.getFluidState();
            if (fluid.is(FluidTags.WATER)
                    && !horse.level().getFluidState(pos.above()).is(FluidTags.WATER)
                    && Math.abs(feet.minY - pos.getY() - fluid.getHeight(horse.level(), pos)) < 0.04) {
                water = true;
            }
            // Empty context omits our water platform. Leaves being phased through are not ground.
            if (!state.is(net.minecraft.tags.BlockTags.LEAVES) || !phasesLeaves(horse)) {
                var shape = state.getCollisionShape(horse.level(), pos, CollisionContext.empty());
                if (!shape.isEmpty() && Shapes.joinIsNotEmpty(Shapes.create(support),
                        shape.move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.AND)) ground = true;
            }
        }
        if (horse.onGround() && ground) {
            if (deadline != READY) access.vr$elusiveDeadline(READY);
        } else if (deadline == READY && water && mounted(horse)) {
            access.vr$elusiveDeadline(horse.level().getGameTime() + WATER_RUN_TICKS);
        }
    }

    private Elusive() {}
}
