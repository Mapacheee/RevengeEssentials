package me.mapacheee.revenge.listener;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.JoinQuitService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@ListenerComponent
public class JoinQuitListener implements Listener {

    private final Container<Messages> messages;
    private final JoinQuitService joinQuitService;

    @Inject
    public JoinQuitListener(Container<Messages> messages, JoinQuitService joinQuitService) {
        this.messages = messages;
        this.joinQuitService = joinQuitService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        Player player = event.getPlayer();

        String parsed = format(player, messages.get().crossServerJoin());
        joinQuitService.handleJoin(player.getName(), parsed);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        Player player = event.getPlayer();
        String parsed = format(player, messages.get().crossServerQuit());
        joinQuitService.handleQuit(player.getName(), parsed);
    }

    private String format(Player player, String message) {
        String parsed = message.replace("<player>", player.getName())
                .replace("<server>", RevengeCoreAPI.get().getServerName());

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            parsed = PlaceholderAPI.setPlaceholders(player, parsed);
        }

        return parsed;
    }
}
