package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.service.FlyService;
import me.mapacheee.revenge.service.PlayerDataService;
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
public class FlyCommand {

    private final FlyService flyService;
    private final PlayerDataService playerDataService;

    @Inject
    public FlyCommand(FlyService flyService, PlayerDataService playerDataService) {
        this.flyService = flyService;
        this.playerDataService = playerDataService;
    }

    @Suggestions("players")
    public List<String> players(CommandContext<Source> context, CommandInput input) {
        String filter = input.lastRemainingToken().toLowerCase();
        return playerDataService.getAllPlayerNames().stream()
                .filter(name -> name.toLowerCase().startsWith(filter))
                .limit(20)
                .collect(Collectors.toList());
    }

    @Command("fly")
    @Permission("revenge.fly")
    public void flySelf(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        flyService.toggleFly(source, player.getName());
    }

    @Command("fly <target>")
    @Permission("revenge.fly.others")
    public void flyOther(Source source, @Argument(value = "target", suggestions = "players") String target) {
        flyService.toggleFly(source, target);
    }
}
