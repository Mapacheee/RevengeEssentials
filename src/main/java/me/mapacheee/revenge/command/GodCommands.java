package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.service.PlayerStateService;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import java.util.List;
import java.util.stream.Collectors;
import me.mapacheee.revenge.service.PlayerDataService;

@CommandComponent
public class GodCommands {

    private final PlayerStateService playerStateService;
    private final PlayerDataService playerDataService;

    @Inject
    public GodCommands(PlayerStateService playerStateService, PlayerDataService playerDataService) {
        this.playerStateService = playerStateService;
        this.playerDataService = playerDataService;
    }

    @Suggestions("players")
    public List<String> players(CommandContext<Source> context, CommandInput input) {
        return playerDataService.getAllPlayerNames()
                .stream()
                .collect(Collectors.toList());
    }

    @Command("god [target]")
    @Permission("revenge.god")
    public void god(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        if (target == null && !(source.source() instanceof Player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        String effectiveTarget = target != null ? target : ((Player) source.source()).getName();
        playerStateService.setGodMode(source, effectiveTarget, null); 
    }
}
