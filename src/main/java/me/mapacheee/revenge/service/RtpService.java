package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossRtpRequestMessage;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RtpService {

    private final Container<Config> config;
    private final Container<Messages> messages;
    private final Plugin plugin;
    private final CrossServerService crossServerService;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> currentlyTeleporting = new ConcurrentHashMap<>();

    private final String RTP_CHANNEL = "revenge:rtp:request";

    @Inject
    public RtpService(Container<Config> config, Container<Messages> messages, Plugin plugin,
            CrossServerService crossServerService) {
        this.config = config;
        this.messages = messages;
        this.plugin = plugin;
        this.crossServerService = crossServerService;
    }

    @OnEnable
    public void onEnable() {
        RevengeCoreAPI.get().getChannelService().subscribe(RTP_CHANNEL, CrossRtpRequestMessage.class, msg -> {
            if (msg.targetServer.equals(RevengeCoreAPI.get().getServerName())) {
                findSafeLocationAsync(msg.targetWorld, msg.radius, loc -> {
                    if (loc != null) {
                        crossServerService.setPendingTeleport(UUID.fromString(msg.uuid), msg.targetServer,
                                msg.targetWorld, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                                false);
                    }
                });
            }
        }, plugin.getSLF4JLogger());
    }

    public void startRtp(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (currentlyTeleporting.getOrDefault(uuid, false)) {
            return;
        }

        if (isOnCooldown(uuid)) {
            long remaining = (cooldowns.get(uuid) + (config.get().rtpCooldownSeconds() * 1000L) - System.currentTimeMillis()) / 1000L;
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().rtpCooldown(),
                    Placeholder.unparsed("time", String.valueOf(remaining))));
            return;
        }

        currentlyTeleporting.put(uuid, true);
        startVisualSequence(player, () -> {
            String targetServer = config.get().rtpTargetServer();
            String targetWorld = config.get().rtpTargetWorld();
            int radius = config.get().rtpRadius();

            if (targetServer.equals(RevengeCoreAPI.get().getServerName())) {
                findSafeLocationAsync(targetWorld, radius, loc -> {
                    if (loc != null) {
                        finishTeleport(player, loc, true);
                    } else {
                        currentlyTeleporting.remove(uuid);
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<#FF4A4A>No se pudo encontrar un lugar seguro, intenta de nuevo."));
                    }
                });
            } else {
                RevengeCoreAPI.get().getChannelService().publish(RTP_CHANNEL,
                        new CrossRtpRequestMessage(uuid.toString(), player.getName(), RevengeCoreAPI.get().getServerName(),
                                targetServer, targetWorld, radius));
                
                Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
                    crossServerService.teleportCrossServer(player, targetServer, targetWorld, 0, 0, 0, 0, 0, false);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().rtpTeleported()));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    setCooldown(uuid);
                    currentlyTeleporting.remove(uuid);
                }, 1000, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        });
    }

    private void findSafeLocationAsync(String worldName, int radius, java.util.function.Consumer<Location> callback) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            callback.accept(null);
            return;
        }

        findSafeLocationRecursive(world, radius, 0, 10, callback);
    }

    private void findSafeLocationRecursive(World world, int radius, int currentAttempt, int maxAttempts, java.util.function.Consumer<Location> callback) {
        if (currentAttempt >= maxAttempts) {
            callback.accept(null);
            return;
        }

        int x = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
        int z = ThreadLocalRandom.current().nextInt(-radius, radius + 1);

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Bukkit.getRegionScheduler().run(plugin, new Location(world, x, 0, z), task -> {
                int highestY = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.MOTION_BLOCKING_NO_LEAVES);
                Block highestBlock = world.getBlockAt(x, highestY, z);

                if (isSafe(highestBlock)) {
                    callback.accept(new Location(world, x + 0.5, highestY + 1, z + 0.5));
                } else {
                    findSafeLocationRecursive(world, radius, currentAttempt + 1, maxAttempts, callback);
                }
            });
        });
    }

    private boolean isSafe(Block block) {
        if (block == null) return false;
        return block.getType().isSolid() && 
               !block.getType().name().contains("LAVA") && 
               !block.getType().name().contains("WATER") &&
               !block.getType().name().contains("CACTUS") &&
               !block.getType().name().contains("MAGMA");
    }

    private void finishTeleport(Player player, Location loc, boolean local) {
        player.getScheduler().run(plugin, task -> {
            player.teleportAsync(loc).thenAccept(result -> {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().rtpTeleported()));
                player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                setCooldown(player.getUniqueId());
                currentlyTeleporting.remove(player.getUniqueId());
            });
        }, null);
    }

    private void startVisualSequence(Player player, Runnable onComplete) {
        AtomicInteger count = new AtomicInteger(0);

        player.showTitle(Title.title(
                MiniMessage.miniMessage().deserialize(messages.get().rtpSearching()),
                MiniMessage.miniMessage().deserialize(""),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ZERO)
        ));

        Bukkit.getRegionScheduler().runAtFixedRate(plugin, player.getLocation(), task -> {
            if (!player.isOnline()) {
                task.cancel();
                currentlyTeleporting.remove(player.getUniqueId());
                return;
            }

            int current = count.getAndIncrement();

            if (current >= 3) {
                task.cancel();
                onComplete.run();
                return;
            }

            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
            
            Location loc = player.getLocation();
            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(x, 1, z), 1, 0, 0, 0, 0);
            }

        }, 1, 20L);
    }

    private boolean isOnCooldown(UUID uuid) {
        if (!cooldowns.containsKey(uuid)) return false;
        long lastUsed = cooldowns.get(uuid);
        return (System.currentTimeMillis() - lastUsed) < (config.get().rtpCooldownSeconds() * 1000L);
    }

    private void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }
}
