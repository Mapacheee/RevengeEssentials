package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.PlayerStateService;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.context.CommandContext;
import me.mapacheee.revenge.channel.CrossHealAllMessage;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.stream.Collectors;
import me.mapacheee.revenge.service.PlayerDataService;

@CommandComponent
public class HealCommands {

    private final PlayerStateService playerStateService;
    private final PlayerDataService playerDataService;
    private final Container<Messages> messages;

    @Inject
    public HealCommands(PlayerStateService playerStateService, PlayerDataService playerDataService, Container<Messages> messages) {
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

    @Command("heal [target]")
    @Permission("revenge.heal")
    public void heal(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        if (target == null && !(source.source() instanceof Player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        if (target != null && target.equalsIgnoreCase("@a")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:heal_all",
                new CrossHealAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console"
                )
            );
            
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().healAll()
            ));
            return;
        }

        String effectiveTarget = target != null ? target : ((Player) source.source()).getName();
        playerStateService.healPlayer(source, effectiveTarget);
    }
}
