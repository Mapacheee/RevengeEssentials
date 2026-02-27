package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossFlyMessage;
import me.mapacheee.revenge.channel.CrossFeedbackMessage;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.channel.CrossFlyAllMessage;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.paper.util.sender.Source;

@Service
public class FlyService {

    private final InventorySyncService inventorySyncService;
    private final Container<Messages> messages;
    private final Plugin plugin;
    private final ChannelService channelService;
    private final PlayerDataService playerDataService;

    @Inject
    public FlyService(InventorySyncService inventorySyncService, Container<Messages> messages, Plugin plugin, PlayerDataService playerDataService) {
        this.inventorySyncService = inventorySyncService;
        this.messages = messages;
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.channelService = RevengeCoreAPI.get().getChannelService();
    }

    @OnEnable
    public void onEnable() {
        channelService.subscribe("revenge:fly", CrossFlyMessage.class, msg -> {
            Player targetPlayer = Bukkit.getPlayerExact(msg.getTargetName());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                applyFlyLocal(targetPlayer, msg.getSenderName(), msg.getState());
            }
        }, plugin.getSLF4JLogger());

        channelService.subscribe("revenge:fly_all",CrossFlyAllMessage.class, msg -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                boolean newState = msg.enabled != null ? msg.enabled : !p.getAllowFlight();
                applyFlyLocal(p, msg.senderName, newState);
            }
        }, plugin.getSLF4JLogger());
    }

    public void toggleFly(Source sender, String targetName) {
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        String senderName = sender.source() instanceof Player p ? p.getName() : "Console";

        if (targetPlayer != null && targetPlayer.isOnline()) {
            applyFlyLocal(targetPlayer, senderName, !targetPlayer.getAllowFlight());
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
                
                channelService.publish("revenge:fly", new CrossFlyMessage(senderName, targetName, null, RevengeCoreAPI.get().getServerName()));
            });
        }
    }

    private void applyFlyLocal(Player targetPlayer, String senderName, Boolean forcedState) {
        targetPlayer.getScheduler().run(plugin, task -> {
            boolean newState = forcedState != null ? forcedState : !targetPlayer.getAllowFlight();
            targetPlayer.setAllowFlight(newState);
            if (!newState)
                targetPlayer.setFlying(false);

            if (newState) {
                targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().flyEnabled()));
            } else {
                targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().flyDisabled()));
            }

            Player senderPlayer = Bukkit.getPlayerExact(senderName);
            if (senderPlayer != null && senderPlayer.isOnline() && !senderPlayer.equals(targetPlayer)) {
                if (newState) {
                    senderPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().flyEnabledOther(),
                            Placeholder.unparsed("player", targetPlayer.getName())));
                } else {
                    senderPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().flyDisabledOther(),
                            Placeholder.unparsed("player", targetPlayer.getName())));
                }
            } else if (!senderName.equals("Console") && !senderName.equals(targetPlayer.getName())) {
                String feedbackStr = MiniMessage.miniMessage().serialize(
                    MiniMessage.miniMessage().deserialize(newState ? messages.get().flyEnabledOther() : messages.get().flyDisabledOther(),
                        Placeholder.unparsed("player", targetPlayer.getName()))
                );
                channelService.publish("revenge:feedback", new CrossFeedbackMessage(senderName, feedbackStr));
            }

            inventorySyncService.savePlayerData(targetPlayer);
        }, null);
    }
}
