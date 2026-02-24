package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.SpawnService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

@CommandComponent
public class SpawnCommands {

    private final SpawnService spawnService;
    private final Container<Messages> messages;
    private final Plugin plugin;

    @Inject
    public SpawnCommands(SpawnService spawnService, Container<Messages> messages, Plugin plugin) {
        this.spawnService = spawnService;
        this.messages = messages;
        this.plugin = plugin;
    }

    @Command("spawn")
    @Permission("revenge.spawn")
    public void spawn(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        spawnService.teleportToSpawn(player);
    }

    @Command("setspawn")
    @Permission("revenge.admin")
    public void setSpawn(Source source) {
        if (!(source.source() instanceof Player player))
            return;

        spawnService.setSpawn(player).thenRun(() -> player.getScheduler().run(plugin,
                task -> player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().spawnSet())), null));
    }
}
