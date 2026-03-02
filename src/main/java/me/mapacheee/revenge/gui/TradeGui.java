package me.mapacheee.revenge.gui;

import me.mapacheee.revenge.api.RevengeCoreAPI;
import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.TradeSession;
import me.mapacheee.revenge.service.TradeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


import me.mapacheee.revenge.channel.CrossTradeMessage;
import me.mapacheee.revenge.service.InventorySyncService;

@ListenerComponent
public class TradeGui implements Listener {

    private final Container<Messages> messages;
    private final TradeService tradeService;
    private final Plugin plugin;

    private final Map<UUID, Inventory> openTradeInventories = new ConcurrentHashMap<>();
    private final Map<UUID, TradeSession> openTradeSessions = new ConcurrentHashMap<>();
    private final Set<UUID> awaitingMoneyInput = ConcurrentHashMap.newKeySet();
    private final Set<UUID> closingTrade = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> sessionPartners = new ConcurrentHashMap<>();

    private static final int[] SEPARATOR_SLOTS = {4, 13, 22, 31, 40, 49};
    private static final int[] PLAYER_A_ITEM_SLOTS = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21};
    private static final int[] PLAYER_B_ITEM_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26};
    private static final int PLAYER_A_MONEY_SLOT = 27;
    private static final int PLAYER_B_MONEY_SLOT = 35;
    private static final int PLAYER_A_CONFIRM_SLOT = 36;
    private static final int PLAYER_B_CONFIRM_SLOT = 44;

    @Inject
    public TradeGui(Container<Messages> messages, TradeService tradeService, Plugin plugin, InventorySyncService inventorySyncService) {
        this.messages = messages;
        this.tradeService = tradeService;
        this.plugin = plugin;
        this.inventorySyncService = inventorySyncService;
        tradeService.setTradeGui(this);
    }

    private final InventorySyncService inventorySyncService;

    public void openTrade(Player playerA, Player playerB, TradeSession session) {
        Component titleA = MiniMessage.miniMessage().deserialize(
            messages.get().tradeGuiTitle(),
            Placeholder.unparsed("player", playerB.getName())
        );
        Component titleB = MiniMessage.miniMessage().deserialize(
            messages.get().tradeGuiTitle(),
            Placeholder.unparsed("player", playerA.getName())
        );

        Inventory invA = Bukkit.createInventory(null, 54, titleA);
        Inventory invB = Bukkit.createInventory(null, 54, titleB);

        setupInventory(invA);
        setupInventory(invB);

        updateMoneyDisplay(invA, session);
        updateMoneyDisplay(invB, session);
        updateConfirmButtons(invA, session);
        updateConfirmButtons(invB, session);

        openTradeInventories.put(playerA.getUniqueId(), invA);
        openTradeInventories.put(playerB.getUniqueId(), invB);
        openTradeSessions.put(playerA.getUniqueId(), session);
        openTradeSessions.put(playerB.getUniqueId(), session);
        sessionPartners.put(playerA.getUniqueId(), playerB.getUniqueId());
        sessionPartners.put(playerB.getUniqueId(), playerA.getUniqueId());

        playerA.getScheduler().run(plugin, task -> playerA.openInventory(invA), null);
        playerB.getScheduler().run(plugin, task -> playerB.openInventory(invB), null);
    }

    public void openTradeCrossServer(Player localPlayer, TradeSession session, String partnerName) {
        Component title = MiniMessage.miniMessage().deserialize(
            messages.get().tradeGuiTitle(),
            Placeholder.unparsed("player", partnerName)
        );
        Inventory inv = Bukkit.createInventory(null, 54, title);
        setupInventory(inv);
        updateMoneyDisplay(inv, session);
        updateConfirmButtons(inv, session);

        openTradeInventories.put(localPlayer.getUniqueId(), inv);
        openTradeSessions.put(localPlayer.getUniqueId(), session);
        UUID otherUuid = session.getOtherPlayer(localPlayer.getUniqueId());
        sessionPartners.put(localPlayer.getUniqueId(), otherUuid);

        localPlayer.getScheduler().run(plugin, task -> localPlayer.openInventory(inv), null);
    }

    private void setupInventory(Inventory inv) {
        ItemStack separator = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta sepMeta = separator.getItemMeta();
        sepMeta.displayName(Component.empty());
        separator.setItemMeta(sepMeta);
        for (int slot : SEPARATOR_SLOTS) {
            inv.setItem(slot, separator.clone());
        }

        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayPane.getItemMeta();
        grayMeta.displayName(Component.empty());
        grayPane.setItemMeta(grayMeta);
        for (int i = 27; i < 54; i++) {
            boolean isSep = false;
            for (int s : SEPARATOR_SLOTS) if (s == i) { isSep = true; break; }
            if (isSep) continue;
            if (i == PLAYER_A_MONEY_SLOT || i == PLAYER_B_MONEY_SLOT ||
                i == PLAYER_A_CONFIRM_SLOT || i == PLAYER_B_CONFIRM_SLOT) continue;
            inv.setItem(i, grayPane.clone());
        }
    }

    private void updateMoneyDisplay(Inventory inv, TradeSession session) {
        ItemStack moneyA = new ItemStack(Material.GOLD_INGOT);
        ItemMeta metaA = moneyA.getItemMeta();
        List<Component> loreA = new ArrayList<>();
        loreA.add(MiniMessage.miniMessage().deserialize(messages.get().tradeMoneyOffer(), Placeholder.unparsed("amount", String.format("%.2f", session.getMoneyA()))).decoration(TextDecoration.ITALIC, false));
        loreA.add(Component.empty());
        loreA.add(MiniMessage.miniMessage().deserialize(messages.get().tradeMoneyClick()).decoration(TextDecoration.ITALIC, false));
        metaA.displayName(MiniMessage.miniMessage().deserialize(messages.get().tradeMoneyLabel()).decoration(TextDecoration.ITALIC, false));
        metaA.lore(loreA);
        moneyA.setItemMeta(metaA);
        inv.setItem(PLAYER_A_MONEY_SLOT, moneyA);

        ItemStack moneyB = new ItemStack(Material.GOLD_INGOT);
        ItemMeta metaB = moneyB.getItemMeta();
        List<Component> loreB = new ArrayList<>();
        loreB.add(MiniMessage.miniMessage().deserialize(messages.get().tradeMoneyOffer(), Placeholder.unparsed("amount", String.format("%.2f", session.getMoneyB()))).decoration(TextDecoration.ITALIC, false));
        loreB.add(Component.empty());
        loreB.add(MiniMessage.miniMessage().deserialize(messages.get().tradeMoneyClick()).decoration(TextDecoration.ITALIC, false));
        metaB.displayName(MiniMessage.miniMessage().deserialize(messages.get().tradeMoneyLabel()).decoration(TextDecoration.ITALIC, false));
        metaB.lore(loreB);
        moneyB.setItemMeta(metaB);
        inv.setItem(PLAYER_B_MONEY_SLOT, moneyB);
    }

    private void updateConfirmButtons(Inventory inv, TradeSession session) {
        ItemStack confirmA = new ItemStack(session.isConfirmedA() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta confirmAMeta = confirmA.getItemMeta();
        confirmAMeta.displayName(MiniMessage.miniMessage().deserialize(
            session.isConfirmedA() ? messages.get().tradeConfirmed() : messages.get().tradeConfirm()
        ).decoration(TextDecoration.ITALIC, false));
        confirmA.setItemMeta(confirmAMeta);
        inv.setItem(PLAYER_A_CONFIRM_SLOT, confirmA);

        ItemStack confirmB = new ItemStack(session.isConfirmedB() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta confirmBMeta = confirmB.getItemMeta();
        confirmBMeta.displayName(MiniMessage.miniMessage().deserialize(
            session.isConfirmedB() ? messages.get().tradeConfirmed() : messages.get().tradeConfirm()
        ).decoration(TextDecoration.ITALIC, false));
        confirmB.setItemMeta(confirmBMeta);
        inv.setItem(PLAYER_B_CONFIRM_SLOT, confirmB);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        TradeSession session = openTradeSessions.get(player.getUniqueId());
        if (session == null) return;

        Inventory tradeInv = openTradeInventories.get(player.getUniqueId());
        if (tradeInv == null || !event.getInventory().equals(tradeInv)) return;

        int slot = event.getRawSlot();
        boolean isPlayerA = session.isPlayerA(player.getUniqueId());

        if (event.getClick().isShiftClick() && event.getClickedInventory() != null && !event.getClickedInventory().equals(tradeInv)) {
            int[] mySlots = isPlayerA ? PLAYER_A_ITEM_SLOTS : PLAYER_B_ITEM_SLOTS;
            int targetSlot = -1;
            for (int s : mySlots) {
                if (tradeInv.getItem(s) == null || tradeInv.getItem(s).getType() == Material.AIR) {
                    targetSlot = s;
                    break;
                }
            }
            if (targetSlot != -1) {
                event.setCancelled(true);
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() != Material.AIR) {
                    tradeInv.setItem(targetSlot, item.clone());
                    event.setCurrentItem(null);
                    syncSlot(session, targetSlot);
                    
                    if (!isPartnerLocal(player.getUniqueId())) {
                        CrossTradeMessage msg = new CrossTradeMessage(CrossTradeMessage.Action.SYNC_SLOT, 
                            RevengeCoreAPI.get().getServerName(),
                            player.getUniqueId().toString(), sessionPartners.get(player.getUniqueId()).toString(), 
                            player.getName(), "");
                        msg.slot = targetSlot;
                        msg.itemData = item != null ? inventorySyncService.serializeItems(new ItemStack[]{item}) : "";
                        tradeService.sendTradeSyncMessage(msg);
                    }
                    
                    resetAndSyncConfirmations(session);
                }
            } else {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(tradeInv)) {
            int[] mySlots = isPlayerA ? PLAYER_A_ITEM_SLOTS : PLAYER_B_ITEM_SLOTS;
            int[] theirSlots = isPlayerA ? PLAYER_B_ITEM_SLOTS : PLAYER_A_ITEM_SLOTS;

            boolean isMySlot = false;
            for (int s : mySlots) if (s == slot) { isMySlot = true; break; }

            boolean isTheirSlot = false;
            for (int s : theirSlots) if (s == slot) { isTheirSlot = true; break; }

            if (isTheirSlot) {
                event.setCancelled(true);
                return;
            }

            for (int s : SEPARATOR_SLOTS) {
                if (s == slot) { event.setCancelled(true); return; }
            }

            if (slot >= 27 && slot < 54) {
                event.setCancelled(true);

                int myMoneySlot = isPlayerA ? PLAYER_A_MONEY_SLOT : PLAYER_B_MONEY_SLOT;
                if (slot == myMoneySlot) {
                    awaitingMoneyInput.add(player.getUniqueId());
                    player.getScheduler().run(plugin, task -> player.closeInventory(), null);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeMoneyPrompt()));
                    
                    player.showTitle(Title.title(
                        MiniMessage.miniMessage().deserialize(messages.get().tradeMoneyTitle()),
                        MiniMessage.miniMessage().deserialize(messages.get().tradeMoneySubtitle())
                    ));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    return;
                }

                int myConfirmSlot = isPlayerA ? PLAYER_A_CONFIRM_SLOT : PLAYER_B_CONFIRM_SLOT;
                if (slot == myConfirmSlot) {
                    if (isPlayerA) {
                        session.setConfirmedA(!session.isConfirmedA());
                    } else {
                        session.setConfirmedB(!session.isConfirmedB());
                    }

                    updateConfirmButtons(tradeInv, session);
                    Inventory partnerInv = openTradeInventories.get(sessionPartners.get(player.getUniqueId()));
                    if (partnerInv != null) updateConfirmButtons(partnerInv, session);

                    if (partnerInv == null) {
                        sendSyncUpdate(player.getUniqueId(), sessionPartners.get(player.getUniqueId()), CrossTradeMessage.Action.SYNC_CONFIRM, session);
                    }

                    if (session.isBothConfirmed()) {
                        collectItemsFromGui(session);

                        Player playerAFromSession = Bukkit.getPlayer(session.getPlayerA());
                        Player playerBFromSession = Bukkit.getPlayer(session.getPlayerB());

                        closingTrade.add(session.getPlayerA());
                        closingTrade.add(session.getPlayerB());

                        if (playerAFromSession != null) playerAFromSession.getScheduler().run(plugin, task -> playerAFromSession.closeInventory(), null);
                        if (playerBFromSession != null) playerBFromSession.getScheduler().run(plugin, task -> playerBFromSession.closeInventory(), null);

                        openTradeInventories.remove(session.getPlayerA());
                        openTradeInventories.remove(session.getPlayerB());
                        openTradeSessions.remove(session.getPlayerA());
                        openTradeSessions.remove(session.getPlayerB());
                        closingTrade.remove(session.getPlayerA());
                        closingTrade.remove(session.getPlayerB());

                        boolean success = tradeService.completeTrade(session);
                        if (!success) {
                            returnItems(session);
                        }
                    }
                    return;
                }

                return;
            }

            if (isMySlot) {
                session.resetConfirmations();
                player.getScheduler().run(plugin, task -> {
                    syncSlot(session, slot);
                    updateBothConfirmButtons(session);
                    
                    if (!isPartnerLocal(player.getUniqueId())) {
                        CrossTradeMessage msg = new CrossTradeMessage(CrossTradeMessage.Action.SYNC_SLOT, 
                            RevengeCoreAPI.get().getServerName(),
                            player.getUniqueId().toString(), sessionPartners.get(player.getUniqueId()).toString(), 
                            player.getName(), "");
                        msg.slot = slot;
                        ItemStack item = tradeInv.getItem(slot);
                        msg.itemData = item != null ? inventorySyncService.serializeItems(new ItemStack[]{item}) : "";
                        tradeService.sendTradeSyncMessage(msg);
                        
                        sendSyncUpdate(player.getUniqueId(), sessionPartners.get(player.getUniqueId()), CrossTradeMessage.Action.SYNC_CONFIRM, session);
                    }
                }, null);
            }
        }
    }

    private boolean isPartnerLocal(UUID playerUuid) {
        UUID partnerUuid = sessionPartners.get(playerUuid);
        return partnerUuid != null && Bukkit.getPlayer(partnerUuid) != null;
    }

    private void sendSyncUpdate(UUID localUuid, UUID remoteUuid, CrossTradeMessage.Action action, TradeSession session) {
        CrossTradeMessage msg = new CrossTradeMessage(action, RevengeCoreAPI.get().getServerName(), localUuid.toString(), remoteUuid.toString(), "", "");
        if (action == CrossTradeMessage.Action.SYNC_MONEY) {
            msg.amount = session.isPlayerA(localUuid) ? session.getMoneyA() : session.getMoneyB();
        } else if (action == CrossTradeMessage.Action.SYNC_CONFIRM) {
            msg.state = session.isPlayerA(localUuid) ? session.isConfirmedA() : session.isConfirmedB();
        }
        tradeService.sendTradeSyncMessage(msg);
    }

    public void externallyUpdateSlot(UUID playerUuid, int slot, String itemData) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        player.getScheduler().run(plugin, task -> {
            Inventory inv = openTradeInventories.get(playerUuid);
            if (inv == null) return;
            
            ItemStack[] items = itemData.isEmpty() ? null : inventorySyncService.deserializeItems(itemData);
            ItemStack item = (items != null && items.length > 0) ? items[0] : null;
            
            inv.setItem(slot, item);
            
            TradeSession session = openTradeSessions.get(playerUuid);
            if (session != null) {
                session.resetConfirmations();
                updateConfirmButtons(inv, session);
            }
        }, null);
    }

    public void externallyUpdateMoney(UUID playerUuid, double amount) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        player.getScheduler().run(plugin, task -> {
            Inventory inv = openTradeInventories.get(playerUuid);
            TradeSession session = openTradeSessions.get(playerUuid);
            if (inv == null || session == null) return;

            UUID remoteUuid = sessionPartners.get(playerUuid);
            if (session.isPlayerA(remoteUuid)) {
                session.setMoneyA(amount);
            } else {
                session.setMoneyB(amount);
            }
            session.resetConfirmations();
            updateMoneyDisplay(inv, session);
            updateConfirmButtons(inv, session);
        }, null);
    }

    public void externallyUpdateConfirm(UUID playerUuid, boolean state) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        player.getScheduler().run(plugin, task -> {
            Inventory inv = openTradeInventories.get(playerUuid);
            TradeSession session = openTradeSessions.get(playerUuid);
            if (inv == null || session == null) return;

            UUID remoteUuid = sessionPartners.get(playerUuid);
            if (session.isPlayerA(remoteUuid)) {
                session.setConfirmedA(state);
            } else {
                session.setConfirmedB(state);
            }
            updateConfirmButtons(inv, session);
            
            if (session.isBothConfirmed()) {
                handleTradeCompletion(playerUuid, session);
            }
        }, null);
    }

    private void handleTradeCompletion(UUID uuid, TradeSession session) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        closingTrade.add(uuid);
        
        collectItemsFromGui(session);
        
        player.getScheduler().run(plugin, task -> player.closeInventory(), null);
        
        openTradeInventories.remove(uuid);
        openTradeSessions.remove(uuid);
        closingTrade.remove(uuid);
        sessionPartners.remove(uuid);

        tradeService.completeTrade(session);
    }


    private void syncSlot(TradeSession session, int slot) {
        Inventory invA = openTradeInventories.get(session.getPlayerA());
        Inventory invB = openTradeInventories.get(session.getPlayerB());
        if (invA == null || invB == null) return;

        boolean isASlot = false;
        for (int s : PLAYER_A_ITEM_SLOTS) if (s == slot) { isASlot = true; break; }

        if (isASlot) {
            ItemStack item = invA.getItem(slot);
            invB.setItem(slot, item != null ? item.clone() : null);
        } else {
            // It's a B slot, sync from B to A
            ItemStack item = invB.getItem(slot);
            invA.setItem(slot, item != null ? item.clone() : null);
        }
    }

    private void updateBothConfirmButtons(TradeSession session) {
        Inventory invA = openTradeInventories.get(session.getPlayerA());
        Inventory invB = openTradeInventories.get(session.getPlayerB());
        if (invA != null) updateConfirmButtons(invA, session);
        if (invB != null) updateConfirmButtons(invB, session);
    }

    private void resetAndSyncConfirmations(TradeSession session) {
        session.resetConfirmations();
        updateBothConfirmButtons(session);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingMoneyInput.remove(player.getUniqueId())) return;

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        try {
            double amount = Double.parseDouble(input);
            if (amount < 0) amount = 0;

            TradeService.EconomyResult ecoResult = tradeService.canAfford(player.getUniqueId(), amount);
            if (ecoResult != TradeService.EconomyResult.SUCCESS) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeNotEnoughMoney()));
                returnToTrade(player);
                return;
            }

            TradeSession session = openTradeSessions.get(player.getUniqueId());
            if (session == null) {
                return;
            }

            final double finalAmount = amount;
            if (session.isPlayerA(player.getUniqueId())) {
                session.setMoneyA(finalAmount);
            } else {
                session.setMoneyB(finalAmount);
            }
            session.resetConfirmations();

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().tradeMoneySet(), Placeholder.unparsed("amount", String.format("%.2f", finalAmount))));

            Inventory invA = openTradeInventories.get(session.getPlayerA());
            Inventory invB = openTradeInventories.get(session.getPlayerB());
            
            Inventory partnerInv = openTradeInventories.get(sessionPartners.get(player.getUniqueId()));
            if (partnerInv != null) {
                updateMoneyDisplay(partnerInv, session);
                updateConfirmButtons(partnerInv, session);
            }
            
            if (invA != null) updateMoneyDisplay(invA, session);
            if (invB != null) updateMoneyDisplay(invB, session);
            
            if (partnerInv == null) {
                sendSyncUpdate(player.getUniqueId(), sessionPartners.get(player.getUniqueId()), CrossTradeMessage.Action.SYNC_MONEY, session);
                sendSyncUpdate(player.getUniqueId(), sessionPartners.get(player.getUniqueId()), CrossTradeMessage.Action.SYNC_CONFIRM, session);
            }

            returnToTrade(player);
        } catch (NumberFormatException e) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeInvalidAmount()));
            returnToTrade(player);
        }
    }

    private void returnToTrade(Player player) {
        Inventory inv = openTradeInventories.get(player.getUniqueId());
        if (inv != null) {
            player.getScheduler().run(plugin, task -> player.openInventory(inv), null);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();

        if (awaitingMoneyInput.contains(uuid)) return;
        if (closingTrade.contains(uuid)) return;

        TradeSession session = openTradeSessions.remove(uuid);
        Inventory inv = openTradeInventories.remove(uuid);

        if (session == null) return;

        UUID otherUuid = session.isPlayerA(uuid) ? session.getPlayerB() : session.getPlayerA();
        closingTrade.add(otherUuid);
        Player other = Bukkit.getPlayer(otherUuid);
        if (other != null) {
            other.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().tradeCancelled()));
            other.getScheduler().run(plugin, task -> {
                if (other.getOpenInventory().getTopInventory().getHolder() == null) {
                    other.closeInventory();
                }
            }, null);
        }

        returnItemsToPlayer(player, inv, session.isPlayerA(uuid) ? PLAYER_A_ITEM_SLOTS : PLAYER_B_ITEM_SLOTS);

        Inventory otherInv = openTradeInventories.remove(otherUuid);
        openTradeSessions.remove(otherUuid);
        if (other != null && otherInv != null) {
            returnItemsToPlayer(other, otherInv, session.isPlayerA(otherUuid) ? PLAYER_A_ITEM_SLOTS : PLAYER_B_ITEM_SLOTS);
        }

        closingTrade.remove(uuid);
        closingTrade.remove(otherUuid);
        sessionPartners.remove(uuid);
        sessionPartners.remove(otherUuid);
        
        // Only notify service if this was the initiating close (to avoid loops)
        if (tradeService.getSession(uuid) != null) {
            tradeService.cancelTrade(uuid);
        }
    }

    private void returnItemsToPlayer(Player player, Inventory inv, int[] slots) {
        if (inv == null) return;
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item).values().forEach(remaining -> 
                    player.getWorld().dropItemNaturally(player.getLocation(), remaining)
                );
                inv.setItem(slot, null);
            }
        }
    }

    private void collectItemsFromGui(TradeSession session) {
        session.getItemsA().clear();
        session.getItemsB().clear();

        Player pA = Bukkit.getPlayer(session.getPlayerA());
        Player pB = Bukkit.getPlayer(session.getPlayerB());
        
        Inventory inv = null;
        if (pA != null) inv = openTradeInventories.get(session.getPlayerA());
        else if (pB != null) inv = openTradeInventories.get(session.getPlayerB());

        if (inv != null) {
            for (int slot : PLAYER_A_ITEM_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    session.getItemsA().add(item.clone());
                }
            }
            for (int slot : PLAYER_B_ITEM_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    session.getItemsB().add(item.clone());
                }
            }
        }
    }


    private void returnItems(TradeSession session) {
        Player playerA = Bukkit.getPlayer(session.getPlayerA());
        Player playerB = Bukkit.getPlayer(session.getPlayerB());

        if (playerA != null) {
            for (ItemStack item : session.getItemsA()) {
                if (item != null) playerA.getInventory().addItem(item);
            }
        }
        if (playerB != null) {
            for (ItemStack item : session.getItemsB()) {
                if (item != null) playerB.getInventory().addItem(item);
            }
        }
    }
}
