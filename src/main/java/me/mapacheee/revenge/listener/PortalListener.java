package me.mapacheee.revenge.listener;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.service.CrossServerService;
import me.mapacheee.revenge.service.PortalService;
import me.mapacheee.revenge.service.SpawnService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.plugin.Plugin;

@ListenerComponent
public class PortalListener implements Listener {

    private final PortalService portalService;
    private final CrossServerService crossServerService;
    private final SpawnService spawnService;
    private final Plugin plugin;

    @Inject
    public PortalListener(PortalService portalService, CrossServerService crossServerService, SpawnService spawnService,
            Plugin plugin) {
        this.portalService = portalService;
        this.crossServerService = crossServerService;
        this.spawnService = spawnService;
        this.plugin = plugin;
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock())
            return;

        Player player = event.getPlayer();
        Block feet = event.getTo().getBlock();
        Block head = event.getTo().clone().add(0, 1, 0).getBlock();

        Material type = null;
        if (feet.getType() == Material.NETHER_PORTAL || head.getType() == Material.NETHER_PORTAL) {
            type = Material.NETHER_PORTAL;
        } else if (feet.getType() == Material.END_PORTAL || head.getType() == Material.END_PORTAL) {
            type = Material.END_PORTAL;
        }

        if (type == null)
            return;

        handlePortal(player, type, event.getFrom());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        TeleportCause cause = event.getCause();
        if (cause == TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
            handlePortal(event.getPlayer(), Material.NETHER_PORTAL, event.getFrom());
        } else if (cause == TeleportCause.END_PORTAL) {
            event.setCancelled(true);
            handlePortal(event.getPlayer(), Material.END_PORTAL, event.getFrom());
        }
    }

    private void handlePortal(Player player, Material type, Location from) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < 3000) {
            return;
        }

        cooldowns.put(uuid, now);

        player.setPortalCooldown(80);

        String currentServerName = RevengeCoreAPI.get().getServerName();
        String netherServerName = RevengeCoreAPI.get().getNetherServer();
        String endServerName = RevengeCoreAPI.get().getEndServer();
        boolean inNetherTarget = currentServerName.equals(netherServerName);
        boolean inEndTarget = currentServerName.equals(endServerName);
        boolean finalInNetherTarget = inNetherTarget;
        boolean finalInEndTarget = inEndTarget;
        Material finalType = type;

        if ((finalInNetherTarget && finalType == Material.NETHER_PORTAL) || (finalInEndTarget && finalType == Material.END_PORTAL)) {
            portalService.getAndClearReturnLocation(uuid.toString()).thenAccept(data -> {
                player.getScheduler().run(plugin, task -> {
                    if (finalInEndTarget && finalType == Material.END_PORTAL) {
                        if (spawnService.hasSpawn()) {
                            spawnService.teleportToSpawn(player);
                        } 
                        else if (data != null) {
                            crossServerService.teleportCrossServer(player, data.getReturnServer(), 
                            data.getReturnWorld(), data.getX(), data.getY(), data.getZ(), data.getYaw(),
                            data.getPitch(), false);
                        }
                    } 
                    else {
                        if (data != null) {
                            crossServerService.teleportCrossServer(player, data.getReturnServer(), 
                            data.getReturnWorld(), data.getX(), data.getY(), data.getZ(), data.getYaw(),
                            data.getPitch(), false);
                        } 
                        else {                            
                            double returnX = Math.max(-50000.0, Math.min(50000.0, from.getX() * 8.0));
                            double returnZ = Math.max(-50000.0, Math.min(50000.0, from.getZ() * 8.0));
                            
                            String targetOverworld = RevengeCoreAPI.get().getOverworldServer();
                            if (targetOverworld == null || targetOverworld.isEmpty()) targetOverworld = "survival";
                            
                            crossServerService.teleportCrossServer(player, targetOverworld, "world", returnX, from.getY(),
                            returnZ, from.getYaw(), from.getPitch(), true);
                        }
                    }
                }, null);
            });
            return;
        }

        if (finalType == Material.NETHER_PORTAL && !finalInNetherTarget) {
            portalService.saveReturnLocation(player);

            double x = from.getX() / 8.0;
            double z = from.getZ() / 8.0;

            x = Math.max(30.0, Math.min(100.0, Math.abs(x)));
            z = Math.max(30.0, Math.min(100.0, Math.abs(z)));

            crossServerService.teleportCrossServer(player, netherServerName, "world_nether", x, from.getY(), z,
            from.getYaw(), from.getPitch(), true);
            return;
        }

        if (finalType == Material.END_PORTAL && !finalInEndTarget) {
            portalService.saveReturnLocation(player);

            crossServerService.teleportCrossServer(player, endServerName, "world_the_end", 100.0, 50.0, 0.0,
            from.getYaw(), from.getPitch(), false);
        }
    }
}
