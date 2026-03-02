package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.TradeService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;

import java.util.List;
import java.util.stream.Collectors;

@CommandComponent
public class TradeCommands {

    private final TradeService tradeService;
    private final Container<Messages> messages;

    @Inject
    public TradeCommands(TradeService tradeService, Container<Messages> messages) {
        this.tradeService = tradeService;
        this.messages = messages;
    }

    @Suggestions("trade_players")
    public List<String> tradePlayers(CommandContext<Source> context, CommandInput input) {
        return tradeService.getPlayerDataService().getAllPlayerNames().stream()
            .collect(Collectors.toList());
    }

    @Command("trade <target>")
    @Permission("revenge.trade")
    public void trade(Source source, @Argument(value = "target", suggestions = "trade_players") String target) {
        if (!(source.source() instanceof Player player)) return;

        tradeService.getPlayerDataService().getUUIDFromName(target).thenAccept(uuid -> {
            if (uuid == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().playerNotFound()));
                return;
            }

            tradeService.requestTrade(player, uuid, target);
        });
    }

    @Command("tradeaccept")
    @Permission("revenge.trade")
    public void tradeAccept(Source source) {
        if (!(source.source() instanceof Player player)) return;
        tradeService.acceptTrade(player);
    }

    @Command("tradedeny")
    @Permission("revenge.trade")
    public void tradeDeny(Source source) {
        if (!(source.source() instanceof Player player)) return;
        tradeService.denyTrade(player);
    }
}
