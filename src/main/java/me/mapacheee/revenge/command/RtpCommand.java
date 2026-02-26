package me.mapacheee.revenge.command;

import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.service.RtpService;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import com.google.inject.Inject;

@CommandComponent
public class RtpCommand {

    private final RtpService rtpService;

    @Inject
    public RtpCommand(RtpService rtpService) {
        this.rtpService = rtpService;
    }

    @Command("rtp")
    @Command("randomtp")
    @Permission("revenge.rtp")
    public void rtpCommand(Source source) {
        if (!(source.source() instanceof Player player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Solo los jugadores pueden usar este comando.</red>"));
            return;
        }

        rtpService.startRtp(player);
    }
}
