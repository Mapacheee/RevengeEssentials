package me.mapacheee.revenge.gui;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.Kit;
import me.mapacheee.revenge.service.InventorySyncService;
import me.mapacheee.revenge.service.KitService;
import me.mapacheee.revenge.service.PlayerDataService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.function.Consumer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import me.mapacheee.revenge.data.KitGuiData;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;

@ListenerComponent
public class KitGui implements Listener {

    private final KitService kitService;
    private final PlayerDataService playerDataService;
    private final KitPreviewGui kitPreviewGui;
    private final Container<Messages> messages;
    private final InventorySyncService inventorySyncService;
    private final Plugin plugin;
    private Component guiTitle;

    @Inject
    public KitGui(KitService kitService, PlayerDataService playerDataService, KitPreviewGui kitPreviewGui, Container<Messages> messages, InventorySyncService inventorySyncService, Plugin plugin) {
        this.kitService = kitService;
        this.playerDataService = playerDataService;
        this.kitPreviewGui = kitPreviewGui;
        this.messages = messages;
        this.inventorySyncService = inventorySyncService;
        this.plugin = plugin;
    }

    private static final List<Integer> AVAILABLE_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );

    public void open(Player player) {
        playerDataService.getPlayerData(player.getUniqueId(), player.getName()).thenAccept(playerData -> {
            player.getScheduler().run(plugin, task -> {
                guiTitle = MiniMessage.miniMessage().deserialize(messages.get().kitGuiTitle());
                
                List<Kit> kits = kitService.getAllKits();
                kits.sort(Comparator.comparingInt(Kit::getGuiSlot));
                
                int size = 54;
                Inventory inv = Bukkit.createInventory(null, size, guiTitle);

                KitGuiData guiData = kitService.getGuiData();

                for (Map.Entry<Integer, String> entry : guiData.getDecorationSlots().entrySet()) {
                    if (entry.getKey() >= 0 && entry.getKey() < size) {
                        try {
                            ItemStack[] items = inventorySyncService.deserializeItems(entry.getValue());
                            if (items != null && items.length > 0 && items[0] != null) {
                                inv.setItem(entry.getKey(), items[0]);
                            }
                        } catch (Exception ignored) { }
                    }
                }

                AtomicInteger autoSlotIndex = new AtomicInteger(0);
                
                List<Kit> assignedKits = new ArrayList<>();
                List<Kit> unassignedKits = new ArrayList<>();
                Set<Integer> occupiedKitSlots = new HashSet<>();

                for (Kit k : kits) {
                    if (k.getGuiSlot() >= 0 && k.getGuiSlot() < size) {
                        assignedKits.add(k);
                    } else {
                        unassignedKits.add(k);
                    }
                }

                Consumer<Kit> renderKit = (kit) -> {
                    Material mat = Material.matchMaterial(kit.getIconMaterial() != null ? kit.getIconMaterial().toUpperCase() : "CHEST");
                    if (mat == null) mat = Material.CHEST;
                    ItemStack item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();
                    
                    meta.displayName(MiniMessage.miniMessage().deserialize(kit.getDisplayName()).decoration(TextDecoration.ITALIC, false));
                    
                    List<Component> lore = new ArrayList<>();
                    if (kit.getLore() != null) {
                        for (String l : kit.getLore()) {
                            lore.add(MiniMessage.miniMessage().deserialize(l).decoration(TextDecoration.ITALIC, false));
                        }
                    }
                    
                    lore.add(Component.empty());
                    
                    int pending = playerData.getPendingKits().getOrDefault(kit.getName().toLowerCase(), 0);
                    long cdRemaining = kitService.getRemainingCooldown(player, kit, playerData);

                    if (pending > 0) {
                        lore.add(MiniMessage.miniMessage().deserialize(messages.get().kitClaimsAvailable(), Placeholder.parsed("amount", String.valueOf(pending))).decoration(TextDecoration.ITALIC, false));
                        meta.setEnchantmentGlintOverride(true);
                    } else if (cdRemaining == -1) {
                        lore.add(MiniMessage.miniMessage().deserialize(messages.get().kitNoPermission()).decoration(TextDecoration.ITALIC, false));
                    } else if (cdRemaining > 0) {
                        String time = formatTime(cdRemaining);
                        lore.add(MiniMessage.miniMessage().deserialize(messages.get().kitCooldown(), Placeholder.parsed("time", time)).decoration(TextDecoration.ITALIC, false));
                    } else {
                        lore.add(MiniMessage.miniMessage().deserialize(messages.get().kitAvailable()).decoration(TextDecoration.ITALIC, false));
                    }
                    
                    if (kit.getCost() > 0) {
                        lore.add(MiniMessage.miniMessage().deserialize(messages.get().kitCost(), Placeholder.parsed("cost", String.valueOf(kit.getCost()))).decoration(TextDecoration.ITALIC, false));
                    }

                    lore.add(Component.empty());
                    lore.add(MiniMessage.miniMessage().deserialize(messages.get().kitClickClaim()).decoration(TextDecoration.ITALIC, false));
                    lore.add(MiniMessage.miniMessage().deserialize(messages.get().kitClickPreview()).decoration(TextDecoration.ITALIC, false));
                    
                    meta.lore(lore);
                    item.setItemMeta(meta);

                    int slot = kit.getGuiSlot();
                    if (slot < 0 || slot >= size) {
                        int candidate = -1;
                        while (autoSlotIndex.get() < AVAILABLE_SLOTS.size()) {
                            int possible = AVAILABLE_SLOTS.get(autoSlotIndex.getAndIncrement());
                            if (!occupiedKitSlots.contains(possible)) {
                                candidate = possible;
                                break;
                            }
                        }
                        
                        if (candidate == -1) {
                            for (int i = 0; i < size; i++) {
                                if (!occupiedKitSlots.contains(i)) {
                                    candidate = i;
                                    break;
                                }
                            }
                        }

                        if (candidate != -1) {
                            slot = candidate;
                        }
                    }
                    
                    if (slot != -1) {
                        inv.setItem(slot, item);
                        occupiedKitSlots.add(slot);
                    }
                };

                for (Kit kit : assignedKits) {
                    renderKit.accept(kit);
                }

                for (Kit kit : unassignedKits) {
                    renderKit.accept(kit);
                }

                player.openInventory(inv);
            }, null);
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (plainTitle.equals(PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(messages.get().kitGuiTitle())))) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || meta.displayName() == null) return;

            String kitNameDisplay = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            Kit kit = kitService.getAllKits().stream()
                    .filter(k -> kitNameDisplay.contains(PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(k.getDisplayName()))))
                    .findFirst().orElse(null);

            if (kit == null) return;

            if (event.isRightClick()) {
                kitPreviewGui.open(player, kit);
            } else if (event.isLeftClick()) {
                player.closeInventory();
                
                playerDataService.getPlayerData(player.getUniqueId(), player.getName()).thenAccept(data -> {
                    long remaining = kitService.getRemainingCooldown(player, kit, data);
                    int pending = data.getPendingKits().getOrDefault(kit.getName().toLowerCase(), 0);
                    
                    player.getScheduler().run(plugin, task -> {
                        if (pending > 0 || remaining == 0) {
                            kitService.processKitClaim(player, kit, data, inventorySyncService);
                            playerDataService.savePlayerData(data);
                            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitClaimedSuccessfully(), Placeholder.parsed("kit", kit.getName())));
                        } else if (remaining == -1) {
                            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitClaimNoPermission()));
                        } else {
                            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitClaimCooldown(), Placeholder.parsed("time", formatTime(remaining))));
                        }
                    }, null);
                });
            }
        }
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
