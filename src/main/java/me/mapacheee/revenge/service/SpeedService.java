package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Service
public class SpeedService {

    private final InventorySyncService inventorySyncService;
    private final Container<Config> config;
    private final Container<Messages> messages;
    private final Plugin plugin;

    @Inject
    public SpeedService(InventorySyncService inventorySyncService, Container<Config> config,
            Container<Messages> messages, Plugin plugin) {
        this.inventorySyncService = inventorySyncService;
        this.config = config;
        this.messages = messages;
        this.plugin = plugin;
    }

    public void setSpeed(Player player, int value, SpeedType type) {
        if (value < 1 || value > 10) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().speedInvalidValue()));
            return;
        }

        float speed = value / 10.0f;

        if (type == SpeedType.FLY) {
            player.setFlySpeed(speed);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().speedSetFly(),
                    Placeholder.unparsed("value", String.valueOf(value))));
        } else {
            player.setWalkSpeed(speed);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().speedSetWalk(),
                    Placeholder.unparsed("value", String.valueOf(value))));
        }

        inventorySyncService.savePlayerData(player);
    }

    public void resetSpeed(Player player) {
        player.setWalkSpeed(config.get().defaultWalkSpeed());
        player.setFlySpeed(config.get().defaultFlySpeed());
        inventorySyncService.savePlayerData(player);
    }

    public enum SpeedType {
        WALK, FLY
    }
}
