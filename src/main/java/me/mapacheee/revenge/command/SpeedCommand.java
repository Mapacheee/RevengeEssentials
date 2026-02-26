package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.service.PlayerDataService;
import me.mapacheee.revenge.service.SpeedService;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@CommandComponent
public class SpeedCommand {

    private final SpeedService speedService;
    private final PlayerDataService playerDataService;

    @Inject
    public SpeedCommand(SpeedService speedService, PlayerDataService playerDataService) {
        this.speedService = speedService;
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

    @Command("speed <value> [type]")
    @Permission("revenge.speed")
    public void speed(
            Source source, 
            @Argument("value") int value, 
            @Nullable @Argument("type") String type
    ) {
        if (!(source.source() instanceof Player player)) {
            source.source().sendMessage("Console must specify a player using /speedother.");
            return;
        }

        SpeedService.SpeedType speedType = SpeedService.SpeedType.BOTH;
        if (type != null) {
            speedType = type.equalsIgnoreCase("walk") ? SpeedService.SpeedType.WALK : SpeedService.SpeedType.FLY;
        }

        speedService.setSpeed(source, player.getName(), value, speedType);
    }

    @Command("speedother <target> <value> [type]")
    @Permission("revenge.speed.others")
    public void speedOther(
            Source source,
            @Argument(value = "target", suggestions = "players") String target,
            @Argument("value") int value,
            @Nullable @Argument("type") String type
    ) {
        SpeedService.SpeedType speedType = SpeedService.SpeedType.BOTH;
        if (type != null) {
            speedType = type.equalsIgnoreCase("walk") ? SpeedService.SpeedType.WALK : SpeedService.SpeedType.FLY;
        }

        speedService.setSpeed(source, target, value, speedType);
    }
}
