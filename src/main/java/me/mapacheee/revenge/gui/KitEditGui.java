package me.mapacheee.revenge.gui;

import com.google.inject.Inject;
import me.mapacheee.revenge.data.KitGuiData;
import com.thewinterframework.paper.listener.ListenerComponent;
import com.thewinterframework.configurate.Container;
import java.util.function.Function;
import me.mapacheee.revenge.config.Messages;
import org.bukkit.inventory.ItemFlag;
import java.util.HashSet;
import java.util.Set;
import me.mapacheee.revenge.data.Kit;
import org.bukkit.event.inventory.InventoryCloseEvent;
import me.mapacheee.revenge.service.InventorySyncService;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.mapacheee.revenge.service.KitService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ListenerComponent
public class KitEditGui implements Listener {

    private final KitService kitService;
    private final InventorySyncService inventorySyncService;
    private final Container<Messages> messages;
    private final Plugin plugin;
    private Component guiTitle;
    
    private final Map<UUID, EditAction> pendingActions = new ConcurrentHashMap<>();

    private static class EditAction {
        Kit kit;
        String type;
        public EditAction(Kit kit, String type) { this.kit = kit; this.type = type; }
    }

    @Inject
    public KitEditGui(KitService kitService, InventorySyncService inventorySyncService, Container<Messages> messages, Plugin plugin) {
        this.kitService = kitService;
        this.inventorySyncService = inventorySyncService;
        this.messages = messages;
        this.plugin = plugin;
    }

