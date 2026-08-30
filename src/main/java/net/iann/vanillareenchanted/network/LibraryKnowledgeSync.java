package net.iann.vanillareenchanted.network;

import net.iann.vanillareenchanted.library.LibraryScanner;
import net.iann.vanillareenchanted.menu.ResearchTableMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class LibraryKnowledgeSync {

    public static void sendToClient(
            ServerPlayer serverPlayer,
            ResearchTableMenu menu
    ) {
        CompoundTag libraryTag = new CompoundTag();

        LibraryScanner.LibraryScanResult scanResult = menu.scanLibrary();

        for (LibraryScanner.LibraryKnowledgeEntry entry : scanResult.getKnowledgeEntries()) {
            ResourceLocation enchantmentId = entry.enchantmentId();
            String enchantmentIdText = enchantmentId.toString();

            int previousLevel = libraryTag.getInt(enchantmentIdText);
            int newLevel = Math.max(previousLevel, entry.level());

            libraryTag.putInt(enchantmentIdText, newLevel);
        }

        PacketDistributor.sendToPlayer(
                serverPlayer,
                new SyncLibraryKnowledgePayload(libraryTag)
        );
    }
}