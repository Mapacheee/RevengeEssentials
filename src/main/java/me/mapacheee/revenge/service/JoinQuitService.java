package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossJoinMessage;
import me.mapacheee.revenge.channel.CrossQuitMessage;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class JoinQuitService {

    private final ChannelService channelService;
    private final Plugin plugin;

    @Inject
    public JoinQuitService(Plugin plugin) {
        this.plugin = plugin;
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

    public void handleJoin(String playerName, String parsedMessage) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            RedissonClient redisson = RevengeCoreAPI.get().getRedisService().client();
            RBucket<String> bucket = redisson.getBucket("revenge:online:" + playerName);

            String previousServer = bucket.get();
            String currentServer = RevengeCoreAPI.get().getServerName();

            bucket.set(currentServer, Duration.ofMinutes(10));

            if (previousServer == null) {
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize(parsedMessage));
                channelService.publish("cross_join",
                        new CrossJoinMessage(playerName, currentServer, parsedMessage));
            }
        });
    }

    public void handleQuit(String playerName, String parsedMessage) {
        String currentServer = RevengeCoreAPI.get().getServerName();

        Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
            RedissonClient redisson = RevengeCoreAPI.get().getRedisService().client();
            RBucket<String> bucket = redisson.getBucket("revenge:online:" + playerName);

            String serverInRedis = bucket.get();

            if (serverInRedis == null || serverInRedis.equals(currentServer)) {
                bucket.delete();
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize(parsedMessage));
                channelService.publish("cross_quit",
                        new CrossQuitMessage(playerName, currentServer, parsedMessage));
            }
        }, 1500, TimeUnit.MILLISECONDS);
    }
}
