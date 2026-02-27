package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.mongodb.client.model.Filters;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.HomeData;
import me.mapacheee.revenge.data.HomeRepository;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import me.mapacheee.revenge.listener.TeleportWarmupListener;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Service
public class HomeService {

    private final HomeRepository homeRepository;
    private final CrossServerService crossServerService;
    private final Container<Config> config;
    private final Container<Messages> messages;
    private final Plugin plugin;
    private final TeleportWarmupListener teleportWarmupListener;

    @Inject
    public HomeService(HomeRepository homeRepository, CrossServerService crossServerService, Container<Config> config,
            Container<Messages> messages, Plugin plugin, TeleportWarmupListener teleportWarmupListener) {
        this.homeRepository = homeRepository;
        this.crossServerService = crossServerService;
        this.config = config;
        this.messages = messages;
        this.plugin = plugin;
        this.teleportWarmupListener = teleportWarmupListener;
    }

    public CompletableFuture<Boolean> setHome(Player player, String name) {
        return getHomes(player.getUniqueId().toString()).thenApply(homes -> {
            int maxHomes = getMaxHomes(player);
            if (homes.size() >= maxHomes) {
                boolean exists = homes.stream().anyMatch(h -> h.getName().equalsIgnoreCase(name));
                if (!exists)
                    return false;
            }

            homeRepository.delete(Filters.and(
                    Filters.eq("uuid", player.getUniqueId().toString()),
                    Filters.eq("name", name.toLowerCase())));

            Location loc = player.getLocation();
            HomeData home = new HomeData(
                    player.getUniqueId().toString(), name.toLowerCase(), getServerName(),
                    RevengeCoreAPI.get().getServerDisplayName(), false,
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            homeRepository.save(home);
            return true;
        });
    }

    public CompletableFuture<Void> deleteHome(String uuid, String name) {
        return CompletableFuture.runAsync(() -> homeRepository.delete(Filters.and(
                Filters.eq("uuid", uuid),
                Filters.eq("name", name.toLowerCase()))));
    }

    public CompletableFuture<HomeData> getHome(String uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            HomeData home = homeRepository.findOne(Filters.and(
                    Filters.eq("uuid", uuid),
                    Filters.eq("name", name.toLowerCase())));

            return home;
        });
    }

    public CompletableFuture<Void> setDefaultHome(String uuid, String name) {
        return getHomes(uuid).thenAccept(homes -> {
            for (HomeData home : homes) {
                homeRepository.delete(Filters.and(
                        Filters.eq("uuid", uuid),
                        Filters.eq("name", home.getName().toLowerCase())));

                if (home.getName().equalsIgnoreCase(name)) {
                    home.setDefaultHome(true);
                } else {
                    home.setDefaultHome(false);
                }

                home.id(null);
                homeRepository.save(home);
            }
        });
    }

    public CompletableFuture<HomeData> getDefaultHome(String uuid) {
        return getHomes(uuid)
                .thenApply(homes -> homes.stream().filter(HomeData::isDefaultHome).findFirst().orElse(null));
    }

    public CompletableFuture<Collection<HomeData>> getHomes(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Collection<HomeData> homes = homeRepository.find(Filters.eq("uuid", uuid));
            return homes;
        });
    }

    public void teleportToHome(Player player, HomeData home) {
        teleportWarmupListener.startWarmup(player, () -> {
            String currentServer = getServerName();

            if (home.getServer().equals(currentServer)) {
                World world = Bukkit.getWorld(home.getWorld());
                if (world == null)
                    return;
                Location loc = new Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
                player.getScheduler().run(plugin, task -> {
                    player.teleportAsync(loc);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            messages.get().homeTeleported(),
                            Placeholder.unparsed("home", home.getName())));
                }, null);
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().crossServerTeleporting()));
                crossServerService.teleportCrossServer(player, home.getServer(), home.getWorld(), home.getX(), home.getY(),
                        home.getZ(), home.getYaw(), home.getPitch(), false);
            }
        });
    }

    public int getMaxHomes(Player player) {
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("revenge.home." + i))
                return i;
        }
        return config.get().defaultMaxHomes();
    }

    private String getServerName() {
        return RevengeCoreAPI.get().getServerName();
    }
}
