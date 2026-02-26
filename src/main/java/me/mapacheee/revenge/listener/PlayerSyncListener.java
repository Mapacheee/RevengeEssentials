package me.mapacheee.revenge.listener;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.revenge.service.BackService;
import me.mapacheee.revenge.service.CrossServerService;
import me.mapacheee.revenge.service.InventorySyncService;
import me.mapacheee.revenge.service.SpawnService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

@ListenerComponent
public class PlayerSyncListener implements Listener {

    private final InventorySyncService inventorySyncService;
    private final SpawnService spawnService;
    private final BackService backService;
    private final CrossServerService crossServerService;

    @Inject
    public PlayerSyncListener(InventorySyncService inventorySyncService, SpawnService spawnService,
            BackService backService, CrossServerService crossServerService) {
        this.inventorySyncService = inventorySyncService;
        this.spawnService = spawnService;
        this.backService = backService;
        this.crossServerService = crossServerService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        inventorySyncService.loadPlayerData(player);

        crossServerService.applyPendingTeleport(player);

        if (spawnService.shouldForceSpawnOnJoin() && spawnService.hasSpawn()) {
            spawnService.teleportToSpawn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        inventorySyncService.savePlayerData(player);
        backService.saveBackLocation(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        backService.saveBackLocation(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND
                || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            backService.saveBackLocation(event.getPlayer(), event.getFrom());
        }
    }
}
