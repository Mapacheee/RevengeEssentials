package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.gui.HomeGui;
import me.mapacheee.revenge.service.HomeService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;
import java.util.List;
import java.util.stream.Collectors;

@CommandComponent
public class HomeCommands {

    private final HomeService homeService;
    private final HomeGui homeGui;
    private final Container<Messages> messages;
    private final Plugin plugin;

    @Inject
    public HomeCommands(HomeService homeService, HomeGui homeGui, Container<Messages> messages, Plugin plugin) {
        this.homeService = homeService;
        this.homeGui = homeGui;
        this.messages = messages;
        this.plugin = plugin;
    }

    @Suggestions("homes")
    public List<String> homes(CommandContext<Source> context, CommandInput input) {
        if (!(context.sender().source() instanceof Player player)) {
            return java.util.Collections.emptyList();
        }
        try {
            return homeService.getHomes(player.getUniqueId().toString()).join().stream()
                    .map(me.mapacheee.revenge.data.HomeData::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    @Command("sethome [name]")
    @Permission("revenge.home")
    public void setHome(Source source,
            @Default("default") @Argument(value = "name", suggestions = "homes") String name) {
        if (!(source.source() instanceof Player player))
            return;

        homeService.setHome(player, name).thenAccept(success -> {
            player.getScheduler().run(plugin, task -> {
                if (success) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            messages.get().homeSet(),
                            Placeholder.unparsed("home", name)));
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().homeMaxReached()));
                }
            }, null);
        });
    }

    @Command("home [name]")
    @Permission("revenge.home")
    public void home(Source source, @Default("default") @Argument(value = "name", suggestions = "homes") String name) {
        if (!(source.source() instanceof Player player))
            return;

        homeService.getHome(player.getUniqueId().toString(), name).thenAccept(home -> {
            player.getScheduler().run(plugin, task -> {
                if (home == null) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            messages.get().homeNotFound(),
                            Placeholder.unparsed("home", name)));
                    return;
                }
                homeService.teleportToHome(player, home);
            }, null);
        });
    }

    @Command("delhome <name>")
    @Permission("revenge.home")
    public void delHome(Source source, @Argument(value = "name", suggestions = "homes") String name) {
        if (!(source.source() instanceof Player player))
            return;

        homeService.getHome(player.getUniqueId().toString(), name).thenAccept(home -> {
            if (home == null) {
                player.getScheduler().run(plugin, task -> player.sendMessage(MiniMessage.miniMessage().deserialize(
                        messages.get().homeNotFound(),
                        Placeholder.unparsed("home", name))), null);
                return;
            }

            homeService.deleteHome(player.getUniqueId().toString(), name)
                    .thenRun(() -> player.getScheduler().run(plugin,
                            task -> player.sendMessage(MiniMessage.miniMessage().deserialize(
                                    messages.get().homeDeleted(),
                                    Placeholder.unparsed("home", name))),
                            null));
        });
    }

    @Command("homes")
    @Permission("revenge.home")
    public void homes(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        homeGui.open(player);
    }
}
