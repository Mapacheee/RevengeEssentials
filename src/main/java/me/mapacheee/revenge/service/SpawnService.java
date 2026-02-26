package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.mongodb.client.model.Filters;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.SpawnData;
import me.mapacheee.revenge.data.SpawnRepository;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

@Service
public class SpawnService {

    private final SpawnRepository spawnRepository;
    private final Container<Config> config;
    private final Container<Messages> messages;
    private final Plugin plugin;
    private SpawnData cachedSpawn;

    private final CrossServerService crossServerService;

    @Inject
    public SpawnService(SpawnRepository spawnRepository, CrossServerService crossServerService,
            Container<Config> config, Container<Messages> messages,
            Plugin plugin) {
        this.spawnRepository = spawnRepository;
        this.crossServerService = crossServerService;
        this.config = config;
        this.messages = messages;
        this.plugin = plugin;
    }

    @OnEnable
    void loadSpawn() {
        Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
            cachedSpawn = spawnRepository.findOne(Filters.exists("server"));

        }, 2, TimeUnit.SECONDS);
    }

    public CompletableFuture<Void> setSpawn(Player player) {
        return CompletableFuture.runAsync(() -> {
            Location loc = player.getLocation();
            spawnRepository.delete(Filters.exists("server"));

            SpawnData spawn = new SpawnData(getServerName(), loc.getWorld().getName(), loc.getX(), loc.getY(),
                    loc.getZ(), loc.getYaw(), loc.getPitch());
            spawnRepository.save(spawn);
            cachedSpawn = spawn;
        });
    }

    public void teleportToSpawn(Player player) {

        if (cachedSpawn == null) {
            cachedSpawn = spawnRepository.findOne(Filters.exists("server"));
        }

        if (cachedSpawn == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().spawnNotSet()));
            return;
        }

        if (cachedSpawn.getServer().equals(getServerName())) {
            World world = Bukkit.getWorld(cachedSpawn.getWorld());
            if (world == null)
                return;

            Location loc = new Location(world, cachedSpawn.getX(), cachedSpawn.getY(), cachedSpawn.getZ(),
                    cachedSpawn.getYaw(), cachedSpawn.getPitch());
            player.getScheduler().run(plugin, task -> {
                player.teleportAsync(loc);
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().spawnTeleported()));
            }, null);
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().crossServerTeleporting()));
            crossServerService.teleportCrossServer(player, cachedSpawn.getServer(), cachedSpawn.getWorld(),
                    cachedSpawn.getX(), cachedSpawn.getY(), cachedSpawn.getZ(), cachedSpawn.getYaw(),
                    cachedSpawn.getPitch(), false);
        }
    }

    public boolean shouldForceSpawnOnJoin() {
        return config.get().forceSpawnOnJoin();
    }

    public boolean hasSpawn() {
        return cachedSpawn != null;
    }

    public Location getSpawnLocation() {
        if (cachedSpawn == null) return null;
        if (!cachedSpawn.getServer().equals(getServerName())) return null;
        World world = Bukkit.getWorld(cachedSpawn.getWorld());
        if (world == null) return null;
        return new Location(world, cachedSpawn.getX(), cachedSpawn.getY(), cachedSpawn.getZ(),
                cachedSpawn.getYaw(), cachedSpawn.getPitch());
    }

    private String getServerName() {
        return RevengeCoreAPI.get().getServerName();
    }
}
