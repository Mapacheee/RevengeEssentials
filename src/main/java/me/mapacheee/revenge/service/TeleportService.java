package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.TpaRequestMessage;
import me.mapacheee.revenge.channel.TpaResponseMessage;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class TeleportService {

    private me.mapacheee.revenge.service.ChannelService channelService;
    private final CrossServerService crossServerService;
    private final Container<Config> config;
    private final Container<Messages> messages;
    private final Plugin plugin;
    private final Map<UUID, TpaRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    @Inject
    public TeleportService(CrossServerService crossServerService, Container<Config> config,
            Container<Messages> messages, Plugin plugin) {
        this.crossServerService = crossServerService;
        this.config = config;
        this.messages = messages;
        this.plugin = plugin;
        startCleanupTask();
    }

    private me.mapacheee.revenge.service.ChannelService getChannelService() {
        if (channelService == null) {
            channelService = RevengeCoreAPI.get().getChannelService();
        }
        return channelService;
    }

    public void sendTpaRequest(Player sender, String targetName, boolean tpaHere) {
        if (isOnCooldown(sender.getUniqueId())) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tpaCooldown()));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target != null && target.isOnline()) {
            Location loc = sender.getLocation();
            pendingRequests.put(target.getUniqueId(), new TpaRequest(sender.getUniqueId(), sender.getName(),
                    target.getUniqueId(), target.getName(), tpaHere, getServerName(),
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                    System.currentTimeMillis()));
            setCooldown(sender.getUniqueId());

            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    tpaHere ? messages.get().tpaHereRequestSent() : messages.get().tpaRequestSent(),
                    Placeholder.unparsed("player", targetName)));
            target.sendMessage(MiniMessage.miniMessage().deserialize(
                    tpaHere ? messages.get().tpaHereRequestReceived() : messages.get().tpaRequestReceived(),
                    Placeholder.unparsed("player", sender.getName())));
            return;
        }

        Location loc = sender.getLocation();
        TpaRequestMessage msg = new TpaRequestMessage(
                getServerName(), sender.getUniqueId().toString(), sender.getName(),
                "", targetName, tpaHere, getServerName(),
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        getChannelService().publish("essentials:tpa:request", msg);
        setCooldown(sender.getUniqueId());
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                tpaHere ? messages.get().tpaHereRequestSent() : messages.get().tpaRequestSent(),
                Placeholder.unparsed("player", targetName)));
    }

    public void acceptTpa(Player target) {
        TpaRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tpaNoPending()));
            return;
        }

        target.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().tpaRequestAcceptedTarget(),
                Placeholder.unparsed("player", request.senderName())));

        Player sender = Bukkit.getPlayer(request.senderUuid());
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().tpaRequestAcceptedSender(),
                    Placeholder.unparsed("player", target.getName())));
            if (request.tpaHere()) {
                target.getScheduler().run(plugin, task -> target.teleportAsync(sender.getLocation()), null);
            } else {
                sender.getScheduler().run(plugin, task -> sender.teleportAsync(target.getLocation()), null);
            }
        } else {
            if (request.tpaHere()) {
                crossServerService.teleportCrossServer(target, request.senderServer(), request.reqWorld(), request.reqX(), request.reqY(), request.reqZ(), request.reqYaw(), request.reqPitch(), false);
            }
            Location loc = target.getLocation();
            TpaResponseMessage response = new TpaResponseMessage(
                    getServerName(), request.senderUuid().toString(), target.getUniqueId().toString(),
                    target.getName(), true, request.tpaHere(), getServerName(), loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            getChannelService().publish("essentials:tpa:response", response);
        }
    }

    public void denyTpa(Player target) {
        TpaRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tpaNoPending()));
            return;
        }

        target.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().tpaRequestDeniedTarget(),
                Placeholder.unparsed("player", request.senderName())));

        Player sender = Bukkit.getPlayer(request.senderUuid());
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().tpaRequestDeniedSender(),
                    Placeholder.unparsed("player", target.getName())));
        } else {
            TpaResponseMessage response = new TpaResponseMessage(
                    getServerName(), request.senderUuid().toString(), target.getUniqueId().toString(),
                    target.getName(), false, request.tpaHere(), getServerName(), "", 0, 0, 0, 0, 0);
            getChannelService().publish("essentials:tpa:response", response);
        }
    }

    public void teleportDirect(Player sender, Player target) {
        sender.getScheduler().run(plugin, task -> sender.teleportAsync(target.getLocation()), null);
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().tpTeleported(),
                Placeholder.unparsed("player", target.getName())));
    }

    public void handleIncomingTpaRequest(String senderUuid, String senderName, String targetName, boolean tpaHere,
            String senderServer, String reqWorld, double reqX, double reqY, double reqZ, float reqYaw, float reqPitch) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline())
            return;

        pendingRequests.put(target.getUniqueId(), new TpaRequest(
                UUID.fromString(senderUuid), senderName, target.getUniqueId(), target.getName(), tpaHere, senderServer,
                reqWorld, reqX, reqY, reqZ, reqYaw, reqPitch,
                System.currentTimeMillis()));

        target.sendMessage(MiniMessage.miniMessage().deserialize(
                tpaHere ? messages.get().tpaHereRequestReceived() : messages.get().tpaRequestReceived(),
                Placeholder.unparsed("player", senderName)));
    }

    public void handleTpaResponse(String senderUuid, String targetName, boolean accepted, boolean tpaHere, String targetServer,
            String targetWorld, double x, double y, double z, float yaw, float pitch) {
        Player sender = Bukkit.getPlayer(UUID.fromString(senderUuid));
        if (sender == null || !sender.isOnline())
            return;

        if (accepted) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().tpaRequestAcceptedSender(),
                    Placeholder.unparsed("player", targetName)));
            if (!tpaHere) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().crossServerTeleporting()));
                crossServerService.teleportCrossServer(sender, targetServer, targetWorld, x, y, z, yaw, pitch, false);
            }
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().tpaRequestDeniedSender(),
                    Placeholder.unparsed("player", targetName)));
        }
    }

    private void startCleanupTask() {
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            long now = System.currentTimeMillis();
            long timeout = config.get().tpaTimeoutSeconds() * 1000L;
            pendingRequests.entrySet().removeIf(entry -> {
                if (now - entry.getValue().timestamp() > timeout) {
                    Player target = Bukkit.getPlayer(entry.getKey());
                    if (target != null && target.isOnline()) {
                        target.getScheduler().run(plugin,
                                t -> target.sendMessage(
                                        MiniMessage.miniMessage().deserialize(messages.get().tpaRequestExpired())),
                                null);
                    }
                    return true;
                }
                return false;
            });
        }, 5, 5, TimeUnit.SECONDS);
    }

    private boolean isOnCooldown(UUID uuid) {
        Long cooldown = cooldowns.get(uuid);
        if (cooldown == null)
            return false;
        return System.currentTimeMillis() - cooldown < config.get().tpaCooldownSeconds() * 1000L;
    }

    private void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    private String getServerName() {
        try {
            return RevengeCoreAPI.get().getServerName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public record TpaRequest(UUID senderUuid, String senderName, UUID targetUuid, String targetName, boolean tpaHere,
            String senderServer, String reqWorld, double reqX, double reqY, double reqZ, float reqYaw, float reqPitch, long timestamp) {
    }
}
