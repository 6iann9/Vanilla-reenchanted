package net.iann.vanillareenchanted.registry;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, VanillaReenchanted.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> MOMENTUM_PROC = SOUNDS.register(
            "momentum_proc", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(VanillaReenchanted.MODID, "momentum_proc")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHOING_EDGE_IMPACT = SOUNDS.register(
            "echoing_edge_impact", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(VanillaReenchanted.MODID, "echoing_edge_impact")));
}
