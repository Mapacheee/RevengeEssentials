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
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossTradeMessage;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradeService {

    private final Container<Messages> messages;
    private final Container<Config> config;
    private final EconomyService economyService;
    private final PlayerDataService playerDataService;
    private final Plugin plugin;

    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> requestTimeouts = new ConcurrentHashMap<>();
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerServers = new ConcurrentHashMap<>();

    private TradeGui tradeGui;
    private static final String TRADE_CHANNEL = "revenge:trade";

    @Inject
    public TradeService(Container<Messages> messages, Container<Config> config,
                        EconomyService economyService, PlayerDataService playerDataService, Plugin plugin) {
        this.messages = messages;
        this.config = config;
        this.economyService = economyService;
        this.playerDataService = playerDataService;
        this.plugin = plugin;
    }

    @OnEnable
    public void onEnable() {
        RevengeCoreAPI.get().getChannelService().subscribe(TRADE_CHANNEL, CrossTradeMessage.class, msg -> {
            if (msg.serverId != null && msg.serverId.equals(RevengeCoreAPI.get().getServerName())) {
                return;
            }
            handleCrossTradeMessage(msg);
        }, plugin.getSLF4JLogger());
    }

    private void handleCrossTradeMessage(CrossTradeMessage msg) {
        UUID senderUuid = UUID.fromString(msg.senderUuid);
        UUID targetUuid = UUID.fromString(msg.targetUuid);

        switch (CrossTradeMessage.Action.valueOf(msg.action)) {
            case REQUEST -> {
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null && target.isOnline()) {
                    pendingRequests.put(targetUuid, senderUuid);
                    playerServers.put(senderUuid, msg.senderName); 
                    
                    target.sendMessage(MiniMessage.miniMessage().deserialize(
                        messages.get().tradeRequestReceived(), Placeholder.unparsed("player", msg.senderName)));
                    
                    int timeoutSeconds = config.get().tradeRequestTimeoutSeconds();
                    ScheduledTask timeoutTask = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
                        if (pendingRequests.remove(targetUuid) != null) {
                            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeExpired()));
                            sendTradeMessage(CrossTradeMessage.Action.CANCEL, targetUuid, senderUuid, target.getName(), msg.senderName);
                        }
                        requestTimeouts.remove(targetUuid);
                    }, timeoutSeconds * 20L);
                    requestTimeouts.put(targetUuid, timeoutTask);
                }
            }
            case ACCEPT -> {
                Player requester = Bukkit.getPlayer(targetUuid); 
                if (requester != null && requester.isOnline()) {
                    requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeAccepted()));
                    startTradeSession(targetUuid, senderUuid, requester.getName(), msg.senderName, false);
                }
            }
            case DENY -> {
                Player requester = Bukkit.getPlayer(targetUuid);
                if (requester != null && requester.isOnline()) {
                    requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeDenied()));
                }
            }
            case CANCEL -> {
                Player p = Bukkit.getPlayer(targetUuid);
                if (p != null) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCancelled()));
                    p.getScheduler().run(plugin, task -> {
                        p.closeInventory();
                    }, null);
                }
                activeSessions.remove(targetUuid);
                activeSessions.remove(senderUuid);
            }
            case SYNC_SLOT -> {
                if (tradeGui != null) {
                    tradeGui.externallyUpdateSlot(targetUuid, msg.slot, msg.itemData);
                }
            }
            case SYNC_MONEY -> {
                if (tradeGui != null) {
                    tradeGui.externallyUpdateMoney(targetUuid, msg.amount);
                }
            }
            case SYNC_CONFIRM -> {
                if (tradeGui != null) {
                    tradeGui.externallyUpdateConfirm(targetUuid, msg.state);
                }
            }
            case COMPLETED -> {
                TradeSession session = activeSessions.get(targetUuid);
                if (session == null) return;
                
                if (completeTrade(session)) {
                    Player p = Bukkit.getPlayer(targetUuid);
                    if (p != null) {
                        p.getScheduler().run(plugin, task -> p.closeInventory(), null);
                    }
                }
            }
            case READY_TO_CLOSE -> {}
        }
    }




    public void setTradeGui(TradeGui tradeGui) {
        this.tradeGui = tradeGui;
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public void requestTrade(Player requester, UUID targetUuid, String targetName) {
        if (requester.getUniqueId().equals(targetUuid)) {
            requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeSelf()));
            return;
        }

        Player localTarget = Bukkit.getPlayer(targetUuid);
        if (localTarget != null && localTarget.isOnline()) {
            requestTradeLocal(requester, localTarget);
            return;
        }

        if (activeSessions.containsKey(requester.getUniqueId()) || pendingRequests.containsValue(requester.getUniqueId())) {
            requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeAlreadyPending()));
            return;
        }

        sendTradeMessage(CrossTradeMessage.Action.REQUEST, requester.getUniqueId(), targetUuid, requester.getName(), targetName);
        requester.sendMessage(MiniMessage.miniMessage().deserialize(
            messages.get().tradeRequestSent(), Placeholder.unparsed("player", targetName)));
    }

    private void requestTradeLocal(Player requester, Player target) {
        if (activeSessions.containsKey(requester.getUniqueId()) || activeSessions.containsKey(target.getUniqueId())) {
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
                requester.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeExpired()));
                target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeExpired()));
            }
            requestTimeouts.remove(target.getUniqueId());
        }, timeoutSeconds * 20L);
        requestTimeouts.put(target.getUniqueId(), timeoutTask);
    }

    public void acceptTrade(Player target) {
        UUID senderUuid = pendingRequests.remove(target.getUniqueId());
        if (senderUuid == null) {
            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeNoPending()));
            return;
        }

        ScheduledTask timeout = requestTimeouts.remove(target.getUniqueId());
        if (timeout != null) timeout.cancel();

        Player sender = Bukkit.getPlayer(senderUuid);
        if (sender != null && sender.isOnline()) {
            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeAccepted()));
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeAccepted()));
            startTradeSession(senderUuid, target.getUniqueId(), sender.getName(), target.getName(), true);
        } else {
            String senderName = playerServers.getOrDefault(senderUuid, "Unknown");
            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeAccepted()));
            sendTradeMessage(CrossTradeMessage.Action.ACCEPT, target.getUniqueId(), senderUuid, target.getName(), "");
            startTradeSession(senderUuid, target.getUniqueId(), senderName, target.getName(), false);
        }
    }

    private void startTradeSession(UUID playerA, UUID playerB, String nameA, String nameB, boolean local) {
        TradeSession session = new TradeSession(playerA, playerB);
        activeSessions.put(playerA, session);
        activeSessions.put(playerB, session);

        if (local) {
            Player pA = Bukkit.getPlayer(playerA);
            Player pB = Bukkit.getPlayer(playerB);
            if (pA != null && pB != null && tradeGui != null) {
                tradeGui.openTrade(pA, pB, session);
            }
        } else {
            Player localPlayer = Bukkit.getPlayer(playerA);
            if (localPlayer == null) localPlayer = Bukkit.getPlayer(playerB);
            
            if (localPlayer != null && tradeGui != null) {
                // Determine who the partner is and use their name
                String partnerName = localPlayer.getUniqueId().equals(playerA) ? nameB : nameA;
                tradeGui.openTradeCrossServer(localPlayer, session, partnerName);
            }
        }
    }

    public void denyTrade(Player target) {
        UUID senderUuid = pendingRequests.remove(target.getUniqueId());
        if (senderUuid == null) {
            target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeNoPending()));
            return;
        }

        ScheduledTask timeout = requestTimeouts.remove(target.getUniqueId());
        if (timeout != null) timeout.cancel();

        target.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeDenied()));
        Player sender = Bukkit.getPlayer(senderUuid);
        if (sender != null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeDenied()));
        } else {
            sendTradeMessage(CrossTradeMessage.Action.DENY, target.getUniqueId(), senderUuid, target.getName(), "");
        }
    }


    public void cancelTrade(UUID playerUuid) {
        TradeSession session = activeSessions.remove(playerUuid);
        if (session == null) return;

        UUID otherUuid = session.getOtherPlayer(playerUuid);
        activeSessions.remove(otherUuid);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCancelled()));

        Player other = Bukkit.getPlayer(otherUuid);
        if (other != null) {
            other.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCancelled()));
            other.getScheduler().run(plugin, task -> {
                if (other.getOpenInventory().getTopInventory().getHolder() == null) {
                    other.closeInventory();
                }
            }, null);
        } else {
            sendTradeMessage(CrossTradeMessage.Action.CANCEL, playerUuid, otherUuid, "", "");
        }
    }

    public boolean completeTrade(TradeSession session) {
        // Remove from active sessions IMMEDIATELY to prevent double processing
        // But only proceed if WE were the ones to remove it
        boolean wasA = activeSessions.remove(session.getPlayerA()) != null;
        boolean wasB = activeSessions.remove(session.getPlayerB()) != null;
        
        if (!wasA && !wasB) {
            return false; // Already processed
        }

        Player playerA = Bukkit.getPlayer(session.getPlayerA());
        Player playerB = Bukkit.getPlayer(session.getPlayerB());


        if (playerA != null) {
            completeHalf(playerA, session, true);
            playerA.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCompleted()));
        }
        if (playerB != null) {
            completeHalf(playerB, session, false);
            playerB.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCompleted()));
        }

        // If one player is remote, notify their server
        if (playerA == null || playerB == null) {
            UUID remoteUuid = playerA == null ? session.getPlayerA() : session.getPlayerB();
            UUID localUuid = playerA != null ? session.getPlayerA() : session.getPlayerB();
            sendTradeMessage(CrossTradeMessage.Action.COMPLETED, localUuid, remoteUuid, "", "");
        }

        return true;
    }


    private void completeHalf(Player player, TradeSession session, boolean isA) {
        double myMoney = isA ? session.getMoneyA() : session.getMoneyB();
        double theirMoney = isA ? session.getMoneyB() : session.getMoneyA();
        List<ItemStack> theirItems = isA ? session.getItemsB() : session.getItemsA();

        if (myMoney > 0) {
            economyService.removeBalance(player.getUniqueId(), player.getName(), myMoney);
        }
        if (theirMoney > 0) {
            economyService.addBalance(player.getUniqueId(), player.getName(), theirMoney);
        }

        for (ItemStack item : theirItems) {
            if (item != null) player.getInventory().addItem(item).values().forEach(remaining -> 
                player.getWorld().dropItemNaturally(player.getLocation(), remaining)
            );
        }
    }


    public void sendTradeMessage(CrossTradeMessage.Action action, UUID senderUuid, UUID targetUuid, String senderName, String targetName) {
        CrossTradeMessage msg = new CrossTradeMessage(action, RevengeCoreAPI.get().getServerName(), senderUuid.toString(), targetUuid.toString(), senderName, targetName);
        RevengeCoreAPI.get().getChannelService().publish(TRADE_CHANNEL, msg);
    }

    public void sendTradeSyncMessage(CrossTradeMessage msg) {
        msg.serverId = RevengeCoreAPI.get().getServerName();
        RevengeCoreAPI.get().getChannelService().publish(TRADE_CHANNEL, msg);
    }

    public enum EconomyResult {
        SUCCESS,
        NOT_ENOUGH_MONEY
    }

    public EconomyResult canAfford(UUID uuid, double amount) {
        Player player = Bukkit.getPlayer(uuid);
        String name = (player != null) ? player.getName() : "";
        
        double balance = economyService.getBalance(uuid, name).join();
        return balance >= amount ? EconomyResult.SUCCESS : EconomyResult.NOT_ENOUGH_MONEY;
    }

    public TradeSession getSession(UUID playerUuid) {
        return activeSessions.get(playerUuid);
    }
}
