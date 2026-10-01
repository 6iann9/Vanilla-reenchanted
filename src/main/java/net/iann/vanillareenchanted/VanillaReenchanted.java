package net.iann.vanillareenchanted;

import net.iann.vanillareenchanted.client.event.ClientParticleEvents;
import net.iann.vanillareenchanted.config.VRConfig;
import net.iann.vanillareenchanted.config.ProtectionShieldConfig;
import net.iann.vanillareenchanted.config.ShieldHudConfig;
import net.iann.vanillareenchanted.client.ProtectionShieldHud;
import net.iann.vanillareenchanted.event.ProtectionShieldEvents;
import net.iann.vanillareenchanted.event.AnvilEvents;
import net.iann.vanillareenchanted.event.EnchantingTableEvents;
import net.iann.vanillareenchanted.event.RestingMendingEvents;
import net.iann.vanillareenchanted.event.VillagerTradeEvents;
import net.iann.vanillareenchanted.network.NetworkEvents;
import net.iann.vanillareenchanted.registry.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(VanillaReenchanted.MODID)
public class VanillaReenchanted {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "iannvanillareenchanted";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public VanillaReenchanted(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Loading Vanilla Reenchanted");

        ModAttachments.register(modEventBus);
        ModEnchantmentEffects.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);

        NeoForge.EVENT_BUS.register(VillagerTradeEvents.class);
        NeoForge.EVENT_BUS.register(AnvilEvents.class);
        NeoForge.EVENT_BUS.register(EnchantingTableEvents.class);
        NeoForge.EVENT_BUS.register(RestingMendingEvents.class);
        NeoForge.EVENT_BUS.register(ProtectionShieldEvents.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.event.WindUpEvents.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.event.WindBurstEvents.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.event.ShockwaveEvents.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.event.EchoingEdgeEvents.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.event.MomentumEvents.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.event.HighStepEvents.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.enchantment.Elusive.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.event.SoulSpeedEvents.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.event.RiptideEvents.class);
        NeoForge.EVENT_BUS.register(net.iann.vanillareenchanted.event.ChannelingEvents.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientParticleEvents::registerParticleProviders);
            modEventBus.addListener(ProtectionShieldHud::register);
            modEventBus.addListener(net.iann.vanillareenchanted.client.EchoingEdgeHud::register);
        }

        modContainer.registerConfig(ModConfig.Type.COMMON, VRConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ProtectionShieldConfig.SPEC, "iannvanillareenchanted-shield-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ShieldHudConfig.SPEC, "iannvanillareenchanted-shield-client.toml");

        modEventBus.addListener(NetworkEvents::registerPayloads);
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {

        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
}
