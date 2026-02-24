package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Service
public class FlyService {

    private final InventorySyncService inventorySyncService;
    private final Container<Messages> messages;
    private final Plugin plugin;

    @Inject
    public FlyService(InventorySyncService inventorySyncService, Container<Messages> messages, Plugin plugin) {
        this.inventorySyncService = inventorySyncService;
        this.messages = messages;
        this.plugin = plugin;
    }

    public void toggleFly(Player player) {
        boolean newState = !player.getAllowFlight();
        player.setAllowFlight(newState);
        if (!newState)
            player.setFlying(false);

        if (newState) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().flyEnabled()));
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().flyDisabled()));
        }

        inventorySyncService.savePlayerData(player);
    }

    public void toggleFly(Player player, Player target) {
        boolean newState = !target.getAllowFlight();
        target.getScheduler().run(plugin, task -> {
            target.setAllowFlight(newState);
            if (!newState)
                target.setFlying(false);

            if (newState) {
                target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().flyEnabled()));
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        messages.get().flyEnabledOther(),
                        Placeholder.unparsed("player", target.getName())));
            } else {
                target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().flyDisabled()));
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        messages.get().flyDisabledOther(),
                        Placeholder.unparsed("player", target.getName())));
            }

            inventorySyncService.savePlayerData(target);
        }, null);
    }
}
