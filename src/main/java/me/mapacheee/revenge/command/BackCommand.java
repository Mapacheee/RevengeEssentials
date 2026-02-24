package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.service.BackService;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

@CommandComponent
public class BackCommand {

    private final BackService backService;

    @Inject
    public BackCommand(BackService backService) {
        this.backService = backService;
    }

    @Command("back")
    @Permission("revenge.back")
    public void back(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        backService.teleportBack(player);
    }
}
