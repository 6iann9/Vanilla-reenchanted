package net.iann.vanillareenchanted.registry;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.enchantment.PlayerKnowledgeData;
import net.iann.vanillareenchanted.enchantment.ProtectionShieldData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, VanillaReenchanted.MODID);

    public static final Supplier<AttachmentType<PlayerKnowledgeData>> PLAYER_KNOWLEDGE =
            ATTACHMENT_TYPES.register(
                    "player_knowledge",
                    () -> AttachmentType.builder(PlayerKnowledgeData::new)
                            .build()
            );

    // No persistence or copy-on-death: reconnecting or respawning starts the shield empty.
    public static final Supplier<AttachmentType<ProtectionShieldData>> PROTECTION_SHIELD =
            ATTACHMENT_TYPES.register("protection_shield",
                    () -> AttachmentType.builder(ProtectionShieldData::new).build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}