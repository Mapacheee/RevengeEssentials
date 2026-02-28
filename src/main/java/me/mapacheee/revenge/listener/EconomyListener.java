package me.mapacheee.revenge.listener;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.revenge.service.EconomyService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@ListenerComponent
public class EconomyListener implements Listener {

    private final EconomyService economyService;

    @Inject
    public EconomyListener(EconomyService economyService) {
        this.economyService = economyService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        economyService.loadPlayerBalance(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        economyService.savePlayerBalance(event.getPlayer());
    }
}
