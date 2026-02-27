package me.mapacheee.revenge.listener;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ListenerComponent
public class TeleportWarmupListener implements Listener {

    private final Container<Config> config;
    private final Container<Messages> messages;
    private final Plugin plugin;

    private final Map<UUID, ScheduledTask> activeWarmups = new ConcurrentHashMap<>();
    private final Map<UUID, Location> initialLocations = new ConcurrentHashMap<>();

    @Inject
    public TeleportWarmupListener(Container<Config> config, Container<Messages> messages, Plugin plugin) {
        this.config = config;
        this.messages = messages;
        this.plugin = plugin;
    }

    public void startWarmup(Player player, Runnable teleportAction) {
        player.getScheduler().run(plugin, t1 -> {
            int seconds = config.get().teleportWarmupSeconds();

            if (seconds <= 0 || player.hasPermission("revenge.teleport.bypass")) {
                teleportAction.run();
                return;
            }

            cancelWarmupAndNotify(player, null); 

            initialLocations.put(player.getUniqueId(), player.getLocation());

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().teleportWarmup(),
                Placeholder.unparsed("seconds", String.valueOf(seconds))
            ));

            ScheduledTask task = player.getScheduler().runDelayed(plugin, t2 -> {
                activeWarmups.remove(player.getUniqueId());
                initialLocations.remove(player.getUniqueId());
                teleportAction.run();
            }, null, seconds * 20L); 

            activeWarmups.put(player.getUniqueId(), task);
        }, null);
    }

    private void cancelWarmupAndNotify(Player player, String messageKey) {
        UUID uuid = player.getUniqueId();
        ScheduledTask task = activeWarmups.remove(uuid);
        initialLocations.remove(uuid);

        if (task != null) {
            task.cancel();
            if (messageKey != null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messageKey));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!activeWarmups.containsKey(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
            cancelWarmupAndNotify(player, messages.get().teleportCancelledMovement());
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (activeWarmups.containsKey(player.getUniqueId())) {
                cancelWarmupAndNotify(player, messages.get().teleportCancelledDamage());
            }
        }
    }
}
