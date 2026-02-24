package me.mapacheee.revenge.service;

import com.google.gson.Gson;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.data.PlayerData;
import me.mapacheee.revenge.data.PlayerRepository;
import me.mapacheee.revenge.channel.PlayerUpdateMessage;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PlayerDataService {

    private final PlayerRepository repository;
    private final ChannelService channelService;
    private final Gson gson = new Gson();
    private final Set<String> allPlayerNames = ConcurrentHashMap.newKeySet();

    @Inject
    public PlayerDataService(PlayerRepository repository) {
        this.repository = repository;
        this.channelService = RevengeCoreAPI.get().getChannelService();

        CompletableFuture.runAsync(() -> {
            for (PlayerData pd : repository.findAll()) {
                allPlayerNames.add(pd.getName());
            }
        });
    }

    public Set<String> getAllPlayerNames() {
        return allPlayerNames;
    }

    public CompletableFuture<PlayerData> getPlayerData(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerData data = repository.findOne(com.mongodb.client.model.Filters.eq("uuid", uuid.toString()));
            if (data == null) {
                data = new PlayerData(uuid.toString(), name);
                repository.save(data);
                allPlayerNames.add(name);
            }
            return data;
        });
    }

    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> repository.save(data));
    }

    public void setPlayerXp(UUID uuid, String name, int xp) {
        getPlayerData(uuid, name).thenAccept(data -> {
            data.setXp(xp);
            repository.save(data);
            channelService.publish("player:update",
                    new PlayerUpdateMessage(uuid.toString(), name, "xp", String.valueOf(xp)));
        });
    }

    public CompletableFuture<Integer> getPlayerXp(UUID uuid, String name) {
        return getPlayerData(uuid, name).thenApply(PlayerData::getXp);
    }

    public void setPlayerLastLocation(UUID uuid, String name, Location location) {
        getPlayerData(uuid, name).thenAccept(data -> {
            data.setLastLocation(gson.toJson(location.serialize()));
            repository.save(data);
        });
    }

    public CompletableFuture<Location> getPlayerLastLocation(UUID uuid, String name) {
        return getPlayerData(uuid, name).thenApply(data -> {
            return null;
        });
    }

    public void setPlayerInventory(UUID uuid, String name, ItemStack[] inventory) {
        getPlayerData(uuid, name).thenAccept(data -> {
            data.setInventory(gson.toJson(inventory));
            repository.save(data);
        });
    }

    public CompletableFuture<ItemStack[]> getPlayerInventory(UUID uuid, String name) {
        return getPlayerData(uuid, name).thenApply(data -> {
            return gson.fromJson(data.getInventory(), ItemStack[].class);
        });
    }
}
