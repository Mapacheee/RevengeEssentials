package me.mapacheee.revenge.listener;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.DeathService;
import me.mapacheee.revenge.service.HomeService;
import me.mapacheee.revenge.service.SpawnService;
import me.mapacheee.revenge.service.CrossServerService;
import me.mapacheee.revenge.service.PlayerDataService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.MagmaCube;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.World;
import org.bukkit.Location;

@ListenerComponent
public class DeathListener implements Listener {

    private final Container<Messages> messages;
    private final DeathService deathService;
    private final HomeService homeService;
    private final SpawnService spawnService;
    private final CrossServerService crossServerService;
    private final PlayerDataService playerDataService;
    private final Plugin plugin;

    @Inject
    public DeathListener(Container<Messages> messages, DeathService deathService, HomeService homeService,
            SpawnService spawnService, CrossServerService crossServerService, PlayerDataService playerDataService, Plugin plugin) {
        this.messages = messages;
        this.deathService = deathService;
        this.homeService = homeService;
        this.spawnService = spawnService;
        this.crossServerService = crossServerService;
        this.playerDataService = playerDataService;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.deathMessage(null);
        Player player = event.getEntity();
        String messageType = messages.get().crossServerDeathOther();

        playerDataService.incrementPlayerDeaths(player.getUniqueId(), player.getName());

        Player killer = player.getKiller();
        if (killer != null) {
            playerDataService.incrementPlayerKills(killer.getUniqueId(), killer.getName());
        }

        EntityDamageEvent damageEvent = player.getLastDamageCause();
        if (damageEvent != null) {
            if (player.getKiller() != null) {
                messageType = messages.get().crossServerDeathPlayer();
            } else if (damageEvent instanceof EntityDamageByEntityEvent byEntity) {
                if (byEntity.getDamager() instanceof Mob
                        || byEntity.getDamager() instanceof Slime
                        || byEntity.getDamager() instanceof Ghast
                        || byEntity.getDamager() instanceof MagmaCube) {
                    messageType = messages.get().crossServerDeathMob();
                }
            }
        }

        String parsed = messageType.replace("<player>", player.getName());
        parsed = parsed.replace("<server>", RevengeCoreAPI.get().getServerDisplayName());

        if (player.getKiller() != null) {
            parsed = parsed.replace("<killer>", player.getKiller().getName());
        }

        if (damageEvent instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager().customName() != null) {
                parsed = parsed.replace("<mob>", MiniMessage.miniMessage()
                        .serialize(byEntity.getDamager().customName()));
            } else {
                parsed = parsed.replace("<mob>",
                        byEntity.getDamager().getType().name().toLowerCase().replace("_", " "));
            }
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            parsed = PlaceholderAPI.setPlaceholders(player, parsed);
        }

        deathService.handleDeath(player.getName(), RevengeCoreAPI.get().getServerName(), parsed);

        player.getScheduler().runDelayed(plugin, task -> {
            if (player.isOnline()) {
                if (player.isDead()) {
                    player.spigot().respawn();
                }

                homeService.getDefaultHome(player.getUniqueId().toString()).thenAccept(defaultHome -> {
                    if (defaultHome != null) {
                        if (defaultHome.getServer().equals(RevengeCoreAPI.get().getServerName())) {
                            World world = Bukkit.getWorld(defaultHome.getWorld());
                            if (world != null) {
                                player.getScheduler().run(plugin, t -> {
                                    player.teleportAsync(new Location(world, defaultHome.getX(), defaultHome.getY(),
                                            defaultHome.getZ(), defaultHome.getYaw(), defaultHome.getPitch()));
                                }, null);
                            }
                        } else {
                            player.getScheduler().run(plugin, t -> {
                                crossServerService.teleportCrossServer(player, defaultHome.getServer(), defaultHome.getWorld(),
                                        defaultHome.getX(), defaultHome.getY(), defaultHome.getZ(), defaultHome.getYaw(),
                                        defaultHome.getPitch(), false);
                            }, null);
                        }
                    } else if (spawnService.hasSpawn()) {
                        player.getScheduler().run(plugin, t -> spawnService.teleportToSpawn(player), null);
                    }
                });
            }
        }, null, 5L);
    }
}
