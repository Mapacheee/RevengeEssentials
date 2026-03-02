package me.mapacheee.revenge.gui;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.AuctionItem;
import me.mapacheee.revenge.service.AuctionService;
import me.mapacheee.revenge.service.InventorySyncService;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ListenerComponent
public class AuctionGui implements Listener {

    private static class AuctionHolder implements org.bukkit.inventory.InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final AuctionService auctionService;
    private final InventorySyncService inventorySyncService;
    private final Container<Messages> messages;
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
    public AuctionGui(AuctionService auctionService, InventorySyncService inventorySyncService, Container<Messages> messages, Plugin plugin) {
        this.auctionService = auctionService;
        this.inventorySyncService = inventorySyncService;
        this.messages = messages;
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        player.getScheduler().run(plugin, task -> {
            Component title = MiniMessage.miniMessage().deserialize(
                    messages.get().ahGuiTitle(),
                    Placeholder.parsed("page", String.valueOf(page + 1))
            );
            Inventory inv = Bukkit.createInventory(new AuctionHolder(), 54, title);

            ItemStack fillDef = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fillMeta = fillDef.getItemMeta();
            fillMeta.displayName(Component.empty());
            fillDef.setItemMeta(fillMeta);
            
            for (int i = 0; i < 54; i++) {
                if (!AVAILABLE_SLOTS.contains(i)) {
                    inv.setItem(i, fillDef);
                }
            }

            List<AuctionItem> items = new ArrayList<>(auctionService.getActiveAuctions());
            items.sort(Comparator.comparingLong(AuctionItem::getDateAdded).reversed());

            int maxPages = (int) Math.ceil((double) items.size() / AVAILABLE_SLOTS.size());
            if (maxPages == 0) maxPages = 1;

            int actualPage = page;
            if (actualPage < 0) actualPage = 0;
            if (actualPage >= maxPages) actualPage = maxPages - 1;

            int startIndex = actualPage * AVAILABLE_SLOTS.size();
            int endIndex = Math.min(startIndex + AVAILABLE_SLOTS.size(), items.size());

            AtomicInteger index = new AtomicInteger(0);

            for (int i = startIndex; i < endIndex; i++) {
                AuctionItem auctionItem = items.get(i);
                
                ItemStack[] des = inventorySyncService.deserializeItems(auctionItem.getItemBase64());
                if (des == null || des.length == 0 || des[0] == null) continue;
                
                ItemStack item = des[0].clone();
                ItemMeta meta = item.getItemMeta();
                if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(item.getType());

                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();
                
                lore.add(Component.empty());
                lore.add(MiniMessage.miniMessage().deserialize(messages.get().ahItemSeller(), Placeholder.parsed("seller", auctionItem.getSellerName())).decoration(TextDecoration.ITALIC, false));
                lore.add(MiniMessage.miniMessage().deserialize(messages.get().ahItemPrice(), Placeholder.parsed("price", String.valueOf(auctionItem.getPrice()))).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                
                if (player.getUniqueId().equals(auctionItem.getSellerUuid())) {
                    lore.add(MiniMessage.miniMessage().deserialize(messages.get().ahItemClickWithdraw()).decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(MiniMessage.miniMessage().deserialize(messages.get().ahItemClickBuy()).decoration(TextDecoration.ITALIC, false));
                }

                meta.lore(lore);

                NamespacedKey key = new NamespacedKey(plugin, "ah_item_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, auctionItem.id().toHexString());
                item.setItemMeta(meta);

                int slot = AVAILABLE_SLOTS.get(index.getAndIncrement());
                inv.setItem(slot, item);
            }

            if (actualPage > 0) {
                ItemStack prevBtn = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevBtn.getItemMeta();
                prevMeta.displayName(MiniMessage.miniMessage().deserialize(messages.get().ahPrevPage()).decoration(TextDecoration.ITALIC, false));
                NamespacedKey pageKey = new NamespacedKey(plugin, "ah_page");
                prevMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, actualPage - 1);
                prevBtn.setItemMeta(prevMeta);
                inv.setItem(PREV_PAGE_SLOT, prevBtn);
            }
            if (actualPage < maxPages - 1) {
                ItemStack nextBtn = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextBtn.getItemMeta();
                nextMeta.displayName(MiniMessage.miniMessage().deserialize(messages.get().ahNextPage()).decoration(TextDecoration.ITALIC, false));
                NamespacedKey pageKey = new NamespacedKey(plugin, "ah_page");
                nextMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, actualPage + 1);
                nextBtn.setItemMeta(nextMeta);
                inv.setItem(NEXT_PAGE_SLOT, nextBtn);
            }

            player.openInventory(inv);
        }, null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AuctionHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        NamespacedKey pageKey = new NamespacedKey(plugin, "ah_page");
        if (clicked.getItemMeta().getPersistentDataContainer().has(pageKey, PersistentDataType.INTEGER)) {
            int targetPage = clicked.getItemMeta().getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
            open(player, targetPage);
            return;
        }

        NamespacedKey itemKey = new NamespacedKey(plugin, "ah_item_id");
        if (clicked.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.STRING)) {
            String auctionId = clicked.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
            
            auctionService.getAuction(auctionId).ifPresent(auction -> {
                player.closeInventory();
                if (auction.getSellerUuid().equals(player.getUniqueId())) {
                    auctionService.cancelListing(player, auctionId).thenAccept(success -> {
                        if (success) {
                            ItemStack[] des = inventorySyncService.deserializeItems(auction.getItemBase64());
                            if (des != null && des.length > 0 && des[0] != null) {
                                player.getInventory().addItem(des[0]);
                            }
                            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().ahWithdrawn()));
                        }
                    });
                } else {
                    auctionService.buyItem(player, auctionId).thenAccept(success -> {
                        if (success) {
                            ItemStack[] des = inventorySyncService.deserializeItems(auction.getItemBase64());
                            if (des != null && des.length > 0 && des[0] != null) {
                                player.getInventory().addItem(des[0]);
                            }
                            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().ahPurchaseSuccess(), Placeholder.parsed("price", String.valueOf(auction.getPrice()))));
                        } else {
                            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().ahPurchaseFail()));
                        }
                    });
                }
            });
        }
    }
}
