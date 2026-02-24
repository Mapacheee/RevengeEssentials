package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.TeleportService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;
import me.mapacheee.revenge.service.PlayerDataService;
import java.util.List;
import java.util.stream.Collectors;

@CommandComponent
public class TeleportCommands {

    private final TeleportService teleportService;
    private final Container<Messages> messages;
    private final PlayerDataService playerDataService;

    @Inject
    public TeleportCommands(TeleportService teleportService, Container<Messages> messages,
            PlayerDataService playerDataService) {
        this.teleportService = teleportService;
        this.messages = messages;
        this.playerDataService = playerDataService;
    }

    @Suggestions("players")
    public List<String> players(CommandContext<Source> context, CommandInput input) {
        return playerDataService.getAllPlayerNames()
                .stream()
                .collect(Collectors.toList());
    }

    @Command("tp <target>")
    @Permission("revenge.tp")
    public void tp(Source source, @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player player))
            return;

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().tpPlayerNotFound(),
                    Placeholder.unparsed("player", target)));
            return;
        }

        teleportService.teleportDirect(player, targetPlayer);
    }

    @Command("tpa <target>")
    @Permission("revenge.tpa")
    public void tpa(Source source, @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player player))
            return;
        teleportService.sendTpaRequest(player, target, false);
    }

    @Command("tpahere <target>")
    @Permission("revenge.tpa")
    public void tpaHere(Source source, @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player player))
            return;
        teleportService.sendTpaRequest(player, target, true);
    }

    @Command("tpaccept")
    public void tpAccept(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        teleportService.acceptTpa(player);
    }

    @Command("tpdeny")
    public void tpDeny(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        teleportService.denyTpa(player);
    }
}
