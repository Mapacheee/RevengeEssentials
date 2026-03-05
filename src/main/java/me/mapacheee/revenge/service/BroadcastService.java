package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossBroadcastMessage;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@Service
public class BroadcastService {

    private final ChannelService channelService;
    private final Container<Messages> messages;
    private final Plugin plugin;

    @Inject
    public BroadcastService(Container<Messages> messages, Plugin plugin) {
        this.messages = messages;
        this.plugin = plugin;
        this.channelService = RevengeCoreAPI.get().getChannelService();

        this.channelService.subscribe("revenge:broadcast", CrossBroadcastMessage.class, msg -> {
            String message = msg.message;
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(messages.get().broadcast(), Placeholder.parsed("message", message)));
        }, plugin.getSLF4JLogger());
    }

    public void sendLocalBroadcast(String message) {
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(messages.get().broadcast(), Placeholder.parsed("message", message)));
    }

    public void sendGlobalBroadcast(String message) {
        sendLocalBroadcast(message);
        channelService.publish("revenge:broadcast", new CrossBroadcastMessage(RevengeCoreAPI.get().getServerName(), message));
    }
}
