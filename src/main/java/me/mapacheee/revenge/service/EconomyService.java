package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.mongodb.client.model.Filters;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnDisable;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossEconomyUpdateMessage;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.data.EconomyData;
import me.mapacheee.revenge.data.EconomyRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EconomyService {

    private final EconomyRepository economyRepository;
    public final PlayerDataService playerDataService;
    private final Container<Config> config;
    private final Plugin plugin;
    private ChannelService channelService;
    private final Map<UUID, Double> balanceCache = new ConcurrentHashMap<>();

    @Inject
    public EconomyService(EconomyRepository economyRepository, PlayerDataService playerDataService, Container<Config> config, Plugin plugin) {
        this.economyRepository = economyRepository;
        this.playerDataService = playerDataService;
        this.config = config;
        this.plugin = plugin;
    }

    private ChannelService getChannelService() {
        if (channelService == null) {
            channelService = RevengeCoreAPI.get().getChannelService();
        }
        return channelService;
    }

    @OnEnable
    public void setupSubscriptions() {
        getChannelService().subscribe("revenge:economy_update", CrossEconomyUpdateMessage.class, this::handleCrossEconomyUpdate, plugin.getSLF4JLogger());
    }

    @OnDisable
    public void unregisterSubscriptions() {
        saveAllCachedSync();
    }

    private void handleCrossEconomyUpdate(CrossEconomyUpdateMessage message) {
        if (message.serverId.equalsIgnoreCase(getServerName())) {
            return;
        }
        UUID uuid = UUID.fromString(message.playerUuid());
        
        if (balanceCache.containsKey(uuid)) {
            balanceCache.put(uuid, message.newBalance());
        }
    }

    public void loadPlayerBalance(Player player) {
        CompletableFuture.supplyAsync(() -> economyRepository.findOne(Filters.eq("uuid", player.getUniqueId().toString())))
            .thenAccept(data -> {
                double startingBal = config.get().startingBalance();
                if (data == null) {
                    balanceCache.put(player.getUniqueId(), startingBal);
                    EconomyData newData = new EconomyData(player.getUniqueId().toString(), player.getName(), startingBal);
                    CompletableFuture.runAsync(() -> economyRepository.save(newData));
                } else {
                    balanceCache.put(player.getUniqueId(), data.getBalance());
                    if (!data.getName().equals(player.getName())) {
                        data.setName(player.getName());
                        CompletableFuture.runAsync(() -> economyRepository.save(data));
                    }
                }
            });
    }

    public void savePlayerBalance(Player player) {
        Double bal = balanceCache.remove(player.getUniqueId());
        if (bal != null) {
            saveBalanceToMongo(player.getUniqueId().toString(), player.getName(), bal);
        }
    }

    private void saveAllCachedSync() {
        for (Map.Entry<UUID, Double> entry : balanceCache.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            String name = p != null ? p.getName() : "Unknown";
            EconomyData data = economyRepository.findOne(Filters.eq("uuid", entry.getKey().toString()));
            if (data == null) {
                data = new EconomyData(entry.getKey().toString(), name, entry.getValue());
            } else {
                data.setBalance(entry.getValue());
            }
            economyRepository.save(data);
        }
        balanceCache.clear();
    }

    private CompletableFuture<Void> saveBalanceToMongo(String uuid, String name, double balance) {
        return CompletableFuture.supplyAsync(() -> economyRepository.findOne(Filters.eq("uuid", uuid)))
            .thenCompose(data -> {
                if (data == null) {
                    return CompletableFuture.runAsync(() -> economyRepository.save(new EconomyData(uuid, name, balance)));
                } else {
                    data.setBalance(balance);
                    return CompletableFuture.runAsync(() -> economyRepository.save(data));
                }
            });
    }

    public CompletableFuture<Double> getBalance(UUID uuid, String name) {
        if (balanceCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(balanceCache.get(uuid));
        }
        return CompletableFuture.supplyAsync(() -> economyRepository.findOne(Filters.eq("uuid", uuid.toString())))
            .thenApply(data -> {
            if (data != null) return data.getBalance();
            return config.get().startingBalance();
        });
    }

    public boolean hasBalance(UUID uuid, String name, double amount) {
        if (balanceCache.containsKey(uuid)) {
            return balanceCache.get(uuid) >= amount;
        }
        try {
            return getBalance(uuid, name).get() >= amount;
        } catch (Exception e) {
            return false;
        }
    }

    public CompletableFuture<Boolean> setBalance(UUID uuid, String name, double amount) {
        if (amount < 0) return CompletableFuture.completedFuture(false);
        
        return saveBalanceToMongo(uuid.toString(), name, amount).thenApply(v -> {
            if (balanceCache.containsKey(uuid)) {
                balanceCache.put(uuid, amount);
            }
            publishUpdate(uuid, name, amount);
            return true;
        });
    }

    public CompletableFuture<Boolean> addBalance(UUID uuid, String name, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return getBalance(uuid, name).thenCompose(current -> {
            double newBalance = current + amount;
            return setBalance(uuid, name, newBalance);
        });
    }

    public CompletableFuture<Boolean> removeBalance(UUID uuid, String name, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return getBalance(uuid, name).thenCompose(current -> {
            if (current < amount) return CompletableFuture.completedFuture(false);
            double newBalance = current - amount;
            return setBalance(uuid, name, newBalance);
        });
    }

    private void publishUpdate(UUID uuid, String name, double newBalance) {
        getChannelService().publish("revenge:economy_update", new CrossEconomyUpdateMessage(getServerName(), uuid.toString(), name, newBalance));
    }

    public String getServerName() {
        return RevengeCoreAPI.get().getServerName();
    }
}
