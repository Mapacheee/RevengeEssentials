package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.channel.CrossClearMessage;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

@Service
public class UtilityService {

    private final Container<Messages> messages;
    private final Plugin plugin;
    private final ChannelService channelService;

    @Inject
    public UtilityService(Container<Messages> messages, Plugin plugin) {
        this.messages = messages;
        this.plugin = plugin;
        this.channelService = RevengeCoreAPI.get().getChannelService();
    }

    @OnEnable
    public void onEnable() {
        channelService.subscribe("revenge:clear", CrossClearMessage.class, msg -> {
            Player targetPlayer = Bukkit.getPlayerExact(msg.targetName);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.getScheduler().run(
                    plugin, 
                    task -> {
                        targetPlayer.getInventory().clear();
                        targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandClearOtherReceived()));
                    }, 
                    null
                );
            }
        }, plugin.getSLF4JLogger());
    }
}
