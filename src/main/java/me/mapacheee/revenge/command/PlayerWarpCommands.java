package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.PlayerWarp;
import me.mapacheee.revenge.service.PlayerWarpService;
import me.mapacheee.revenge.service.CrossServerService;
import me.mapacheee.revenge.gui.PlayerWarpGui;
import me.mapacheee.revenge.gui.PlayerWarpEditGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
public class PlayerWarpCommands {

    private final PlayerWarpService pwarpService;
    private final CrossServerService crossServerService;
    private final PlayerWarpGui pwarpGui;
    private final PlayerWarpEditGui pwarpEditGui;
    private final Container<Messages> messages;

    @Inject
    public PlayerWarpCommands(PlayerWarpService pwarpService, CrossServerService crossServerService, PlayerWarpGui pwarpGui, PlayerWarpEditGui pwarpEditGui, Container<Messages> messages) {
        this.pwarpService = pwarpService;
        this.crossServerService = crossServerService;
        this.pwarpGui = pwarpGui;
        this.pwarpEditGui = pwarpEditGui;
        this.messages = messages;
    }

    @Suggestions("pwarps")
    public List<String> pwarps(CommandContext<Source> context, CommandInput input) {
        return pwarpService.getPwarps().stream()
                .map(PlayerWarp::getName)
                .collect(Collectors.toList());
    }

    @Command("pwarp set <name>")
    @Command("pwarp create <name>")
    @Permission("revenge.pwarp.set")
    public void setPwarp(Source source, @Argument("name") String name) {
        if (!(source.source() instanceof Player sender)) return;
        String existingName = name.toLowerCase();
        PlayerWarp warp = pwarpService.getPwarp(existingName);
        if (warp != null && !warp.getOwnerUuid().equals(sender.getUniqueId()) && !sender.hasPermission("revenge.pwarp.admin")) {
            String err = messages.get().pwarpAdminOverride() != null ? messages.get().pwarpAdminOverride() : "<red>Ese PlayerWarp ya existe y pertenece a otro jugador.";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + err));
            return;
        }

        if (warp == null) {
            warp = new PlayerWarp(existingName, sender.getUniqueId(), me.mapacheee.revenge.api.RevengeCoreAPI.get().getServerName(),
                sender.getWorld().getName(), sender.getLocation().getX(), sender.getLocation().getY(),
                sender.getLocation().getZ(), sender.getLocation().getYaw(), sender.getLocation().getPitch());
            warp.setDisplayName("<aqua>" + name);
        } else {
            warp.setServer(me.mapacheee.revenge.api.RevengeCoreAPI.get().getServerName());
            warp.setWorld(sender.getWorld().getName());
            warp.setX(sender.getLocation().getX());
            warp.setY(sender.getLocation().getY());
            warp.setZ(sender.getLocation().getZ());
            warp.setYaw(sender.getLocation().getYaw());
            warp.setPitch(sender.getLocation().getPitch());
        }
        pwarpService.savePwarp(warp);
        String msg = messages.get().pwarpSetSuccess() != null ? messages.get().pwarpSetSuccess() : "<green>PlayerWarp '<warp>' ha sido establecido.";
        sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg, Placeholder.parsed("warp", name)));
    }

    @Command("delpwarp <name>")
    @Permission("revenge.pwarp.set")
    public void delPwarp(Source source, @Argument(value = "name", suggestions = "pwarps") String name) {
        if (!(source.source() instanceof Player sender)) return;
        PlayerWarp warp = pwarpService.getPwarp(name);
        if (warp != null) {
            if (!warp.getOwnerUuid().equals(sender.getUniqueId()) && !sender.hasPermission("revenge.pwarp.admin")) {
                String err = messages.get().pwarpCannotDelete() != null ? messages.get().pwarpCannotDelete() : "<red>No puedes borrar un PlayerWarp que no es tuyo.";
                sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + err));
                return;
            }
            pwarpService.deletePwarp(warp.getName());
            String msg = messages.get().pwarpEditDeleteSuccess() != null ? messages.get().pwarpEditDeleteSuccess() : "<red>Player Warp eliminado.";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg));
        } else {
            String msg = messages.get().pwarpNotFound() != null ? messages.get().pwarpNotFound() : "<red>PlayerWarp no encontrado.";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg));
        }
    }

    @Command("pwarp")
    @Permission("revenge.pwarp.use")
    public void pwarpGui(Source source) {
        if (!(source.source() instanceof Player sender)) return;
        if (pwarpService.getPwarps().isEmpty()) {
            String msg = messages.get().pwarpsEmpty() != null ? messages.get().pwarpsEmpty() : "<red>No hay PlayerWarps disponibles.";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg));
            return;
        }
        pwarpGui.open(sender, 0);
    }

    @Command("pwarp edit")
    @Permission("revenge.pwarp.set")
    public void pwarpEdit(Source source) {
        if (!(source.source() instanceof Player sender)) return;
        pwarpEditGui.openList(sender, 0);
    }

    @Command("pwarp <name>")
    @Permission("revenge.pwarp.use")
    public void pwarpTeleport(Source source, @Argument(value = "name", suggestions = "pwarps") String name) {
        if (!(source.source() instanceof Player sender)) return;
        PlayerWarp warp = pwarpService.getPwarp(name);
        if (warp != null) {
            String msg = messages.get().pwarpTeleporting() != null ? messages.get().pwarpTeleporting() : "<yellow>Teletransportando a <warp>...";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg, Placeholder.parsed("warp", warp.getName())));
            crossServerService.teleportCrossServer(
                    sender,
                    warp.getServer(),
                    warp.getWorld(),
                    warp.getX(),
                    warp.getY(),
                    warp.getZ(),
                    warp.getYaw(),
                    warp.getPitch(),
                    false
            );
        } else {
            String err = messages.get().pwarpNotFound() != null ? messages.get().pwarpNotFound() : "<red>PlayerWarp no encontrado.";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + err));
        }
    }
}
