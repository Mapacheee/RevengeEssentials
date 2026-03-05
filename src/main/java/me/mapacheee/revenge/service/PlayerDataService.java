package me.mapacheee.revenge.service;

import com.google.gson.Gson;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.data.PlayerData;
import me.mapacheee.revenge.data.PlayerRepository;
import me.mapacheee.revenge.channel.PlayerUpdateMessage;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;
import com.mongodb.client.model.Filters;
import com.google.inject.Inject;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PlayerDataService {

    private final PlayerRepository repository;
    private final ChannelService channelService;
    private final Plugin plugin;
    private final Gson gson = new Gson();
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();

    @Inject
    public PlayerDataService(PlayerRepository repository, Plugin plugin) {
        this.repository = repository;
        this.plugin = plugin;
        this.channelService = RevengeCoreAPI.get().getChannelService();
    }

    @OnEnable
    public void onEnable() {
        channelService.subscribe("player:update", PlayerUpdateMessage.class, this::handlePlayerUpdate, plugin.getSLF4JLogger());
    }

    private void handlePlayerUpdate(PlayerUpdateMessage msg) {
        UUID uuid = UUID.fromString(msg.uuid);
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            String field = msg.field;
            String value = msg.value;
            if (field.equals("kills")) {
                data.setKills(Integer.parseInt(value));
            } else if (field.equals("deaths")) {
                data.setDeaths(Integer.parseInt(value));
            } else if (field.equals("xp")) {
                data.setXp(Integer.parseInt(value));
            }
        }
    }

    public void loadPlayerData(Player player) {
        getPlayerData(player.getUniqueId(), player.getName()).thenAccept(data -> {
            playerDataCache.put(player.getUniqueId(), data);
        });
    }

    public void unloadPlayerData(Player player) {
        playerDataCache.remove(player.getUniqueId());
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

    public CompletableFuture<PlayerData> loadFreshPlayerData(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerData data = repository.findOne(Filters.eq("uuid", uuid.toString()));
            if (data == null) {
                data = new PlayerData(uuid.toString(), name);
                repository.save(data);
            }
            playerDataCache.put(uuid, data);
            return data;
        });
    }

    public CompletableFuture<PlayerData> getPlayerData(UUID uuid, String name) {
        PlayerData cached = playerDataCache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return loadFreshPlayerData(uuid, name);
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

    public CompletableFuture<Integer> getPlayerKills(UUID uuid, String name) {
        return getPlayerData(uuid, name).thenApply(PlayerData::getKills);
    }

    public CompletableFuture<Integer> getPlayerDeaths(UUID uuid, String name) {
        return getPlayerData(uuid, name).thenApply(PlayerData::getDeaths);
    }

    public void incrementPlayerKills(UUID uuid, String name) {
        getPlayerData(uuid, name).thenAccept(data -> {
            int newKills = data.getKills() + 1;
            data.setKills(newKills);
            repository.save(data);
            channelService.publish("player:update",
                    new PlayerUpdateMessage(uuid.toString(), name, "kills", String.valueOf(newKills)));
        });
    }

    public void incrementPlayerDeaths(UUID uuid, String name) {
        getPlayerData(uuid, name).thenAccept(data -> {
            int newDeaths = data.getDeaths() + 1;
            data.setDeaths(newDeaths);
            repository.save(data);
            channelService.publish("player:update",
                    new PlayerUpdateMessage(uuid.toString(), name, "deaths", String.valueOf(newDeaths)));
        });
    }
}
