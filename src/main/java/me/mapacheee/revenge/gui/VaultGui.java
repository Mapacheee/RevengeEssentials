package me.mapacheee.revenge.gui;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.service.VaultService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ListenerComponent
public class VaultGui implements Listener {

    private final VaultService vaultService;
    private final Container<Messages> messages;
    private final Plugin plugin;

    private final Map<UUID, Integer> openVaultPages = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> openVaultRows = new ConcurrentHashMap<>();

    @Inject
    public VaultGui(VaultService vaultService, Container<Messages> messages, Plugin plugin) {
        this.vaultService = vaultService;
        this.messages = messages;
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        int maxRows = vaultService.getMaxRows(player);
        int maxPages = vaultService.getMaxPages(player);

        if (page < 1) page = 1;
        if (page > maxPages) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().vaultNoPermissionPage()));
            return;
        }

        final int finalPage = page;

        vaultService.loadVault(player, finalPage).thenAccept(vaultData -> {
            player.getScheduler().run(plugin, task -> {
                int totalRows = maxRows + 1;
                int totalSlots = totalRows * 9;

                Component title = MiniMessage.miniMessage().deserialize(
                    messages.get().vaultGuiTitle(),
                    Placeholder.parsed("page", String.valueOf(finalPage))
                );

                Inventory inv = Bukkit.createInventory(null, totalSlots, title);

                int itemSlots = maxRows * 9;
                if (vaultData != null && vaultData.getContentsBase64() != null) {
                    ItemStack[] items = vaultService.deserializeContents(vaultData.getContentsBase64(), itemSlots);
                    for (int i = 0; i < items.length && i < itemSlots; i++) {
                        if (items[i] != null) {
                            inv.setItem(i, items[i]);
                        }
                    }
                }

                int navRowStart = maxRows * 9;
                for (int i = navRowStart; i < totalSlots; i++) {
                    ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                    ItemMeta paneMeta = pane.getItemMeta();
                    paneMeta.displayName(Component.empty());
                    pane.setItemMeta(paneMeta);
                    inv.setItem(i, pane);
                }

                if (finalPage > 1) {
                    ItemStack prevBtn = new ItemStack(Material.ARROW);
                    ItemMeta prevMeta = prevBtn.getItemMeta();
                    prevMeta.displayName(MiniMessage.miniMessage().deserialize(messages.get().vaultPrevPage()).decoration(TextDecoration.ITALIC, false));
                    NamespacedKey pageKey = new NamespacedKey(plugin, "vault_page");
                    prevMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, finalPage - 1);
                    prevBtn.setItemMeta(prevMeta);
                    inv.setItem(navRowStart, prevBtn);
                }

                if (finalPage < maxPages) {
                    ItemStack nextBtn = new ItemStack(Material.ARROW);
                    ItemMeta nextMeta = nextBtn.getItemMeta();
                    nextMeta.displayName(MiniMessage.miniMessage().deserialize(messages.get().vaultNextPage()).decoration(TextDecoration.ITALIC, false));
                    NamespacedKey pageKey = new NamespacedKey(plugin, "vault_page");
                    nextMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, finalPage + 1);
                    nextBtn.setItemMeta(nextMeta);
                    inv.setItem(navRowStart + 8, nextBtn);
                }

                openVaultPages.put(player.getUniqueId(), finalPage);
                openVaultRows.put(player.getUniqueId(), maxRows);

                player.openInventory(inv);
            }, null);
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openVaultPages.containsKey(player.getUniqueId())) return;

        int maxRows = openVaultRows.getOrDefault(player.getUniqueId(), 1);
        int navRowStart = maxRows * 9;

        if (event.getRawSlot() >= navRowStart && event.getRawSlot() < (maxRows + 1) * 9) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.hasItemMeta()) {
                NamespacedKey pageKey = new NamespacedKey(plugin, "vault_page");
                Integer targetPage = clicked.getItemMeta().getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
                if (targetPage != null) {
                    int currentPage = openVaultPages.getOrDefault(player.getUniqueId(), 1);
                    Inventory currentInv = event.getInventory();

                    Inventory tempInv = Bukkit.createInventory(null, maxRows * 9);
                    for (int i = 0; i < maxRows * 9; i++) {
                        ItemStack item = currentInv.getItem(i);
                        if (item != null) {
                            tempInv.setItem(i, item);
                        }
                    }

                    vaultService.saveVault(player, currentPage, tempInv).thenRun(() -> {
                        open(player, targetPage);
                    });
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Integer currentPage = openVaultPages.remove(player.getUniqueId());
        Integer maxRows = openVaultRows.remove(player.getUniqueId());

        if (currentPage == null || maxRows == null) return;

        Inventory closedInv = event.getInventory();
        int itemSlots = maxRows * 9;
        Inventory tempInv = Bukkit.createInventory(null, itemSlots);
        for (int i = 0; i < itemSlots && i < closedInv.getSize(); i++) {
            ItemStack item = closedInv.getItem(i);
            if (item != null) {
                tempInv.setItem(i, item);
            }
        }

        vaultService.saveVault(player, currentPage, tempInv);
    }
}
