package net.iann.vanillareenchanted.enchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** One authoritative copy per horse, even if a bound horn is copied in creative. */
public final class HorsekeeperStorage extends SavedData {
    private final Map<UUID, CompoundTag> horses = new HashMap<>();

    public static HorsekeeperStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(HorsekeeperStorage::new, HorsekeeperStorage::load), "iannvanillareenchanted_horsekeeper");
    }

    private static HorsekeeperStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        var storage = new HorsekeeperStorage();
        for (Tag entry : tag.getList("Horses", Tag.TAG_COMPOUND)) {
            CompoundTag horse = (CompoundTag) entry;
            if (horse.hasUUID("UUID")) storage.horses.put(horse.getUUID("UUID"), horse.copy());
        }
        return storage;
    }

    public CompoundTag horse(UUID id) {
        CompoundTag horse = horses.get(id);
        return horse == null ? null : horse.copy();
    }

    public boolean isStored(UUID id) { return horses.containsKey(id); }

    public String storedName(UUID id, HolderLookup.Provider registries) {
        CompoundTag horse = horses.get(id);
        if (horse == null || !horse.contains("CustomName", Tag.TAG_STRING)) return "";
        var name = net.minecraft.network.chat.Component.Serializer.fromJson(horse.getString("CustomName"), registries);
        return name == null ? "" : name.getString();
    }

    public void store(UUID id, CompoundTag horse) {
        horses.put(id, horse.copy());
        setDirty();
    }

    public void release(UUID id) {
        horses.remove(id);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        horses.values().forEach(horse -> entries.add(horse.copy()));
        tag.put("Horses", entries);
        return tag;
    }
}
