package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossDeathMessage;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@Service
public class DeathService {

    private final ChannelService channelService;
    private final Plugin plugin;

    @Inject
    public DeathService(Plugin plugin) {
        this.plugin = plugin;
        this.channelService = RevengeCoreAPI.get().getChannelService();

        this.channelService.subscribe("cross_death", CrossDeathMessage.class, msg -> {
            if (msg.getServerName().equals(RevengeCoreAPI.get().getServerName()))
                return;
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg.getParsedMessage()));
        }, plugin.getSLF4JLogger());
    }

    public void handleDeath(String playerName, String currentServer, String parsedMessage) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(parsedMessage));
            channelService.publish("cross_death", new CrossDeathMessage(playerName, currentServer, parsedMessage));
        });
    }
}
