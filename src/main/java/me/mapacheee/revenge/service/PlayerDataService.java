package me.mapacheee.revenge.service;

import com.google.gson.Gson;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.data.PlayerData;
import me.mapacheee.revenge.data.PlayerRepository;
import me.mapacheee.revenge.channel.PlayerUpdateMessage;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import com.mongodb.client.model.Filters;
import com.google.inject.Inject;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PlayerDataService {

    private final PlayerRepository repository;
    private final ChannelService channelService;
    private final Gson gson = new Gson();

    @Inject
    public PlayerDataService(PlayerRepository repository) {
        this.repository = repository;
        this.channelService = RevengeCoreAPI.get().getChannelService();
    }

    public Set<String> getAllPlayerNames() {
        RedissonClient redisson = RevengeCoreAPI.get().getRedisService().client();
        RMapCache<String, String> uuidMap = redisson.getMapCache("revenge:uuidmap");
        return uuidMap.readAllKeySet();
    }

    public CompletableFuture<UUID> getUUIDFromName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            RedissonClient redisson = RevengeCoreAPI.get().getRedisService().client();
            RMapCache<String, String> uuidMap = redisson.getMapCache("revenge:uuidmap");
            
            String cachedUuid = uuidMap.get(name.toLowerCase());
            if (cachedUuid != null) {
                return UUID.fromString(cachedUuid);
            }
            
            PlayerData data = repository.findOne(Filters.regex("name", "^" + name + "$", "i"));
            return data != null ? UUID.fromString(data.getUuid()) : null;
        });
    }

    public CompletableFuture<PlayerData> getPlayerData(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerData data = repository.findOne(Filters.eq("uuid", uuid.toString()));
            if (data == null) {
                data = new PlayerData(uuid.toString(), name);
                repository.save(data);
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
