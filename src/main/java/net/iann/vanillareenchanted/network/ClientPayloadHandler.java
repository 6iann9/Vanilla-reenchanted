package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.menu.ResearchTableMenu;
import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleSyncPlayerKnowledge(
            SyncPlayerKnowledgePayload payload,
            IPayloadContext context
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        minecraft.player
                .getData(ModAttachments.PLAYER_KNOWLEDGE)
                .deserializeNBT(
                        minecraft.player.registryAccess(),
                        payload.knowledgeTag()
                );
    }
    public static void handleSyncLibraryKnowledge(
            SyncLibraryKnowledgePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null) {
                return;
            }

            if (!(minecraft.player.containerMenu instanceof ResearchTableMenu researchTableMenu)) {
                return;
            }

            CompoundTag libraryTag = payload.libraryTag();

            researchTableMenu.clearSyncedLibraryLevels();

            for (String key : libraryTag.getAllKeys()) {
                ResourceLocation enchantmentId = ResourceLocation.tryParse(key);

                if (enchantmentId == null) {
                    continue;
                }

                researchTableMenu.setSyncedLibraryLevel(
                        enchantmentId,
                        libraryTag.getInt(key)
                );
            }

            researchTableMenu.refreshVisibleEnchantments();
        });
    }
}