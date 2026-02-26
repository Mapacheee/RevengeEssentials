package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossJoinMessage;
import me.mapacheee.revenge.channel.CrossQuitMessage;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import com.thewinterframework.service.annotation.lifecycle.OnDisable;
import org.redisson.api.RedissonClient;
import com.mongodb.client.model.Filters;
import org.bukkit.World;
import java.time.Duration;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.util.concurrent.TimeUnit;
import me.mapacheee.revenge.data.LogoutLocation;
import me.mapacheee.revenge.data.LogoutLocationRepository;

@Service
public class JoinQuitService {

    private final ChannelService channelService;
    private final Plugin plugin;
    private final LogoutLocationRepository logoutLocationRepository;
    private final CrossServerService crossServerService;

    @Inject
    public JoinQuitService(Plugin plugin, LogoutLocationRepository logoutLocationRepository, CrossServerService crossServerService) {
        this.plugin = plugin;
        this.logoutLocationRepository = logoutLocationRepository;
        this.crossServerService = crossServerService;
        this.channelService = RevengeCoreAPI.get().getChannelService();

        this.channelService.subscribe("cross_join", CrossJoinMessage.class, msg -> {
            if (msg.getServerName().equals(RevengeCoreAPI.get().getServerName()))
                return;
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg.getParsedMessage()));
        }, plugin.getSLF4JLogger());

        this.channelService.subscribe("cross_quit", CrossQuitMessage.class, msg -> {
            if (msg.getServerName().equals(RevengeCoreAPI.get().getServerName()))
                return;
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg.getParsedMessage()));
        }, plugin.getSLF4JLogger());
    }

    public void handleJoin(Player player, String parsedMessage) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String playerName = player.getName();
            RedissonClient redisson = RevengeCoreAPI.get().getRedisService().client();
            RBucket<String> bucket = redisson.getBucket("revenge:online:" + playerName);

            String previousServer = bucket.get();
            String currentServer = RevengeCoreAPI.get().getServerName();

            bucket.set(currentServer, Duration.ofMinutes(10));
            
            RMapCache<String, String> uuidMap = redisson.getMapCache("revenge:uuidmap");
            uuidMap.put(playerName.toLowerCase(), player.getUniqueId().toString(), 7, TimeUnit.DAYS);
            
            if (previousServer == null) {
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize(parsedMessage));
                channelService.publish("cross_join",
                        new CrossJoinMessage(playerName, currentServer, parsedMessage));

                LogoutLocation lastLoc = logoutLocationRepository.findOne(
                        Filters.eq("uuid", player.getUniqueId().toString()));

                if (lastLoc != null) {
                    if (lastLoc.getServer().equals(currentServer)) {
                        World w = Bukkit.getWorld(lastLoc.getWorld());
                        if (w != null) {
                            Location restoreLoc = new Location(
                                    w, lastLoc.getX(), lastLoc.getY(), lastLoc.getZ(),
                                    lastLoc.getYaw(), lastLoc.getPitch());
                            player.getScheduler().run(plugin, t -> player.teleportAsync(restoreLoc), null);
                        }
                    } else {
                        crossServerService.teleportCrossServer(player, lastLoc.getServer(),
                                lastLoc.getWorld(), lastLoc.getX(), lastLoc.getY(), lastLoc.getZ(),
                                lastLoc.getYaw(), lastLoc.getPitch(), false);
                    }
                }
            }
        });
    }

    public void handleQuit(Player player, String parsedMessage) {
        String currentServer = RevengeCoreAPI.get().getServerName();
        String playerName = player.getName();
        String uuidStr = player.getUniqueId().toString();

        Location loc = player.getLocation();
        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            logoutLocationRepository.delete(Filters.eq("uuid", uuidStr));
            LogoutLocation logoutLoc = new LogoutLocation(
                    uuidStr, currentServer, loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            logoutLocationRepository.save(logoutLoc);
        });

        Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
            Player onlinePlayer = Bukkit.getPlayer(UUID.fromString(uuidStr));
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                return;
            }

            RedissonClient redisson = RevengeCoreAPI.get().getRedisService().client();
            RBucket<String> bucket = redisson.getBucket("revenge:online:" + playerName);

            String serverInRedis = bucket.get();

            if (serverInRedis == null || serverInRedis.equals(currentServer)) {
                bucket.delete();
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize(parsedMessage));
                channelService.publish("cross_quit",
                        new CrossQuitMessage(playerName, currentServer, parsedMessage));
            }
        }, 5000, TimeUnit.MILLISECONDS);
    }

    @OnDisable
    public void onDisable() {
        RedissonClient redisson = RevengeCoreAPI.get().getRedisService().client();
        String currentServer = RevengeCoreAPI.get().getServerName();
        for (Player p : Bukkit.getOnlinePlayers()) {
            RBucket<String> bucket = redisson.getBucket("revenge:online:" + p.getName());
            String serverInRedis = bucket.get();
            if (currentServer.equals(serverInRedis)) {
                bucket.delete();
            }

            String uuidStr = p.getUniqueId().toString();
            logoutLocationRepository.delete(Filters.eq("uuid", uuidStr));
            Location loc = p.getLocation();
            LogoutLocation logoutLoc = new LogoutLocation(
                    uuidStr, currentServer, loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            logoutLocationRepository.save(logoutLoc);
        }
    }

    public void saveLogoutLocation(Player player) {
        String uuidStr = player.getUniqueId().toString();
        Location loc = player.getLocation();
        String currentServer = RevengeCoreAPI.get().getServerName();
        
        Bukkit.getAsyncScheduler().runNow(plugin, t -> {
            logoutLocationRepository.delete(Filters.eq("uuid", uuidStr));
            LogoutLocation logoutLoc = new LogoutLocation(
                    uuidStr, currentServer, loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            logoutLocationRepository.save(logoutLoc);
        });
    }
}
