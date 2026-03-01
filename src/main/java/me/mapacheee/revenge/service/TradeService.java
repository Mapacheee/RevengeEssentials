package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.TradeSession;
import me.mapacheee.revenge.gui.TradeGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradeService {

    private final Container<Messages> messages;
    private final Container<Config> config;
    private final EconomyService economyService;
    private final Plugin plugin;

    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> requestTimeouts = new ConcurrentHashMap<>();
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();

    private TradeGui tradeGui;

    @Inject
    public TradeService(Container<Messages> messages, Container<Config> config,
                        EconomyService economyService, Plugin plugin) {
        this.messages = messages;
        this.config = config;
        this.economyService = economyService;
        this.plugin = plugin;
    }

    public void setTradeGui(TradeGui tradeGui) {
        this.tradeGui = tradeGui;
    }

    public void requestTrade(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeSelf()));
            return;
        }

        if (activeSessions.containsKey(requester.getUniqueId()) || activeSessions.containsKey(target.getUniqueId())) {
            requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeAlreadyPending()));
            return;
        }

        if (pendingRequests.containsKey(target.getUniqueId()) || pendingRequests.containsValue(requester.getUniqueId())) {
            requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeAlreadyPending()));
            return;
        }

        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());

        requester.sendMessage(MiniMessage.miniMessage().deserialize(
            messages.get().tradeRequestSent(), Placeholder.unparsed("player", target.getName())));
        target.sendMessage(MiniMessage.miniMessage().deserialize(
            messages.get().tradeRequestReceived(), Placeholder.unparsed("player", requester.getName())));

        int timeoutSeconds = config.get().tradeRequestTimeoutSeconds();
        ScheduledTask timeoutTask = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
            if (pendingRequests.remove(target.getUniqueId()) != null) {
                Player req = Bukkit.getPlayer(requester.getUniqueId());
                Player tgt = Bukkit.getPlayer(target.getUniqueId());
                if (req != null) req.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeExpired()));
                if (tgt != null) tgt.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeExpired()));
            }
            requestTimeouts.remove(target.getUniqueId());
        }, timeoutSeconds * 20L);

        requestTimeouts.put(target.getUniqueId(), timeoutTask);
    }

    public void acceptTrade(Player target) {
        UUID requesterUuid = pendingRequests.remove(target.getUniqueId());
        if (requesterUuid == null) {
            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeNoPending()));
            return;
        }

        ScheduledTask timeout = requestTimeouts.remove(target.getUniqueId());
        if (timeout != null) timeout.cancel();

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeExpired()));
            return;
        }

        target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeAccepted()));
        requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeAccepted()));

        TradeSession session = new TradeSession(requester.getUniqueId(), target.getUniqueId());
        activeSessions.put(requester.getUniqueId(), session);
        activeSessions.put(target.getUniqueId(), session);

        if (tradeGui != null) {
            tradeGui.openTrade(requester, target, session);
        }
    }

    public void denyTrade(Player target) {
        UUID requesterUuid = pendingRequests.remove(target.getUniqueId());
        if (requesterUuid == null) {
            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeNoPending()));
            return;
        }

        ScheduledTask timeout = requestTimeouts.remove(target.getUniqueId());
        if (timeout != null) timeout.cancel();

        target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeDenied()));
        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null) {
            requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeDenied()));
        }
    }

    public void cancelTrade(UUID playerUuid) {
        TradeSession session = activeSessions.remove(playerUuid);
        if (session == null) return;

        UUID otherUuid = session.getOtherPlayer(playerUuid);
        activeSessions.remove(otherUuid);

        Player player = Bukkit.getPlayer(playerUuid);
        Player other = Bukkit.getPlayer(otherUuid);

        if (player != null) player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCancelled()));
        if (other != null) {
            other.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCancelled()));
            other.getScheduler().run(plugin, task -> other.closeInventory(), null);
        }
    }

    public boolean completeTrade(TradeSession session) {
        Player playerA = Bukkit.getPlayer(session.getPlayerA());
        Player playerB = Bukkit.getPlayer(session.getPlayerB());

        if (playerA == null || playerB == null) return false;

        if (session.getMoneyA() > 0) {
            double balanceA = economyService.getBalance(playerA.getUniqueId(), playerA.getName()).join();
            if (balanceA < session.getMoneyA()) return false;
        }
        if (session.getMoneyB() > 0) {
            double balanceB = economyService.getBalance(playerB.getUniqueId(), playerB.getName()).join();
            if (balanceB < session.getMoneyB()) return false;
        }

        if (session.getMoneyA() > 0) {
            economyService.removeBalance(playerA.getUniqueId(), playerA.getName(), session.getMoneyA());
            economyService.addBalance(playerB.getUniqueId(), playerB.getName(), session.getMoneyA());
        }
        if (session.getMoneyB() > 0) {
            economyService.removeBalance(playerB.getUniqueId(), playerB.getName(), session.getMoneyB());
            economyService.addBalance(playerA.getUniqueId(), playerA.getName(), session.getMoneyB());
        }

        for (ItemStack item : session.getItemsA()) {
            if (item != null) playerB.getInventory().addItem(item);
        }
        for (ItemStack item : session.getItemsB()) {
            if (item != null) playerA.getInventory().addItem(item);
        }

        activeSessions.remove(session.getPlayerA());
        activeSessions.remove(session.getPlayerB());

        playerA.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCompleted()));
        playerB.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCompleted()));

        return true;
    }

    public TradeSession getSession(UUID playerUuid) {
        return activeSessions.get(playerUuid);
    }

    public boolean hasActiveSession(UUID playerUuid) {
        return activeSessions.containsKey(playerUuid);
    }
}
