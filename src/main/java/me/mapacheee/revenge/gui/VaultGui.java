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
import org.bukkit.inventory.InventoryHolder;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ListenerComponent
public class VaultGui implements Listener {

    private static class VaultHolder implements InventoryHolder {
        private final int page;
        private final int rows;
        public VaultHolder(int page, int rows) { this.page = page; this.rows = rows; }
        public int getPage() { return page; }
        public int getRows() { return rows; }
        @Override public Inventory getInventory() { return null; }
    }

    private final VaultService vaultService;
    private final Container<Messages> messages;
    private final Plugin plugin;

    private final Map<UUID, Inventory> openVaultInventories = new ConcurrentHashMap<>();
    private final Set<UUID> navigating = ConcurrentHashMap.newKeySet();

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

                Inventory inv = Bukkit.createInventory(new VaultHolder(finalPage, maxRows), totalSlots, title);

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

                openVaultInventories.put(player.getUniqueId(), inv);
                player.openInventory(inv);
            }, null);
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof VaultHolder holder)) return;

        Inventory topInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        int maxRows = holder.getRows();
        int navRowStart = maxRows * 9;

        if (clickedInv.equals(topInv)) {
            if (event.getSlot() >= navRowStart) {
                event.setCancelled(true);
                handleNavigation(player, event.getCurrentItem(), holder);
            }
        } else if (event.isShiftClick()) {
            if (isVaultFull(topInv, navRowStart)) {
                event.setCancelled(true);
            }
        }
    }


    private boolean isVaultFull(Inventory inv, int itemSlots) {
        for (int i = 0; i < itemSlots; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) return false;
        }
        return true;
    }

    private void handleNavigation(Player player, ItemStack clicked, VaultHolder holder) {
        if (clicked != null && clicked.hasItemMeta()) {
            NamespacedKey pageKey = new NamespacedKey(plugin, "vault_page");
            Integer targetPage = clicked.getItemMeta().getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
            if (targetPage != null) {
                int currentPage = holder.getPage();
                int maxRows = holder.getRows();
                Inventory vaultInv = openVaultInventories.get(player.getUniqueId());
                if (vaultInv == null) return;

                Inventory tempInv = Bukkit.createInventory(null, maxRows * 9);
                for (int i = 0; i < maxRows * 9; i++) {
                    ItemStack item = vaultInv.getItem(i);
                    if (item != null) {
                        tempInv.setItem(i, item);
                    }
                }

                navigating.add(player.getUniqueId());
                vaultService.saveVault(player, currentPage, tempInv).thenRun(() -> {
                    open(player, targetPage);
                });
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof VaultHolder holder)) return;

        UUID uuid = player.getUniqueId();
        if (navigating.remove(uuid)) return;

        openVaultInventories.remove(uuid);

        int currentPage = holder.getPage();
        int maxRows = holder.getRows();

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
