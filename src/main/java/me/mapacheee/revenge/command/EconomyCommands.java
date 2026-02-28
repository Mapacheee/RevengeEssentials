package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.EconomyService;
import me.mapacheee.revenge.service.PlayerDataService;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.paper.util.sender.Source;
import org.checkerframework.checker.nullness.qual.Nullable;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;

import java.util.List;
import java.util.stream.Collectors;
@CommandComponent
public class EconomyCommands {

    private final Container<Messages> messages;
    private final EconomyService economyService;
    private final PlayerDataService playerDataService;

    @Inject
    public EconomyCommands(Container<Messages> messages, EconomyService economyService, PlayerDataService playerDataService) {
        this.messages = messages;
        this.economyService = economyService;
        this.playerDataService = playerDataService;
    }

    @Suggestions("players")
    public List<String> players(CommandContext<Source> context, CommandInput input) {
        return playerDataService.getAllPlayerNames()
                .stream()
                .collect(Collectors.toList());
    }

    @Command("balance|bal|money [player]")
    @Permission("revenge.balance")
    public void balance(Source source, @Nullable @Argument(value = "player", suggestions = "players") String playerName) {
        if (playerName == null) {
            if (!(source.source() instanceof Player p)) {
                source.source().sendMessage("Provide a player name");
                return;
            }
            economyService.getBalance(p.getUniqueId(), p.getName()).thenAccept(amount -> {
                String parsed = messages.get().balanceSelf();
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed, Placeholder.unparsed("amount", formatAmount(amount))));
            });
            return;
        }

        playerDataService.getUUIDFromName(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                String parsed = messages.get().playerNotFound().replace("<player>", playerName);
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed));
                return;
            }
            economyService.getBalance(uuid, playerName).thenAccept(amount -> {
                String parsed = messages.get().balanceOther().replace("<player>", playerName);
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed, Placeholder.unparsed("amount", formatAmount(amount))));
            });
        });
    }

    @Command("pay <player> <amount>")
    @Permission("revenge.pay")
    public void pay(Source source, @Argument(value = "player", suggestions = "players") String playerName, @Argument("amount") double amount) {
        if (!(source.source() instanceof Player sender)) {
            source.source().sendMessage("Solo para jugadores");
            return;
        }
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().invalidAmount()));
            return;
        }
        if (sender.getName().equalsIgnoreCase(playerName)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().invalidAmount()));
            return;
        }

        economyService.hasBalance(sender.getUniqueId(), sender.getName(), amount);
        economyService.getBalance(sender.getUniqueId(), sender.getName()).thenAccept(bal -> {
            if (bal < amount) {
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().notEnoughMoney()));
                return;
            }

            playerDataService.getUUIDFromName(playerName).thenAccept(uuid -> {
                if (uuid == null) {
                    String parsed = messages.get().playerNotFound().replace("<player>", playerName);
                    source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed));
                    return;
                }

                economyService.removeBalance(sender.getUniqueId(), sender.getName(), amount).thenAccept(success -> {
                    if (success) {
                        economyService.addBalance(uuid, playerName, amount).thenAccept(s2 -> {
                            String sentParsed = messages.get().paySent().replace("<player>", playerName);
                            source.source().sendMessage(MiniMessage.miniMessage().deserialize(sentParsed, Placeholder.unparsed("amount", formatAmount(amount))));

                            Player targetOnline = Bukkit.getPlayer(uuid);
                            if (targetOnline != null) {
                                String receivedParsed = messages.get().payReceived().replace("<player>", sender.getName());
                                targetOnline.sendMessage(MiniMessage.miniMessage().deserialize(receivedParsed, Placeholder.unparsed("amount", formatAmount(amount))));
                            }
                        });
                    }
                });
            });
        });
    }

    @Command("eco give <player> <amount>")
    @Permission("revenge.eco.give")
    public void ecoGive(Source source, @Argument(value = "player", suggestions = "players") String playerName, @Argument("amount") double amount) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().invalidAmount()));
            return;
        }
        playerDataService.getUUIDFromName(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                String parsed = messages.get().playerNotFound().replace("<player>", playerName);
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed));
                return;
            }
            economyService.addBalance(uuid, playerName, amount).thenAccept(s -> {
                String parsed = messages.get().balanceGive().replace("<player>", playerName);
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed, Placeholder.unparsed("amount", formatAmount(amount))));
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    @Command("eco take <player> <amount>")
    @Permission("revenge.eco.take")
    public void ecoTake(Source source, @Argument(value = "player", suggestions = "players") String playerName, @Argument("amount") double amount) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().invalidAmount()));
            return;
        }
        playerDataService.getUUIDFromName(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                String parsed = messages.get().playerNotFound().replace("<player>", playerName);
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed));
                return;
            }
            economyService.removeBalance(uuid, playerName, amount).thenAccept(s -> {
                String parsed = messages.get().balanceTake().replace("<player>", playerName);
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed, Placeholder.unparsed("amount", formatAmount(amount))));
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    @Command("eco set <player> <amount>")
    @Permission("revenge.eco.set")
    public void ecoSet(Source source, @Argument(value = "player", suggestions = "players") String playerName, @Argument("amount") double amount) {
        if (amount < 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().invalidAmount()));
            return;
        }
        playerDataService.getUUIDFromName(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                String parsed = messages.get().playerNotFound().replace("<player>", playerName);
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed));
                return;
            }
            economyService.setBalance(uuid, playerName, amount).thenAccept(s -> {
                String parsed = messages.get().balanceSet().replace("<player>", playerName);
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(parsed, Placeholder.unparsed("amount", formatAmount(amount))));
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    private String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
}
