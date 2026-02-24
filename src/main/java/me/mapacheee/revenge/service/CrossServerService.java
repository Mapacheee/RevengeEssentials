package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.data.PendingTeleport;
import me.mapacheee.revenge.data.PendingTeleportMessage;
import me.mapacheee.revenge.data.PendingTeleportRepository;
import com.mongodb.client.model.Filters;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class CrossServerService {

    private final PendingTeleportRepository pendingTeleportRepository;
    private final Plugin plugin;
    private final String PENDING_TP_CHANNEL = "revenge:pending_tp";

    @Inject
    public CrossServerService(PendingTeleportRepository pendingTeleportRepository, Plugin plugin) {
        this.pendingTeleportRepository = pendingTeleportRepository;
        this.plugin = plugin;
    }

    @OnEnable
    public void onEnable() {
        RevengeCoreAPI.get().getChannelService().subscribe(PENDING_TP_CHANNEL, PendingTeleportMessage.class, msg -> {
            if (msg.targetServer.equals(getServerName())) {
                setPendingTeleport(UUID.fromString(msg.uuid), msg.targetServer, msg.world, msg.x, msg.y, msg.z, msg.yaw,
                        msg.pitch);
            }
        }, plugin.getSLF4JLogger());
    }

    public void setPendingTeleport(UUID uuid, String server, String world, double x, double y, double z, float yaw,
            float pitch) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            pendingTeleportRepository.delete(Filters.eq("uuid", uuid.toString()));
            PendingTeleport pending = new PendingTeleport(uuid.toString(), server, world, x, y, z, yaw, pitch);
            pendingTeleportRepository.save(pending);
        });
    }

    public CompletableFuture<PendingTeleport> getPendingTeleport(UUID uuid) {
        return CompletableFuture
                .supplyAsync(() -> pendingTeleportRepository.findOne(Filters.eq("uuid", uuid.toString())));
    }

    public void clearPendingTeleport(UUID uuid) {
        Bukkit.getAsyncScheduler().runNow(plugin,
                task -> pendingTeleportRepository.delete(Filters.eq("uuid", uuid.toString())));
    }

    public void teleportCrossServer(Player player, String server, String world, double x, double y, double z, float yaw,
            float pitch) {

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            PendingTeleportMessage msg = new PendingTeleportMessage(
                    getServerName(),
                    player.getUniqueId().toString(),
                    server,
                    world,
                    x, y, z, yaw, pitch);
            try {
                RevengeCoreAPI.get().getChannelService().publish(PENDING_TP_CHANNEL, msg);
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to publish PendingTeleport to Valkey", e);
            }
        });
    }

    public void applyPendingTeleport(Player player) {
        getPendingTeleport(player.getUniqueId()).thenAccept(pending -> {
            if (pending == null)
                return;
            World w = Bukkit.getWorld(pending.getWorld());
            if (w == null)
                return;
            Location loc = new Location(w, pending.getX(), pending.getY(), pending.getZ(), pending.getYaw(),
                    pending.getPitch());
            player.getScheduler().run(plugin, task -> {
                player.setMetadata("revenge_skip_back_save", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                player.teleportAsync(loc).thenAccept(result -> {
                    player.removeMetadata("revenge_skip_back_save", plugin);
                });
            }, null);
            clearPendingTeleport(player.getUniqueId());
        });
    }

    public String getServerName() {
        return RevengeCoreAPI.get().getServerName();
    }
}
