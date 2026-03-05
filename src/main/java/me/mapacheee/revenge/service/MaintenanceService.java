package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossMaintenanceMessage;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

@Service
public class MaintenanceService {

    private final ChannelService channelService;
    private final Plugin plugin;
    private final Container<Messages> messages;
    private boolean maintenanceInProgress = false;

    @Inject
    public MaintenanceService(Plugin plugin, Container<Messages> messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.channelService = RevengeCoreAPI.get().getChannelService();

        this.channelService.subscribe("revenge:maintenance", CrossMaintenanceMessage.class, msg -> {
            startMaintenanceSequence(msg.countdown);
        }, plugin.getSLF4JLogger());
    }

    public void startMaintenanceSequence(int seconds) {
        if (maintenanceInProgress) return;
        maintenanceInProgress = true;

        new BukkitRunnable() {
            int time = seconds;

            @Override
            public void run() {
                if (time <= 0) {
                    executeShutdown();
                    cancel();
                    return;
                }

                if (time == seconds || time % 10 == 0 || time <= 5) {
                    Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                            messages.get().maintenanceCountdown().replace("<seconds>", String.valueOf(time))
                    ));
                }

                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void executeShutdown() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kick(MiniMessage.miniMessage().deserialize(messages.get().maintenanceKick()));
        }
        Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 20L);
    }

    public void triggerMaintenanceNetwork(int seconds) {
        channelService.publish("revenge:maintenance", new CrossMaintenanceMessage(
                RevengeCoreAPI.get().getServerName(),
                seconds
        ));
    }
}
