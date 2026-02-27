package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossGodModeAllMessage;
import me.mapacheee.revenge.config.Messages;
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
    private final Container<Messages> messages;

    @Inject
    public GodCommands(PlayerStateService playerStateService, PlayerDataService playerDataService, Container<Messages> messages) {
        this.playerStateService = playerStateService;
        this.playerDataService = playerDataService;
        this.messages = messages;
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

        if (target != null && target.equalsIgnoreCase("@a")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:godmode_all",
                new CrossGodModeAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console",
                    null
                )
            );
            
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().godmodeUpdatedAll()
            ));
            return;
        }

        String effectiveTarget = target != null ? target : ((Player) source.source()).getName();
        playerStateService.setGodMode(source, effectiveTarget, null); 
    }
}