    private static final List<Integer> AVAILABLE_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );

    public void open(Player player, Kit kit) {
        guiTitle = MiniMessage.miniMessage().deserialize(messages.get().kitEditGuiEditorPrefix() + kit.getName());
        Inventory inv = Bukkit.createInventory(null, 45, guiTitle);

        inv.setItem(11, makeItem(Material.NAME_TAG, "<yellow>Renombrar DisplayName", "<gray>Click para escribir en el chat el nuevo\n<gray>nombre visual con MiniMessage."));
        inv.setItem(13, makeItem(Material.WRITABLE_BOOK, "<aqua>Añadir línea al Lore", "<gray>Añade una descripción debajo."));
        inv.setItem(15, makeItem(Material.BARRIER, "<red>Borrar Lore", "<gray>Limpia todas las líneas del lore."));

        String matName = kit.getIconMaterial() != null ? kit.getIconMaterial() : "CHEST";
        inv.setItem(22, makeItem(Material.ITEM_FRAME, "<light_purple>Cambiar Icono", "<gray>Actual: <yellow>" + matName + "\n<gray>Asegurate de sostener un bloque/ítem\n<gray>en tu mano y da click aquí\n<gray>para transferirle el ícono al kit."));

        inv.setItem(29, makeItem(Material.GOLD_INGOT, "<gold>Editar Precio", "<gray>Actual: <yellow>$" + kit.getCost()));
        inv.setItem(31, makeItem(Material.CLOCK, "<blue>Editar Cooldown", "<gray>Actual: <aqua>" + kit.getCooldownSeconds() + " seg"));
        inv.setItem(33, makeItem(Material.CHEST, "<green>Actualizar Ítems", "<gray>Sobrescribe el kit con tu inventario actual\n<gray>incluyendo armaduras."));

        inv.setItem(40, makeItem(Material.LAVA_BUCKET, "<red>Borrar Kit", "<gray>Click para eliminar definitivamente\n<gray>este kit del servidor."));

        player.openInventory(inv);
    }

    public void openList(Player player) {
        guiTitle = MiniMessage.miniMessage().deserialize(messages.get().kitEditGuiListTitle());
        Inventory inv = Bukkit.createInventory(null, 54, guiTitle);

        int autoSlotIndex = 0;

        for (Kit kit : kitService.getAllKits()) {
            Material mat = Material.matchMaterial(kit.getIconMaterial() != null ? kit.getIconMaterial().toUpperCase() : "CHEST");
            if (mat == null) mat = Material.CHEST;
            
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(MiniMessage.miniMessage().deserialize(kit.getDisplayName()).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<gray>ID: " + kit.getName()).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(MiniMessage.miniMessage().deserialize("<yellow>Click para abrir el Editor").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
            
            int slot = kit.getGuiSlot();
            if (slot < 0 || slot >= 54) {
                if (autoSlotIndex < AVAILABLE_SLOTS.size()) {
                    slot = AVAILABLE_SLOTS.get(autoSlotIndex++);
                } else {
                    slot = inv.firstEmpty();
                }
            }
            if (slot != -1) inv.setItem(slot, item);
        }

        inv.setItem(49, makeItem(Material.ENDER_EYE, messages.get().kitEditGuiLayoutIconName(), messages.get().kitEditGuiLayoutIconLore()));

        player.openInventory(inv);
    }

    public void openLayoutEditor(Player player) {
        guiTitle = MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditGuiLayoutTitle());
        Inventory inv = Bukkit.createInventory(null, 54, guiTitle);

        KitGuiData guiData = kitService.getGuiData();
        Set<Integer> occupiedSlots = new HashSet<>();

        for (Map.Entry<Integer, String> entry : guiData.getDecorationSlots().entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < 54) {
                try {
                    ItemStack[] items = inventorySyncService.deserializeItems(entry.getValue());
                    if (items != null && items.length > 0 && items[0] != null) {
                        inv.setItem(entry.getKey(), items[0]);
                        occupiedSlots.add(entry.getKey());
                    }
                } catch (Exception ignored) { }
            }
        }

        Function<Kit, ItemStack> makeLayoutKitItem = (kit) -> {
            Material mat = Material.matchMaterial(kit.getIconMaterial() != null ? kit.getIconMaterial().toUpperCase() : "CHEST");
            if (mat == null) mat = Material.CHEST;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(MiniMessage.miniMessage().deserialize(kit.getDisplayName()).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<gray>ID: " + kit.getName()).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        };

        List<Kit> unplacedKits = new ArrayList<>();

        for (Kit kit : kitService.getAllKits()) {
            int slot = kit.getGuiSlot();
            if (slot >= 0 && slot < 54) {
                inv.setItem(slot, makeLayoutKitItem.apply(kit));
                occupiedSlots.add(slot);
            } else {
                unplacedKits.add(kit);
            }
        }

        int emptyIndex = 0;
        for (Kit unplaced : unplacedKits) {
            while (emptyIndex < 54 && occupiedSlots.contains(emptyIndex)) {
                emptyIndex++;
            }
            if (emptyIndex < 54) {
                inv.setItem(emptyIndex, makeLayoutKitItem.apply(unplaced));
                occupiedSlots.add(emptyIndex);
                emptyIndex++;
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String plainListTitle = PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(messages.get().kitEditGuiListTitle()));
        String plainEditorPrefix = PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(messages.get().kitEditGuiEditorPrefix()));
        
        if (plainTitle.equals(plainListTitle)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (clicked.getType() == Material.ENDER_EYE) {
                openLayoutEditor(player);
                return;
            }

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || meta.displayName() == null) return;

            String kitNameDisplay = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            Kit targetKit = kitService.getAllKits().stream()
                .filter(k -> kitNameDisplay.contains(PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(k.getDisplayName()))))
                .findFirst().orElse(null);

            if (targetKit != null) {
                open(player, targetKit);
            }
            return;
        }

        if (plainTitle.startsWith(plainEditorPrefix)) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String kitName = plainTitle.substring(plainEditorPrefix.length()).trim();
            Kit kit = kitService.getKit(kitName);
            if (kit == null) return;

            Material mat = clicked.getType();
            if (mat == Material.NAME_TAG) {
                pendingActions.put(player.getUniqueId(), new EditAction(kit, "displayName"));
                player.closeInventory();
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditPromptDisplayName()));
            } else if (mat == Material.WRITABLE_BOOK) {
                pendingActions.put(player.getUniqueId(), new EditAction(kit, "addLore"));
                player.closeInventory();
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditPromptLore()));
            } else if (mat == Material.BARRIER) {
                kit.setLore(new ArrayList<>());
                kitService.saveKit(kit);
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditLoreCleared()));
                open(player, kit);
            } else if (mat == Material.GOLD_INGOT) {
                pendingActions.put(player.getUniqueId(), new EditAction(kit, "cost"));
                player.closeInventory();
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditPromptCost()));
            } else if (mat == Material.CLOCK) {
                pendingActions.put(player.getUniqueId(), new EditAction(kit, "cooldown"));
                player.closeInventory();
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditPromptCooldown()));
            } else if (mat == Material.ITEM_FRAME) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditIconError()));
                } else {
                    kit.setIconMaterial(hand.getType().name());
                    kitService.saveKit(kit);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditIconUpdated(), net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("material", hand.getType().name())));
                    open(player, kit);
                }
            } else if (mat == Material.CHEST) {
                kit.setInventoryBase64(inventorySyncService.serializeItems(player.getInventory().getContents()));
                kitService.saveKit(kit);
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditItemsUpdated()));
                player.closeInventory();
            } else if (mat == Material.LAVA_BUCKET && event.getRawSlot() == 40) {
                kitService.deleteKit(kit.getName());
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitDeleted()));
                openList(player);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        EditAction action = pendingActions.remove(player.getUniqueId());
        if (action == null) return;

        event.setCancelled(true);
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        if (msg.equalsIgnoreCase("cancelar")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditCancelled()));
            player.getScheduler().run(plugin, task -> open(player, action.kit), null);
            return;
        }

        Kit kit = action.kit;
        try {
            switch (action.type) {
                case "displayName":
                    kit.setDisplayName(msg);
                    break;
                case "addLore":
                    List<String> lore = kit.getLore();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add(msg);
                    kit.setLore(lore);
                    break;
                case "cost":
                    kit.setCost(Double.parseDouble(msg));
                    break;
                case "cooldown":
                    kit.setCooldownSeconds(Long.parseLong(msg));
                    break;
            }
            kitService.saveKit(kit);
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditSaved()));
        } catch (NumberFormatException e) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitEditInvalidNumber()));
        }

        player.getScheduler().run(plugin, task -> open(player, kit), null);
    }

    private ItemStack makeItem(Material type, String name, String loreLines) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));
        List<Component> cLore = new ArrayList<>();
        for (String ln : loreLines.split("\n")) {
            cLore.add(MiniMessage.miniMessage().deserialize(ln).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(cLore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        String layoutTitleStr = messages.get().kitEditGuiLayoutTitle();
        if (layoutTitleStr == null) {
            layoutTitleStr = "<gradient:#BF2A45:#E82A5D>Editor de Plantilla</gradient>";
        }

        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String plainLayoutTitle = PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(layoutTitleStr));
        
        if (!plainTitle.equals(plainLayoutTitle)) return;

        Inventory inv = event.getInventory();
        KitGuiData guiData = kitService.getGuiData();
        
        guiData.getKitSlots().clear();
        guiData.getDecorationSlots().clear();

        for (int i = 0; i < 54; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            ItemMeta meta = item.getItemMeta();
            boolean isKit = false;
            
            if (meta != null && meta.hasLore()) {
                List<Component> lore = meta.lore();
                if (lore != null) {
                    for (Component comp : lore) {
                        String plainLore = PlainTextComponentSerializer.plainText().serialize(comp);
                        if (plainLore.startsWith("ID: ")) {
                            guiData.getKitSlots().put(i, plainLore.substring(4).trim());
                            isKit = true;
                            break;
                        }
                    }
                }
            }

            if (!isKit) {
                if (meta != null) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                    item.setItemMeta(meta);
                }
                guiData.getDecorationSlots().put(i, inventorySyncService.serializeItems(new ItemStack[]{item}));
            }
        }

        for (Kit kit : kitService.getAllKits()) {
            boolean found = false;
            for (Map.Entry<Integer, String> entry : guiData.getKitSlots().entrySet()) {
                if (entry.getValue().equalsIgnoreCase(kit.getName())) {
                    kit.setGuiSlot(entry.getKey());
                    found = true;
                    break;
                }
            }
            if (!found) {
                kit.setGuiSlot(-1);
            }
            kitService.saveKit(kit);
        }

        kitService.saveGuiData();
        event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitGuiLayoutSaved()));
    }
}
