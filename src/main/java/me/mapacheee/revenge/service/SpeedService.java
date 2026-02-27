package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossSpeedMessage;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import me.mapacheee.revenge.channel.CrossSpeedAllMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import me.mapacheee.revenge.channel.CrossFeedbackMessage;
import org.incendo.cloud.paper.util.sender.Source;

@Service
public class SpeedService {

    private final InventorySyncService inventorySyncService;
    private final Container<Config> config;
    private final Container<Messages> messages;
    private final ChannelService channelService;
    private final PlayerDataService playerDataService;
    private final Plugin plugin;

    @Inject
    public SpeedService(InventorySyncService inventorySyncService, Container<Config> config,
            Container<Messages> messages, PlayerDataService playerDataService, Plugin plugin) {
        this.inventorySyncService = inventorySyncService;
        this.config = config;
        this.messages = messages;
        this.playerDataService = playerDataService;
        this.plugin = plugin;
        this.channelService = RevengeCoreAPI.get().getChannelService();
    }

    @OnEnable
    public void onEnable() {
        channelService.subscribe("revenge:speed", CrossSpeedMessage.class, msg -> {
            Player targetPlayer = Bukkit.getPlayerExact(msg.getTargetName());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                applySpeedLocal(targetPlayer, msg.getSenderName(), msg.getSpeed(), msg.getType());
            }
        }, plugin.getSLF4JLogger());

        channelService.subscribe("revenge:speed_all", CrossSpeedAllMessage.class, msg -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                applySpeedLocal(p, msg.senderName, msg.value, msg.type);
            }
        }, plugin.getSLF4JLogger());
    }

    public void setSpeed(Source sender, String targetName, int value, SpeedType type) {
        if (value < 1 || value > 10) {
            sender.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().speedInvalidValue()));
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        String senderName = sender.source() instanceof Player p ? p.getName() : "Console";

        if (targetPlayer != null && targetPlayer.isOnline()) {
            applySpeedLocal(targetPlayer, senderName, value, type.name());
        } else {
            playerDataService.getUUIDFromName(targetName).thenAccept(targetUuid -> {
                if (targetUuid == null) {
                    if (sender.source() instanceof Player p) {
                        p.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().playerNotOnline(),
                                Placeholder.unparsed("player", targetName)));
                    } else {
                        sender.source().sendMessage("Player not found.");
                    }
                    return;
                }
                
                playerDataService.getPlayerData(targetUuid, targetName).thenAccept(data -> {});
                
                channelService.publish("revenge:speed", new CrossSpeedMessage(senderName, targetName, value, type.name(), RevengeCoreAPI.get().getServerName()));
            });
        }
    }

    private void applySpeedLocal(Player targetPlayer, String senderName, int value, String typeStr) {
        targetPlayer.getScheduler().run(plugin, task -> {
            float speed = value / 10.0f;
            boolean isWalk = typeStr.equalsIgnoreCase("WALK") || typeStr.equalsIgnoreCase("BOTH");
            boolean isFly = typeStr.equalsIgnoreCase("FLY") || typeStr.equalsIgnoreCase("BOTH");

            if (isWalk) targetPlayer.setWalkSpeed(speed);
            if (isFly) targetPlayer.setFlySpeed(speed);

            String selfMessageKey;
            String otherMessageKey;

            if (typeStr.equalsIgnoreCase("FLY")) {
                selfMessageKey = messages.get().speedSetFly();
                otherMessageKey = messages.get().speedSetFlyOther();
            } else if (typeStr.equalsIgnoreCase("WALK")) {
                selfMessageKey = messages.get().speedSetWalk();
                otherMessageKey = messages.get().speedSetWalkOther();
            } else {
                selfMessageKey = messages.get().speedSet();
                otherMessageKey = messages.get().speedSetOther();
            }

            targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(
                    selfMessageKey,
                    Placeholder.unparsed("value", String.valueOf(value))));

            Player senderPlayer = Bukkit.getPlayerExact(senderName);
            if (senderPlayer != null && senderPlayer.isOnline() && !senderPlayer.equals(targetPlayer)) {
                senderPlayer.sendMessage(MiniMessage.miniMessage().deserialize(otherMessageKey,
                        Placeholder.unparsed("player", targetPlayer.getName()),
                        Placeholder.unparsed("value", String.valueOf(value))));
            } else if (!senderName.equalsIgnoreCase("Console") && !senderName.equalsIgnoreCase(targetPlayer.getName())) {
                String feedbackStr = MiniMessage.miniMessage().serialize(
                    MiniMessage.miniMessage().deserialize(otherMessageKey,
                        Placeholder.unparsed("player", targetPlayer.getName()),
                        Placeholder.unparsed("value", String.valueOf(value)))
                );
                channelService.publish("revenge:feedback", new CrossFeedbackMessage(senderName, feedbackStr));
            }

            inventorySyncService.savePlayerData(targetPlayer);
        }, null);
    }

    public void resetSpeed(Player player) {
        player.setWalkSpeed(config.get().defaultWalkSpeed());
        player.setFlySpeed(config.get().defaultFlySpeed());
        inventorySyncService.savePlayerData(player);
    }

    public enum SpeedType {
        WALK, FLY, BOTH
    }
}
