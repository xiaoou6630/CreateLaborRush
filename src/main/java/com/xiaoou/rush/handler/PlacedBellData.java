package com.xiaoou.rush.handler;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 放置在地上的附魔钟持久化存储（MC 的钟无 BlockEntity，附魔信息在放置后丢失。
 * 存到世界数据中，重启不丢）。
 */
public class PlacedBellData extends SavedData {

    private static final String NAME = "createlaborrush_bells";
    private final Map<String, int[]> bells = new HashMap<>();

    public PlacedBellData() {
    }

    public PlacedBellData(CompoundTag tag, HolderLookup.Provider lookup) {
        for (String key : tag.getAllKeys()) {
            bells.put(key, tag.getIntArray(key));
        }
    }

    public static PlacedBellData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(PlacedBellData::new, PlacedBellData::new), NAME
        );
    }

    public int[] get(String key) {
        return bells.get(key);
    }

    public void put(String key, int[] value) {
        bells.put(key, value);
        setDirty();
    }

    public boolean remove(String key) {
        if (bells.remove(key) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public Map<String, int[]> all() {
        return new HashMap<>(bells);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        for (Map.Entry<String, int[]> entry : bells.entrySet()) {
            tag.putIntArray(entry.getKey(), entry.getValue());
        }
        return tag;
    }
}
