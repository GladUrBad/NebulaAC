package com.gladurbad.nebula.manager;

import com.gladurbad.nebula.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> playerDataMap = new HashMap<UUID, PlayerData>();

    public PlayerData getPlayerData(final Player player) {
        return playerDataMap.get(player.getUniqueId());
    }

    public void add(final Player player) {
        playerDataMap.put(player.getUniqueId(), new PlayerData(player));
    }

    public boolean has(final Player player) {
        return this.playerDataMap.containsKey(player.getUniqueId());
    }

    public void remove(final Player player) {
        playerDataMap.remove(player.getUniqueId());
    }

    public Collection<PlayerData> getAllData() {
        return playerDataMap.values();
    }
}
