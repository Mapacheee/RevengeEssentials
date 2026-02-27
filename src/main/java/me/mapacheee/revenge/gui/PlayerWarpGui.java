package me.mapacheee.revenge.gui;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.PlayerWarp;
import me.mapacheee.revenge.data.PlayerWarpGuiData;
import me.mapacheee.revenge.service.InventorySyncService;
import me.mapacheee.revenge.service.PlayerWarpService;
import me.mapacheee.revenge.service.CrossServerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@ListenerComponent
public class PlayerWarpGui implements Listener {

    private final PlayerWarpService pwarpService;
    private final CrossServerService crossServerService;
    private final Container<Messages> messages;
    private final InventorySyncService inventorySyncService;
    private final Plugin plugin;
    
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;

    private static final List<Integer> AVAILABLE_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );

    @Inject
    public PlayerWarpGui(PlayerWarpService pwarpService, CrossServerService crossServerService, Container<Messages> messages, InventorySyncService inventorySyncService, Plugin plugin) {
        this.pwarpService = pwarpService;
        this.crossServerService = crossServerService;
        this.messages = messages;
        this.inventorySyncService = inventorySyncService;
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        player.getScheduler().run(plugin, task -> {
            Component title = MiniMessage.miniMessage().deserialize(
                messages.get().pwarpGuiTitle() != null ? messages.get().pwarpGuiTitle() : "<aqua>Player Warps <gray>- Página <page>",
                Placeholder.parsed("page", String.valueOf(page + 1))
            );
            Inventory inv = Bukkit.createInventory(null, 54, title);

            PlayerWarpGuiData guiData = pwarpService.getGuiData();

            if (guiData != null && guiData.getDecorativeItems() != null) {
                for (Map.Entry<Integer, String> entry : guiData.getDecorativeItems().entrySet()) {
                    if (entry.getKey() >= 0 && entry.getKey() < 54 && entry.getKey() != PREV_PAGE_SLOT && entry.getKey() != NEXT_PAGE_SLOT) {
                        try {
                            ItemStack[] items = inventorySyncService.deserializeItems(entry.getValue());
                            if (items != null && items.length > 0 && items[0] != null) {
                                if (items[0].hasItemMeta() && items[0].getItemMeta().getPersistentDataContainer().getKeys().stream().anyMatch(k -> k.getKey().contains("warp"))) continue;
                                inv.setItem(entry.getKey(), items[0]);
                            }
                        } catch (Exception ignored) { }
                    }
                }
            }

            List<PlayerWarp> warps = new ArrayList<>(pwarpService.getPwarps());
            warps.sort(Comparator.comparing(PlayerWarp::getName));

            int maxPages = (int) Math.ceil((double) warps.size() / AVAILABLE_SLOTS.size());
            if (maxPages == 0) maxPages = 1;

            int actualPage = page;
            if (actualPage < 0) actualPage = 0;
            if (actualPage >= maxPages) actualPage = maxPages - 1;

            int startIndex = actualPage * AVAILABLE_SLOTS.size();
            int endIndex = Math.min(startIndex + AVAILABLE_SLOTS.size(), warps.size());

            AtomicInteger index = new AtomicInteger(0);

            for (int i = startIndex; i < endIndex; i++) {
                PlayerWarp warp = warps.get(i);
                
                Material mat = Material.matchMaterial(warp.getIconMaterial() != null ? warp.getIconMaterial().toUpperCase() : "ENDER_EYE");
                if (mat == null) mat = Material.ENDER_EYE;
                
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                
                meta.displayName(MiniMessage.miniMessage().deserialize(warp.getDisplayName()).decoration(TextDecoration.ITALIC, false));
                
                List<Component> lore = new ArrayList<>();
                String ownerName = warp.getOwnerUuid() != null ? Bukkit.getOfflinePlayer(warp.getOwnerUuid()).getName() : "Servidor";
                if (ownerName == null) ownerName = "Desconocido";
                
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Dueño: <white>" + ownerName).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                
                if (warp.getDescription() != null) {
                    for (String l : warp.getDescription()) {
                        lore.add(MiniMessage.miniMessage().deserialize(l).decoration(TextDecoration.ITALIC, false));
                    }
                }
                lore.add(Component.empty());
                lore.add(MiniMessage.miniMessage().deserialize(messages.get().pwarpClickTeleport() != null ? messages.get().pwarpClickTeleport() : "<aqua>▶ Click para viajar").decoration(TextDecoration.ITALIC, false));
                
                meta.lore(lore);
                
                NamespacedKey key = new NamespacedKey(plugin, "pwarp_gui_target");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, warp.getName());
                
                item.setItemMeta(meta);
                
                int slot = AVAILABLE_SLOTS.get(index.getAndIncrement());
                inv.setItem(slot, item);
            }

            if (actualPage > 0) {
                ItemStack prevBtn = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevBtn.getItemMeta();
                prevMeta.displayName(MiniMessage.miniMessage().deserialize("<aqua><bold>⮜ Página Anterior").decoration(TextDecoration.ITALIC, false));
                NamespacedKey pageKey = new NamespacedKey(plugin, "pwarp_gui_page");
                prevMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, actualPage - 1);
                prevBtn.setItemMeta(prevMeta);
                inv.setItem(PREV_PAGE_SLOT, prevBtn);
            } else if (guiData != null && guiData.getDecorativeItems() != null && guiData.getDecorativeItems().containsKey(PREV_PAGE_SLOT)) {
                try {
                    ItemStack[] items = inventorySyncService.deserializeItems(guiData.getDecorativeItems().get(PREV_PAGE_SLOT));
                    if (items != null && items.length > 0 && items[0] != null) {
                        if (items[0].hasItemMeta() && items[0].getItemMeta().getPersistentDataContainer().getKeys().stream().anyMatch(k -> k.getKey().contains("warp"))) return;
                        inv.setItem(PREV_PAGE_SLOT, items[0]);
                    }
                } catch (Exception ignored) {}
            }

            if (actualPage < maxPages - 1) {
                ItemStack nextBtn = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextBtn.getItemMeta();
                nextMeta.displayName(MiniMessage.miniMessage().deserialize("<aqua><bold>Página Siguiente ⮞").decoration(TextDecoration.ITALIC, false));
                NamespacedKey pageKey = new NamespacedKey(plugin, "pwarp_gui_page");
                nextMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, actualPage + 1);
                nextBtn.setItemMeta(nextMeta);
                inv.setItem(NEXT_PAGE_SLOT, nextBtn);
            } else if (guiData != null && guiData.getDecorativeItems() != null && guiData.getDecorativeItems().containsKey(NEXT_PAGE_SLOT)) {
                try {
                    ItemStack[] items = inventorySyncService.deserializeItems(guiData.getDecorativeItems().get(NEXT_PAGE_SLOT));
                    if (items != null && items.length > 0 && items[0] != null) {
                        if (items[0].hasItemMeta() && items[0].getItemMeta().getPersistentDataContainer().getKeys().stream().anyMatch(k -> k.getKey().contains("warp"))) return;
                        inv.setItem(NEXT_PAGE_SLOT, items[0]);
                    }
                } catch (Exception ignored) {}
            }

            player.openInventory(inv);
        }, null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        Inventory topInv = event.getView().getTopInventory();
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInv)) {
            if (topInv.getSize() == 54) {
               boolean isWarpGui = false;
               NamespacedKey pageKey = new NamespacedKey(plugin, "pwarp_gui_page");
               NamespacedKey warpKey = new NamespacedKey(plugin, "pwarp_gui_target");
               for (ItemStack it : topInv.getContents()) {
                   if (it != null && it.hasItemMeta() && (it.getItemMeta().getPersistentDataContainer().has(pageKey, PersistentDataType.INTEGER) || it.getItemMeta().getPersistentDataContainer().has(warpKey, PersistentDataType.STRING))) {
                       isWarpGui = true;
                       break;
                   }
               }
               if (isWarpGui) event.setCancelled(true);
            }
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            boolean isWarpGui = false;
            NamespacedKey pageKey = new NamespacedKey(plugin, "pwarp_gui_page");
            NamespacedKey warpKey = new NamespacedKey(plugin, "pwarp_gui_target");
            for (ItemStack it : topInv.getContents()) {
                if (it != null && it.hasItemMeta() && (it.getItemMeta().getPersistentDataContainer().has(pageKey, PersistentDataType.INTEGER) || it.getItemMeta().getPersistentDataContainer().has(warpKey, PersistentDataType.STRING))) {
                    isWarpGui = true;
                    break;
                }
            }
            if (isWarpGui) event.setCancelled(true);
            return;
        }

        NamespacedKey warpKey = new NamespacedKey(plugin, "pwarp_gui_target");
        NamespacedKey pageKey = new NamespacedKey(plugin, "pwarp_gui_page");
        PersistentDataContainer container = clicked.getItemMeta().getPersistentDataContainer();

        if (container.has(pageKey, PersistentDataType.INTEGER) || container.has(warpKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        } else {
            boolean isWarpGui = false;
            for (ItemStack it : topInv.getContents()) {
                if (it != null && it.hasItemMeta() && (it.getItemMeta().getPersistentDataContainer().has(pageKey, PersistentDataType.INTEGER) || it.getItemMeta().getPersistentDataContainer().has(warpKey, PersistentDataType.STRING))) {
                    isWarpGui = true;
                    break;
                }
            }
            if (isWarpGui) event.setCancelled(true);
        }

        if (container.has(pageKey, PersistentDataType.INTEGER)) {
            int targetPage = container.get(pageKey, PersistentDataType.INTEGER);
            open(player, targetPage);
            return;
        }

        if (container.has(warpKey, PersistentDataType.STRING)) {
            String warpName = container.get(warpKey, PersistentDataType.STRING);
            PlayerWarp warp = pwarpService.getPwarp(warpName);
            if (warp != null) {
                player.closeInventory();
                String tMsg = messages.get().pwarpTeleporting() != null ? messages.get().pwarpTeleporting() : "<yellow>Teletransportando a <warp>...";
                player.sendMessage(MiniMessage.miniMessage().deserialize(tMsg, Placeholder.parsed("warp", warp.getName())));
                crossServerService.teleportCrossServer(
                    player,
                    warp.getServer(),
                    warp.getWorld(),
                    warp.getX(),
                    warp.getY(),
                    warp.getZ(),
                    warp.getYaw(),
                    warp.getPitch(),
                    false
                );
            }
        }
    }
}
