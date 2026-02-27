package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossSpeedAllMessage;
import me.mapacheee.revenge.config.Messages;
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
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.List;
import java.util.stream.Collectors;

@CommandComponent
public class SpeedCommand {

    private final SpeedService speedService;
    private final PlayerDataService playerDataService;
    private final Container<Messages> messages;

    @Inject
    public SpeedCommand(SpeedService speedService, PlayerDataService playerDataService, Container<Messages> messages) {
        this.speedService = speedService;
        this.playerDataService = playerDataService;
        this.messages = messages;
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

        if (target.equalsIgnoreCase("@a")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:speed_all",
                new CrossSpeedAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console",
                    value,
                    speedType.name()
                )
            );
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().speedSetAll()
            ));
            return;
        }

        speedService.setSpeed(source, target, value, speedType);
    }
}
