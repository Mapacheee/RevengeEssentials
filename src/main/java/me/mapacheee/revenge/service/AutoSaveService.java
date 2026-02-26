package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

@Service
public class AutoSaveService {

    private final Container<Config> config;
    private final InventorySyncService inventorySyncService;
    private final JoinQuitService joinQuitService;
    private final Plugin plugin;

    @Inject
    public AutoSaveService(Container<Config> config, InventorySyncService inventorySyncService, JoinQuitService joinQuitService, Plugin plugin) {
        this.config = config;
        this.inventorySyncService = inventorySyncService;
        this.joinQuitService = joinQuitService;
        this.plugin = plugin;
    }

    @OnEnable
    public void onEnable() {
        long intervalMinutes = config.get().autoSaveIntervalMinutes();
        if (intervalMinutes <= 0) return;

        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                inventorySyncService.savePlayerData(player);
                joinQuitService.saveLogoutLocation(player);
            }
            plugin.getSLF4JLogger().info("Global Auto-Save completed for {} online players.", Bukkit.getOnlinePlayers().size());
        }, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
    }
}
