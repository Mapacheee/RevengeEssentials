package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.TeleportService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.mapacheee.revenge.channel.CrossTeleportAllMessage;
import me.mapacheee.revenge.channel.CrossTeleportHereMessage;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import me.mapacheee.revenge.channel.CrossTeleportRequestMessage;
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

        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:tp_req",
                new CrossTeleportRequestMessage(
                    RevengeCoreAPI.get().getServerName(),
                    player.getName(),
                    target,
                    RevengeCoreAPI.get().getServerName()
                )
            );
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().crossServerTeleporting()));
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

    @Command("tphere <target>")
    @Permission("revenge.tphere")
    public void tphere(Source source, @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Este comando solo puede ejecutarse en el juego.</red>"));
            return;
        }

        if (target.equalsIgnoreCase("all")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:tphere_all",
                new CrossTeleportAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    player.getName(),
                    RevengeCoreAPI.get().getServerName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ(),
                    player.getLocation().getYaw(),
                    player.getLocation().getPitch(),
                    player.getLocation().getWorld().getName()
                )
            );
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tpaHereAll()));
            return;
        }

        Player localTarget = Bukkit.getPlayerExact(target);
        if (localTarget != null) {
            localTarget.teleportAsync(player.getLocation()).thenRun(() -> {
                localTarget.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandTphereReceived(),
                    Placeholder.parsed("sender", player.getName())
                ));
            });
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandTphereTeleporting(),
                Placeholder.parsed("target", localTarget.getName())
            ));
        } else {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:tphere",
                new CrossTeleportHereMessage(
                    RevengeCoreAPI.get().getServerName(),
                    target,
                    RevengeCoreAPI.get().getServerName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ(),
                    player.getLocation().getYaw(),
                    player.getLocation().getPitch(),
                    player.getLocation().getWorld().getName()
                )
            );
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandTphereTeleporting(),
                Placeholder.parsed("target", target)
            ));
        }
    }
}
