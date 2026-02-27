package me.mapacheee.revenge.gui;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.revenge.data.Kit;
import me.mapacheee.revenge.service.InventorySyncService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@ListenerComponent
public class KitPreviewGui implements Listener {

    private final InventorySyncService inventorySyncService;
    private Component guiTitle;

    @Inject
    public KitPreviewGui(InventorySyncService inventorySyncService) {
        this.inventorySyncService = inventorySyncService;
    }

    public void open(Player player, Kit kit) {
        guiTitle = MiniMessage.miniMessage().deserialize("<gradient:#F2AE2E:#F2CF66>Preview: " + kit.getName() + "</gradient>");
        Inventory inv = Bukkit.createInventory(null, 54, guiTitle);

        ItemStack[] items = inventorySyncService.deserializeItems(kit.getInventoryBase64());
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) {
                    inv.addItem(item);
                }
            }
        }
        
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta meta = backButton.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<red>Volver a Kits").decoration(TextDecoration.ITALIC, false));
        backButton.setItemMeta(meta);
        inv.setItem(49, backButton);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title().equals(guiTitle) && guiTitle != null) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.ARROW) {
                player.closeInventory();
                player.performCommand("kits");
            }
        }
    }
}
