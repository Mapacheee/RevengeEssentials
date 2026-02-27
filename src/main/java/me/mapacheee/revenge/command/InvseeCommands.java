package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.service.InvseeService;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import java.util.List;
import java.util.stream.Collectors;
import me.mapacheee.revenge.service.PlayerDataService;

@CommandComponent
public class InvseeCommands {

    private final InvseeService invseeService;
    private final PlayerDataService playerDataService;

    @Inject
    public InvseeCommands(InvseeService invseeService, PlayerDataService playerDataService) {
        this.invseeService = invseeService;
        this.playerDataService = playerDataService;
    }

    @Suggestions("players")
    public List<String> players(CommandContext<Source> context, CommandInput input) {
        return playerDataService.getAllPlayerNames()
                .stream()
                .collect(Collectors.toList());
    }

    @Command("invsee <target>")
    @Permission("revenge.invsee")
    public void invsee(Source source, @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Solo los jugadores pueden usar este comando.</red>"));
            return;
        }

        invseeService.openInvsee(player, target);
    }
}
