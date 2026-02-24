package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.service.SpeedService;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

@CommandComponent
public class SpeedCommand {

    private final SpeedService speedService;

    @Inject
    public SpeedCommand(SpeedService speedService) {
        this.speedService = speedService;
    }

    @Command("speed <value> [type]")
    @Permission("revenge.speed")
    public void speed(Source source, @Argument("value") int value, @Default("fly") @Argument("type") String type) {
        if (!(source.source() instanceof Player player))
            return;

        SpeedService.SpeedType speedType = type.equalsIgnoreCase("walk")
                ? SpeedService.SpeedType.WALK
                : SpeedService.SpeedType.FLY;

        speedService.setSpeed(player, value, speedType);
    }
}
