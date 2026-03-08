package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.Warp;
import me.mapacheee.revenge.service.WarpService;
import me.mapacheee.revenge.service.CrossServerService;
import me.mapacheee.revenge.gui.WarpGui;
import me.mapacheee.revenge.gui.WarpEditGui;
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
public class WarpCommands {

    private final WarpService warpService;
    private final CrossServerService crossServerService;
    private final WarpGui warpGui;
    private final WarpEditGui warpEditGui;
    private final Container<Messages> messages;

    @Inject
    public WarpCommands(WarpService warpService, CrossServerService crossServerService, WarpGui warpGui, WarpEditGui warpEditGui, Container<Messages> messages) {
        this.warpService = warpService;
        this.crossServerService = crossServerService;
        this.warpGui = warpGui;
        this.warpEditGui = warpEditGui;
        this.messages = messages;
    }

    @Suggestions("warps")
    public List<String> warps(CommandContext<Source> context, CommandInput input) {
        return warpService.getWarps().stream()
                .map(Warp::getName)
                .collect(Collectors.toList());
    }

    @Command("setwarp <name>")
    @Permission("revenge.warp.setwarp")
    public void setWarp(Source source, @Argument("name") String name) {
        if (!(source.source() instanceof Player sender)) return;
        String existingName = name.toLowerCase();
        Warp warp = warpService.getWarp(existingName);
        if (warp == null) {
            warp = new Warp(existingName, me.mapacheee.revenge.api.RevengeCoreAPI.get().getServerName(),
                sender.getWorld().getName(), sender.getLocation().getX(), sender.getLocation().getY(),
                sender.getLocation().getZ(), sender.getLocation().getYaw(), sender.getLocation().getPitch());
            warp.setDisplayName("<dark_purple>" + name);
        } else {
            warp.setServer(me.mapacheee.revenge.api.RevengeCoreAPI.get().getServerName());
            warp.setWorld(sender.getWorld().getName());
            warp.setX(sender.getLocation().getX());
            warp.setY(sender.getLocation().getY());
            warp.setZ(sender.getLocation().getZ());
            warp.setYaw(sender.getLocation().getYaw());
            warp.setPitch(sender.getLocation().getPitch());
        }
        warpService.saveWarp(warp);
        String msg = messages.get().warpSetSuccess() != null ? messages.get().warpSetSuccess() : "<green>Warp '<warp>' ha sido establecido.";
        sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg, Placeholder.parsed("warp", name)));
    }

    @Command("delwarp <name>")
    @Permission("revenge.warp.delwarp")
    public void delWarp(Source source, @Argument(value = "name", suggestions = "warps") String name) {
        if (!(source.source() instanceof Player sender)) return;
        Warp warp = warpService.getWarp(name);
        if (warp != null) {
            warpService.deleteWarp(warp.getName());
            String msg = messages.get().warpEditDeleteSuccess() != null ? messages.get().warpEditDeleteSuccess() : "<red>Warp eliminado.";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg));
        } else {
            String msg = messages.get().warpNotFound() != null ? messages.get().warpNotFound() : "<red>Warp no encontrado.";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg));
        }
    }

    @Command("warp|warps")
    @Permission("revenge.warp.use")
    public void warpGui(Source source) {
        if (!(source.source() instanceof Player sender)) return;
        if (warpService.getWarps().isEmpty()) {
            String msg = messages.get().warpsEmpty() != null ? messages.get().warpsEmpty() : "<red>No hay warps disponibles.";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + msg));
            return;
        }
        warpGui.open(sender, 0);
    }

    @Command("warp edit")
    @Permission("revenge.warp.edit")
    public void warpEdit(Source source) {
        if (!(source.source() instanceof Player sender)) return;
        warpEditGui.openList(sender, 0);
    }

    @Command("warp <name>")
    @Permission("revenge.warp.use")
    public void warpTeleport(Source source, @Argument(value = "name", suggestions = "warps") String name) {
        if (!(source.source() instanceof Player sender)) return;
        Warp warp = warpService.getWarp(name);
        if (warp != null) {
            String msg = messages.get().warpTeleporting() != null ? messages.get().warpTeleporting() : "<yellow>Teletransportando a <warp>...";
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
            String err = messages.get().warpNotFound() != null ? messages.get().warpNotFound() : "<red>Warp no encontrado.";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + err));
        }
    }
}
