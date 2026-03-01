package me.mapacheee.revenge.gui;

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
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

@ListenerComponent
public class TradeGui implements Listener {

    private final Container<Messages> messages;
    private final TradeService tradeService;
    private final Plugin plugin;

    private final Map<UUID, Inventory> openTradeInventories = new ConcurrentHashMap<>();
    private final Map<UUID, TradeSession> openTradeSessions = new ConcurrentHashMap<>();
    private final Set<UUID> awaitingMoneyInput = ConcurrentHashMap.newKeySet();
    private final Set<UUID> closingTrade = ConcurrentHashMap.newKeySet();

    private static final int[] SEPARATOR_SLOTS = {4, 13, 22, 31, 40, 49};
    private static final int[] PLAYER_A_ITEM_SLOTS = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21};
    private static final int[] PLAYER_B_ITEM_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26};
    private static final int PLAYER_A_MONEY_SLOT = 27;
    private static final int PLAYER_B_MONEY_SLOT = 35;
    private static final int PLAYER_A_CONFIRM_SLOT = 36;
    private static final int PLAYER_B_CONFIRM_SLOT = 44;

    @Inject
    public TradeGui(Container<Messages> messages, TradeService tradeService, Plugin plugin) {
        this.messages = messages;
        this.tradeService = tradeService;
        this.plugin = plugin;
        tradeService.setTradeGui(this);
    }

    public void openTrade(Player playerA, Player playerB, TradeSession session) {
        Component title = MiniMessage.miniMessage().deserialize(messages.get().tradeGuiTitle());
        Inventory inv = Bukkit.createInventory(null, 54, title);

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

        updateMoneyDisplay(inv, session);
        updateConfirmButtons(inv, session);

        openTradeInventories.put(playerA.getUniqueId(), inv);
        openTradeInventories.put(playerB.getUniqueId(), inv);
        openTradeSessions.put(playerA.getUniqueId(), session);
        openTradeSessions.put(playerB.getUniqueId(), session);

        playerA.getScheduler().run(plugin, task -> playerA.openInventory(inv), null);
        playerB.getScheduler().run(plugin, task -> playerB.openInventory(inv), null);
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

                if (session.isBothConfirmed()) {
                    collectItemsFromGui(tradeInv, session);

                    Player playerA = Bukkit.getPlayer(session.getPlayerA());
                    Player playerB = Bukkit.getPlayer(session.getPlayerB());

                    closingTrade.add(session.getPlayerA());
                    closingTrade.add(session.getPlayerB());

                    if (playerA != null) playerA.getScheduler().run(plugin, task -> playerA.closeInventory(), null);
                    if (playerB != null) playerB.getScheduler().run(plugin, task -> playerB.closeInventory(), null);

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
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> updateConfirmButtons(tradeInv, session));
        }
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

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().tradeMoneySet(), Placeholder.unparsed("amount", String.format("%.2f", finalAmount))));

            Inventory inv = openTradeInventories.get(player.getUniqueId());
            if (inv != null) {
                updateMoneyDisplay(inv, session);
                updateConfirmButtons(inv, session);
                player.getScheduler().run(plugin, task -> player.openInventory(inv), null);
            }
        } catch (NumberFormatException e) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().tradeInvalidAmount()));
            Inventory inv = openTradeInventories.get(player.getUniqueId());
            if (inv != null) {
                player.getScheduler().run(plugin, task -> player.openInventory(inv), null);
            }
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

        int[] mySlots = session.isPlayerA(uuid) ? PLAYER_A_ITEM_SLOTS : PLAYER_B_ITEM_SLOTS;
        if (inv != null) {
            for (int slot : mySlots) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    player.getInventory().addItem(item);
                    inv.setItem(slot, null);
                }
            }
        }

        tradeService.cancelTrade(uuid);
    }

    private void collectItemsFromGui(Inventory inv, TradeSession session) {
        session.getItemsA().clear();
        session.getItemsB().clear();

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
