package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.channel.CrossGamemodeMessage;
import me.mapacheee.revenge.channel.CrossGodModeMessage;
import me.mapacheee.revenge.channel.CrossHealMessage;
import me.mapacheee.revenge.channel.CrossFeedbackMessage;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.incendo.cloud.paper.util.sender.Source;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Service
public class PlayerStateService {

    private final Container<Messages> messages;
    private final Plugin plugin;
    private final ChannelService channelService;
    private final PlayerDataService playerDataService;

    @Inject
    public PlayerStateService(Container<Messages> messages, Plugin plugin, PlayerDataService playerDataService) {
        this.messages = messages;
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.channelService = RevengeCoreAPI.get().getChannelService();
    }

    @OnEnable
    public void onEnable() {
        channelService.subscribe("revenge:feedback", CrossFeedbackMessage.class, msg -> {
            Player sender = Bukkit.getPlayerExact(msg.getOriginalSender());
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(msg.getSerializedMessage()));
            }
        }, plugin.getSLF4JLogger());

        channelService.subscribe("revenge:gamemode", CrossGamemodeMessage.class, msg -> {
            Player targetPlayer = Bukkit.getPlayerExact(msg.getTargetName());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                applyGamemodeLocal(targetPlayer, msg.getSenderName(), msg.getGameMode());
            }
        }, plugin.getSLF4JLogger());

        channelService.subscribe("revenge:godmode", CrossGodModeMessage.class, msg -> {
            Player targetPlayer = Bukkit.getPlayerExact(msg.getTargetName());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                boolean newState = msg.getEnabled() != null ? msg.getEnabled() : !targetPlayer.isInvulnerable();
                applyGodModeLocal(targetPlayer, msg.getSenderName(), newState);
            }
        }, plugin.getSLF4JLogger());

        channelService.subscribe("revenge:heal", CrossHealMessage.class, msg -> {
            Player targetPlayer = Bukkit.getPlayerExact(msg.getTargetName());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                applyHealLocal(targetPlayer, msg.getSenderName());
            }
        }, plugin.getSLF4JLogger());
    }

    public void setGameMode(Source sender, String targetName, GameMode mode) {
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        String senderName = sender.source() instanceof Player p ? p.getName() : "Console";

        if (targetPlayer != null && targetPlayer.isOnline()) {
            applyGamemodeLocal(targetPlayer, senderName, mode.name());
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
                
                playerDataService.getPlayerData(targetUuid, targetName).thenAccept(data -> {
                    data.setQueuedGamemode(mode.name());
                    playerDataService.savePlayerData(data);
                });
                channelService.publish("revenge:gamemode", new CrossGamemodeMessage(senderName, targetName, mode.name()));
            });
        }
    }

    public void setGodMode(Source sender, String targetName, Boolean state) {
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        String senderName = sender.source() instanceof Player p ? p.getName() : "Console";

        if (targetPlayer != null && targetPlayer.isOnline()) {
            applyGodModeLocal(targetPlayer, senderName, state != null ? state : !targetPlayer.isInvulnerable());
        } else {
            playerDataService.getUUIDFromName(targetName).thenAccept(targetUuid -> {
                if (targetUuid == null) {
                    if (sender.source() instanceof Player p) {
                        p.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().playerNotOnline(),
                                Placeholder.unparsed("player", targetName)));
                    }
                    return;
                }
                playerDataService.getPlayerData(targetUuid, targetName).thenAccept(data -> {
                    data.setQueuedGodmode(state);
                    playerDataService.savePlayerData(data);
                });
                channelService.publish("revenge:godmode", new CrossGodModeMessage(senderName, targetName, state));
            });
        }
    }

    public void healPlayer(Source sender, String targetName) {
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        String senderName = sender.source() instanceof Player p ? p.getName() : "Console";

        if (targetPlayer != null && targetPlayer.isOnline()) {
            applyHealLocal(targetPlayer, senderName);
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
                playerDataService.getPlayerData(targetUuid, targetName).thenAccept(data -> {
                    data.setQueuedHeal(true);
                    playerDataService.savePlayerData(data);
                });
                channelService.publish("revenge:heal", new CrossHealMessage(senderName, targetName));
            });
        }
    }

    private void applyGamemodeLocal(Player targetPlayer, String senderName, String gameModeNode) {
        try {
            GameMode g = GameMode.valueOf(gameModeNode);
            targetPlayer.getScheduler().run(plugin, task -> {
                targetPlayer.setGameMode(g);
                targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().gamemodeUpdatedSelf(),
                        Placeholder.unparsed("gamemode", g.name())));
                
                Player senderPlayer = Bukkit.getPlayerExact(senderName);
                if (senderPlayer != null && senderPlayer.isOnline() && !senderPlayer.equals(targetPlayer)) {
                    senderPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().gamemodeUpdatedOther(),
                            Placeholder.unparsed("gamemode", g.name()),
                            Placeholder.unparsed("player", targetPlayer.getName())));
                } else if (!senderName.equals("Console") && !senderName.equals(targetPlayer.getName())) {
                    String feedbackStr = MiniMessage.miniMessage().serialize(
                        MiniMessage.miniMessage().deserialize(messages.get().gamemodeUpdatedOther(),
                            Placeholder.unparsed("gamemode", g.name()),
                            Placeholder.unparsed("player", targetPlayer.getName()))
                    );
                    channelService.publish("revenge:feedback", new CrossFeedbackMessage(senderName, feedbackStr));
                }
            }, null);
        } catch (IllegalArgumentException ignored) {}
    }

    private void applyGodModeLocal(Player targetPlayer, String senderName, boolean state) {
        targetPlayer.getScheduler().run(plugin, task -> {
            targetPlayer.setInvulnerable(state);
            String messageKey = state ? messages.get().godmodeEnabledSelf() : messages.get().godmodeDisabledSelf();
            targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messageKey));

            Player senderPlayer = Bukkit.getPlayerExact(senderName);
            String otherMessageKey = state ? messages.get().godmodeEnabledOther() : messages.get().godmodeDisabledOther();
            if (senderPlayer != null && senderPlayer.isOnline() && !senderPlayer.equals(targetPlayer)) {
                senderPlayer.sendMessage(MiniMessage.miniMessage().deserialize(otherMessageKey,
                        Placeholder.unparsed("player", targetPlayer.getName())));
            } else if (!senderName.equals("Console") && !senderName.equals(targetPlayer.getName())) {
                String feedbackStr = MiniMessage.miniMessage().serialize(
                    MiniMessage.miniMessage().deserialize(otherMessageKey,
                        Placeholder.unparsed("player", targetPlayer.getName()))
                );
                channelService.publish("revenge:feedback", new CrossFeedbackMessage(senderName, feedbackStr));
            }
        }, null);
    }

    private void applyHealLocal(Player targetPlayer, String senderName) {
        targetPlayer.getScheduler().run(plugin, task -> {
            double maxHealth = targetPlayer.getAttribute(Attribute.MAX_HEALTH) != null 
                ? targetPlayer.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
            targetPlayer.setHealth(maxHealth);
            targetPlayer.setFoodLevel(20);
            targetPlayer.setFireTicks(0);
            targetPlayer.getActivePotionEffects().forEach(effect -> targetPlayer.removePotionEffect(effect.getType()));

            targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().healSelf()));

            Player senderPlayer = Bukkit.getPlayerExact(senderName);
            if (senderPlayer != null && senderPlayer.isOnline() && !senderPlayer.equals(targetPlayer)) {
                senderPlayer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().healOther(),
                        Placeholder.unparsed("player", targetPlayer.getName())));
            } else if (!senderName.equals("Console") && !senderName.equals(targetPlayer.getName())) {
                String feedbackStr = MiniMessage.miniMessage().serialize(
                    MiniMessage.miniMessage().deserialize(messages.get().healOther(),
                        Placeholder.unparsed("player", targetPlayer.getName()))
                );
                channelService.publish("revenge:feedback", new CrossFeedbackMessage(senderName, feedbackStr));
            }
        }, null);
    }
}
