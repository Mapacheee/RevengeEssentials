package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.service.InvseeService;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import net.kyori.adventure.text.minimessage.MiniMessage;

@CommandComponent
public class InvseeCommands {

    private final InvseeService invseeService;

    @Inject
    public InvseeCommands(InvseeService invseeService) {
        this.invseeService = invseeService;
    }

    @Command("invsee <target>")
    @Permission("revenge.invsee")
    public void invsee(Source source, @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Solo los jugadores pueden usar este comando.</red>"));
            return;
        }

        invseeService.openInvsee(player, target);
    }
}
