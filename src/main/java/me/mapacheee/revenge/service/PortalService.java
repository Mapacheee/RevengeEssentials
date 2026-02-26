package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.data.PortalData;
import me.mapacheee.revenge.data.PortalRepository;
import com.mongodb.client.model.Filters;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

@Service
public class PortalService {

    private final PortalRepository portalRepository;
    private final Plugin plugin;

    @Inject
    public PortalService(PortalRepository portalRepository, Plugin plugin) {
        this.portalRepository = portalRepository;
        this.plugin = plugin;
    }

    public void saveReturnLocation(Player player) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String uuid = player.getUniqueId().toString();
            Location loc = player.getLocation();

            portalRepository.delete(Filters.eq("uuid", uuid));

            PortalData data = new PortalData(
                    uuid,
                    RevengeCoreAPI.get().getServerName(),
                    loc.getWorld().getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ(),
                    loc.getYaw(),
                    loc.getPitch());

            portalRepository.save(data);
        });
    }

    public CompletableFuture<PortalData> getAndClearReturnLocation(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PortalData data = portalRepository.findOne(Filters.eq("uuid", uuid));
            if (data != null) {
                portalRepository.delete(Filters.eq("uuid", uuid));
            }
            return data;
        });
    }
}
