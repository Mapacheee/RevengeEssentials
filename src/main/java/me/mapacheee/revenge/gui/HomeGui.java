package me.mapacheee.revenge.gui;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.HomeData;
import me.mapacheee.revenge.service.HomeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HomeGui implements Listener {

    private final HomeService homeService;
    private final Container<Messages> messages;
    private final Plugin plugin;
    private final Map<UUID, List<HomeData>> viewerHomes = new ConcurrentHashMap<>();
    private Component guiTitle;

    @Inject
    public HomeGui(HomeService homeService, Container<Messages> messages, Plugin plugin) {
        this.homeService = homeService;
        this.messages = messages;
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        homeService.getHomes(player.getUniqueId().toString()).thenAccept(homes -> {
            player.getScheduler().run(plugin, task -> {
                guiTitle = MiniMessage.miniMessage().deserialize(messages.get().homeGuiTitle());
                int size = homes.size() <= 9 ? 27 : homes.size() <= 18 ? 36 : 54;
                Inventory inv = Bukkit.createInventory(null, size, guiTitle);

                fillBorders(inv, size);

                List<HomeData> homeList = new ArrayList<>(homes);
                viewerHomes.put(player.getUniqueId(), homeList);

                int slot = 10;
                for (HomeData home : homeList) {
                    if (slot >= size - 9)
                        break;
                    if (slot % 9 == 0)
                        slot++;
                    if (slot % 9 == 8)
                        slot += 2;

                    ItemStack item = new ItemStack(Material.ENDER_PEARL);
                    ItemMeta meta = item.getItemMeta();
                    meta.displayName(Component.text(home.getName())
                            .color(TextColor.fromHexString("#F2AE2E"))
                            .decoration(TextDecoration.ITALIC, false));

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.empty());
                    lore.add(Component.text("Servidor: ").color(TextColor.fromHexString("#8A8A8A"))
                            .append(Component.text(home.getServer()).color(TextColor.fromHexString("#F27B35")))
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Mundo: ").color(TextColor.fromHexString("#8A8A8A"))
                            .append(Component.text(home.getWorld()).color(TextColor.fromHexString("#F27B35")))
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component
                            .text("X: " + (int) home.getX() + " Y: " + (int) home.getY() + " Z: " + (int) home.getZ())
                            .color(TextColor.fromHexString("#F28B30"))
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.empty());
                    lore.add(Component.text("▸ Click Izquierdo para teletransportarte")
                            .color(TextColor.fromHexString("#F2AE2E"))
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("▸ Click Derecho para eliminar").color(TextColor.fromHexString("#BF2A45"))
                            .decoration(TextDecoration.ITALIC, false));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                    inv.setItem(slot, item);
                    slot++;
                }

                if (homes.isEmpty()) {
                    ItemStack noHomes = new ItemStack(Material.BARRIER);
                    ItemMeta meta = noHomes.getItemMeta();
                    meta.displayName(MiniMessage.miniMessage().deserialize(messages.get().homeGuiNoHomes())
                            .decoration(TextDecoration.ITALIC, false));
                    noHomes.setItemMeta(meta);
                    inv.setItem(size / 2, noHomes);
                }

                player.openInventory(inv);
            }, null);
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (event.getView().title().equals(guiTitle) && guiTitle != null) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR
                    || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE || clicked.getType() == Material.BARRIER)
                return;

            List<HomeData> homes = viewerHomes.get(player.getUniqueId());
            if (homes == null)
                return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || meta.displayName() == null)
                return;

            String homeName = MiniMessage.miniMessage().serialize(meta.displayName());
            HomeData home = homes.stream()
                    .filter(h -> homeName.contains(h.getName()))
                    .findFirst().orElse(null);

            if (home == null)
                return;

            if (event.isRightClick()) {
                homeService.deleteHome(player.getUniqueId().toString(), home.getName()).thenRun(() -> {
                    player.getScheduler().run(plugin, task -> {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(
                                messages.get().homeDeleted(),
                                Placeholder.unparsed("home",
                                        home.getName())));
                        player.closeInventory();
                        open(player);
                    }, null);
                });
            } else {
                player.closeInventory();
                homeService.teleportToHome(player, home);
            }
        }
    }

    private void fillBorders(Inventory inv, int size) {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);

        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = size - 9; i < size; i++)
            inv.setItem(i, border);
        for (int i = 9; i < size - 9; i += 9)
            inv.setItem(i, border);
        for (int i = 17; i < size - 9; i += 9)
            inv.setItem(i, border);
    }
}
