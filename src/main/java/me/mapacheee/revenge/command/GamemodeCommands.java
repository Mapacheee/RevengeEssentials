package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossGamemodeAllMessage;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.PlayerStateService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
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
public class GamemodeCommands {

    private final PlayerStateService playerStateService;
    private final PlayerDataService playerDataService;
    private final Container<Messages> messages;

    @Inject
    public GamemodeCommands(PlayerStateService playerStateService, PlayerDataService playerDataService, Container<Messages> messages) {
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

    @Command("gmc [target]")
    @Permission("revenge.gamemode")
    public void gmc(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, GameMode.CREATIVE);
    }

    @Command("gms [target]")
    @Permission("revenge.gamemode")
    public void gms(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, GameMode.SURVIVAL);
    }

    @Command("gmsp [target]")
    @Permission("revenge.gamemode")
    public void gmsp(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, GameMode.SPECTATOR);
    }

    @Command("gma [target]")
    @Permission("revenge.gamemode")
    public void gma(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, GameMode.ADVENTURE);
    }
    
    @Command("gamemode <mode> [target]")
    @Permission("revenge.gamemode")
    public void gm(Source source, @Argument("mode") GameMode mode, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, mode);
    }

    private void processGamemode(Source source, @Nullable String targetName, GameMode mode) {
        if (targetName == null && !(source.source() instanceof Player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        if (targetName != null && targetName.equalsIgnoreCase("@a")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:gamemode_all",
                new CrossGamemodeAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console",
                    mode.name()
                )
            );
            
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().gamemodeUpdatedAll(),
                Placeholder.parsed("mode", mode.name())
            ));
            return;
        }
        
        String effectiveTarget = targetName != null ? targetName : ((Player) source.source()).getName();
        playerStateService.setGameMode(source, effectiveTarget, mode);
    }
}
