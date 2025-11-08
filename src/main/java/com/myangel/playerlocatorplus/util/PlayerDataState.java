package com.myangel.playerlocatorplus.util;

import com.myangel.playerlocatorplus.PlayerLocatorPlus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataState extends SavedData {
    private static final String DATA_NAME = PlayerLocatorPlus.MODID + "_player_data";

    private final Map<UUID, PlayerData> players = new HashMap<>();

    public static PlayerDataState get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(PlayerDataState::load, PlayerDataState::new, DATA_NAME);
    }

    private static PlayerDataState load(CompoundTag tag) {
        PlayerDataState state = new PlayerDataState();
        for (String key : tag.getAllKeys()) {
            CompoundTag playerTag = tag.getCompound(key);
            PlayerData data = new PlayerData();
            if (playerTag.contains("customColor")) {
                data.setCustomColor(playerTag.getInt("customColor"));
            }
            try {
                state.players.put(UUID.fromString(key), data);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putInt("customColor", entry.getValue().customColor());
            tag.put(entry.getKey().toString(), playerTag);
        }
        return tag;
    }

    public PlayerData get(UUID uuid) {
        return players.computeIfAbsent(uuid, id -> {
            setDirty();
            return new PlayerData();
        });
    }

    public void setCustomColor(UUID uuid, int color) {
        PlayerData data = get(uuid);
        data.setCustomColor(color);
        setDirty();
    }
}
