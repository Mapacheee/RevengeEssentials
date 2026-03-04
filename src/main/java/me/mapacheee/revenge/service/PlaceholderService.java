package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.placeholder.EssentialsPlaceholderExpansion;
import org.bukkit.Bukkit;
import org.slf4j.Logger;

@Service
public class PlaceholderService {

    private final PlayerDataService playerDataService;

    @Inject
    public PlaceholderService(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    @OnEnable
    public void onEnable(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EssentialsPlaceholderExpansion(playerDataService).register();
            logger.info("PlaceholderAPI expansion registered successfully!");
        } else {
            logger.warn("PlaceholderAPI not found, expansion will not be registered.");
        }
    }
}
