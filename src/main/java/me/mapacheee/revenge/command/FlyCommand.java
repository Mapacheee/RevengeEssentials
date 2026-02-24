package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.FlyService;
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
import java.util.List;
import java.util.stream.Collectors;

@CommandComponent
public class FlyCommand {

    private final FlyService flyService;
    private final Container<Messages> messages;

    @Inject
    public FlyCommand(FlyService flyService, Container<Messages> messages) {
        this.flyService = flyService;
        this.messages = messages;
    }

    @Suggestions("players")
    public List<String> players(CommandContext<Source> context, CommandInput input) {
        return Bukkit.getOnlinePlayers()
                .stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    @Command("fly")
    @Permission("revenge.fly")
    public void flySelf(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        flyService.toggleFly(player);
    }

    @Command("fly <target>")
    @Permission("revenge.fly.others")
    public void flyOther(Source source, @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player player))
            return;

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().playerNotOnline(),
                    Placeholder.unparsed("player", target)));
            return;
        }

        flyService.toggleFly(player, targetPlayer);
    }
}
