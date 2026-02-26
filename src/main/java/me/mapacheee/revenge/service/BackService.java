package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.mongodb.client.model.Filters;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.BackData;
import me.mapacheee.revenge.data.BackRepository;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

@Service
public class BackService {

    private final BackRepository backRepository;
    private final CrossServerService crossServerService;
    private final Container<Messages> messages;
    private final Plugin plugin;

    @Inject
    public BackService(BackRepository backRepository, CrossServerService crossServerService,
            Container<Messages> messages, Plugin plugin) {
        this.backRepository = backRepository;
        this.crossServerService = crossServerService;
        this.messages = messages;
        this.plugin = plugin;
    }

    public void saveBackLocation(Player player) {
        Location loc = player.getLocation();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            backRepository.delete(Filters.eq("uuid", player.getUniqueId().toString()));
            BackData back = new BackData(
                    player.getUniqueId().toString(), getServerName(),
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            backRepository.save(back);
        });
    }

    public void saveBackLocation(Player player, Location loc) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            backRepository.delete(Filters.eq("uuid", player.getUniqueId().toString()));
            BackData back = new BackData(
                    player.getUniqueId().toString(), getServerName(),
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            backRepository.save(back);
        });
    }

    public CompletableFuture<BackData> getBackLocation(String uuid) {
        return CompletableFuture.supplyAsync(() -> backRepository.findOne(Filters.eq("uuid", uuid)));
    }

    public void teleportBack(Player player) {
        getBackLocation(player.getUniqueId().toString()).thenAccept(back -> {
            if (back == null) {
                player.getScheduler().run(plugin,
                        task -> player
                                .sendMessage(MiniMessage.miniMessage().deserialize(messages.get().backNoLocation())),
                        null);
                return;
            }

            String currentServer = getServerName();
            if (back.getServer().equals(currentServer)) {
                World world = Bukkit.getWorld(back.getWorld());
                if (world == null)
                    return;
                Location loc = new Location(world, back.getX(), back.getY(), back.getZ(), back.getYaw(),
                        back.getPitch());
                player.getScheduler().run(plugin, task -> {
                    player.teleportAsync(loc);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().backTeleported()));
                }, null);
            } else {
                player.getScheduler().run(plugin, task -> {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().backCrossServer()));
                    crossServerService.teleportCrossServer(player, back.getServer(), back.getWorld(), back.getX(),
                            back.getY(), back.getZ(), back.getYaw(), back.getPitch(), false);
                }, null);
            }
        });
    }

    private String getServerName() {
        return RevengeCoreAPI.get().getServerName();
    }
}
