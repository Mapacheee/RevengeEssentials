package me.mapacheee.revenge.gui;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.Warp;
import me.mapacheee.revenge.data.WarpGuiData;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.service.InventorySyncService;
import me.mapacheee.revenge.service.WarpService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ListenerComponent
public class WarpEditGui implements Listener {

    private final WarpService warpService;
    private final Container<Messages> messages;
    private final InventorySyncService inventorySyncService;
    private final Plugin plugin;
    
    private final Map<UUID, EditAction> pendingActions = new ConcurrentHashMap<>();
    private final Set<UUID> editingLayouts = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int LAYOUT_EDIT_SLOT = 49;

    private static final List<Integer> AVAILABLE_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );

    private record EditAction(Warp warp, String type) {}

    @Inject
    public WarpEditGui(WarpService warpService, Container<Messages> messages, InventorySyncService inventorySyncService, Plugin plugin) {
        this.warpService = warpService;
        this.messages = messages;
        this.inventorySyncService = inventorySyncService;
        this.plugin = plugin;
    }

    public void openList(Player player, int page) {
        player.getScheduler().run(plugin, task -> {
            String tMsg = messages.get().warpEditListTitle() != null ? messages.get().warpEditListTitle() : "<dark_red><bold>Editor Warps</bold> <gray>- Pág <page>";
            Component title = MiniMessage.miniMessage().deserialize(tMsg, Placeholder.parsed("page", String.valueOf(page + 1)));
            Inventory inv = Bukkit.createInventory(null, 54, title);

            List<Warp> warps = new ArrayList<>(warpService.getWarps());
            warps.sort(Comparator.comparing(Warp::getName));

            int maxPages = (int) Math.ceil((double) warps.size() / AVAILABLE_SLOTS.size());
            if (maxPages == 0) maxPages = 1;

            int actualPage = page;
            if (actualPage < 0) actualPage = 0;
            if (actualPage >= maxPages) actualPage = maxPages - 1;

            int startIndex = actualPage * AVAILABLE_SLOTS.size();
            int endIndex = Math.min(startIndex + AVAILABLE_SLOTS.size(), warps.size());

            AtomicInteger index = new AtomicInteger(0);
            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
            NamespacedKey warpKey = new NamespacedKey(plugin, "warp_target");

            for (int i = startIndex; i < endIndex; i++) {
                Warp warp = warps.get(i);
                Material mat = Material.matchMaterial(warp.getIconMaterial() != null ? warp.getIconMaterial().toUpperCase() : "ENDER_PEARL");
                if (mat == null) mat = Material.ENDER_PEARL;
                
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(MiniMessage.miniMessage().deserialize(warp.getDisplayName()).decoration(TextDecoration.ITALIC, false));
                
                List<Component> lore = new ArrayList<>();
                lore.add(MiniMessage.miniMessage().deserialize("<gray>ID: " + warp.getName()).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                String clickToEditMsg = messages.get().warpClickTeleport() != null ? messages.get().warpClickTeleport() : "<yellow>Click para editar";
                lore.add(MiniMessage.miniMessage().deserialize(clickToEditMsg).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                
                meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_list");
                meta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
                item.setItemMeta(meta);
                
                int slot = AVAILABLE_SLOTS.get(index.getAndIncrement());
                inv.setItem(slot, item);
            }

            if (actualPage > 0) {
                ItemStack prevBtn = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevBtn.getItemMeta();
                String pMsg = messages.get().warpEditPrevPage() != null ? messages.get().warpEditPrevPage() : "<red><bold>⮜ Página Anterior";
                prevMeta.displayName(MiniMessage.miniMessage().deserialize(pMsg).decoration(TextDecoration.ITALIC, false));
                NamespacedKey pageKey = new NamespacedKey(plugin, "list_page");
                prevMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, actualPage - 1);
                prevMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_list");
                prevBtn.setItemMeta(prevMeta);
                inv.setItem(PREV_PAGE_SLOT, prevBtn);
            }

            if (actualPage < maxPages - 1) {
                ItemStack nextBtn = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextBtn.getItemMeta();
                String nMsg = messages.get().warpEditNextPage() != null ? messages.get().warpEditNextPage() : "<red><bold>Página Siguiente ⮞";
                nextMeta.displayName(MiniMessage.miniMessage().deserialize(nMsg).decoration(TextDecoration.ITALIC, false));
                NamespacedKey pageKey = new NamespacedKey(plugin, "list_page");
                nextMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, actualPage + 1);
                nextMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_list");
                nextBtn.setItemMeta(nextMeta);
                inv.setItem(NEXT_PAGE_SLOT, nextBtn);
            }

            ItemStack layoutBtn = new ItemStack(Material.ENDER_EYE);
            ItemMeta layoutMeta = layoutBtn.getItemMeta();
            String lMsg = messages.get().kitEditGuiLayoutIconName() != null ? messages.get().kitEditGuiLayoutIconName() : "<light_purple>Editar Diseño";
            layoutMeta.displayName(MiniMessage.miniMessage().deserialize(lMsg).decoration(TextDecoration.ITALIC, false));
            layoutMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_layout_btn");
            layoutBtn.setItemMeta(layoutMeta);
            inv.setItem(LAYOUT_EDIT_SLOT, layoutBtn);

            player.openInventory(inv);
        }, null);
    }

    public void openEditor(Player player, Warp warp) {
        player.getScheduler().run(plugin, task -> {
            String eMsg = messages.get().warpEditEditorTitle() != null ? messages.get().warpEditEditorTitle() : "<dark_red><bold>Editar:</bold> <gray><warp>";
            Component title = MiniMessage.miniMessage().deserialize(eMsg, Placeholder.parsed("warp", warp.getName()));
            Inventory inv = Bukkit.createInventory(null, 54, title);

            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
            NamespacedKey actionKey = new NamespacedKey(plugin, "edit_action");
            NamespacedKey warpKey = new NamespacedKey(plugin, "warp_target");

            ItemStack nameBtn = new ItemStack(Material.NAME_TAG);
            ItemMeta nameMeta = nameBtn.getItemMeta();
            String nameMsg = messages.get().warpEditBtnName() != null ? messages.get().warpEditBtnName() : "<yellow>Cambiar Nombre Visible";
            nameMeta.displayName(MiniMessage.miniMessage().deserialize(nameMsg).decoration(TextDecoration.ITALIC, false));
            nameMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_action");
            nameMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "name");
            nameMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            nameBtn.setItemMeta(nameMeta);
            inv.setItem(11, nameBtn);

            ItemStack loreBtn = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta loreMeta = loreBtn.getItemMeta();
            String loreMsg = messages.get().warpEditBtnLore() != null ? messages.get().warpEditBtnLore() : "<yellow>Añadir línea de Descripción";
            loreMeta.displayName(MiniMessage.miniMessage().deserialize(loreMsg).decoration(TextDecoration.ITALIC, false));
            loreMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_action");
            loreMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "lore");
            loreMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            loreBtn.setItemMeta(loreMeta);
            inv.setItem(13, loreBtn);

            ItemStack clearBtn = new ItemStack(Material.BARRIER);
            ItemMeta clearMeta = clearBtn.getItemMeta();
            String clearMsg = messages.get().warpEditBtnClearLore() != null ? messages.get().warpEditBtnClearLore() : "<red>Limpiar Descripción";
            clearMeta.displayName(MiniMessage.miniMessage().deserialize(clearMsg).decoration(TextDecoration.ITALIC, false));
            clearMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_action");
            clearMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "clear_lore");
            clearMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            clearBtn.setItemMeta(clearMeta);
            inv.setItem(15, clearBtn);

            ItemStack iconBtn = new ItemStack(Material.ITEM_FRAME);
            ItemMeta iconMeta = iconBtn.getItemMeta();
            String iconMsg = messages.get().warpEditBtnIcon() != null ? messages.get().warpEditBtnIcon() : "<light_purple>Cambiar Ícono";
            String iconLoreMsg = messages.get().warpEditBtnIconLore() != null ? messages.get().warpEditBtnIconLore() : "<gray>Usa el ítem en tu mano principal.";
            iconMeta.displayName(MiniMessage.miniMessage().deserialize(iconMsg).decoration(TextDecoration.ITALIC, false));
            iconMeta.lore(List.of(MiniMessage.miniMessage().deserialize(iconLoreMsg).decoration(TextDecoration.ITALIC, false)));
            iconMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_action");
            iconMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "icon");
            iconMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            iconBtn.setItemMeta(iconMeta);
            inv.setItem(29, iconBtn);

            ItemStack locBtn = new ItemStack(Material.ENDER_PEARL);
            ItemMeta locMeta = locBtn.getItemMeta();
            String locMsg = messages.get().warpEditBtnLocation() != null ? messages.get().warpEditBtnLocation() : "<aqua>Actualizar Ubicación";
            String locLoreMsg = messages.get().warpEditBtnLocationLore() != null ? messages.get().warpEditBtnLocationLore() : "<gray>Actualiza el warp a tu posición actual.";
            locMeta.displayName(MiniMessage.miniMessage().deserialize(locMsg).decoration(TextDecoration.ITALIC, false));
            locMeta.lore(List.of(MiniMessage.miniMessage().deserialize(locLoreMsg).decoration(TextDecoration.ITALIC, false)));
            locMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_action");
            locMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "location");
            locMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            locBtn.setItemMeta(locMeta);
            inv.setItem(31, locBtn);

            ItemStack delBtn = new ItemStack(Material.LAVA_BUCKET);
            ItemMeta delMeta = delBtn.getItemMeta();
            String delMsg = messages.get().warpEditBtnDelete() != null ? messages.get().warpEditBtnDelete() : "<dark_red><bold>Borrar Warp";
            delMeta.displayName(MiniMessage.miniMessage().deserialize(delMsg).decoration(TextDecoration.ITALIC, false));
            delMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_action");
            delMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "delete");
            delMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            delBtn.setItemMeta(delMeta);
            inv.setItem(33, delBtn);

            player.openInventory(inv);
        }, null);
    }

    public void openLayoutEditor(Player player) {
        player.getScheduler().run(plugin, task -> {
            editingLayouts.add(player.getUniqueId());
            String lTitle = messages.get().warpEditLayoutTitle() != null ? messages.get().warpEditLayoutTitle() : "<light_purple>Editor de Diseño: Warps";
            Component title = MiniMessage.miniMessage().deserialize(lTitle);
            Inventory inv = Bukkit.createInventory(null, 54, title);

            WarpGuiData guiData = warpService.getGuiData();
            if (guiData != null && guiData.getDecorativeItems() != null) {
                for (Map.Entry<Integer, String> entry : guiData.getDecorativeItems().entrySet()) {
                    if (entry.getKey() >= 0 && entry.getKey() < 54) {
                        try {
                            ItemStack[] items = inventorySyncService.deserializeItems(entry.getValue());
                            if (items != null && items.length > 0 && items[0] != null) {
                                boolean skip = false;
                                if (items[0].hasItemMeta()) {
                                    PersistentDataContainer pdc = items[0].getItemMeta().getPersistentDataContainer();
                                    NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
                                    if (pdc.has(typeKey, PersistentDataType.STRING)) skip = true;
                                    for (NamespacedKey k : pdc.getKeys()) {
                                        if (k.getKey().contains("warp") || k.getKey().contains("page") || k.getKey().contains("list") || k.getKey().contains("target")) skip = true;
                                    }
                                }
                                if (skip) continue;
                                inv.setItem(entry.getKey(), items[0]);
                            }
                        } catch (Exception ignored) { }
                    }
                }
            }
            
            List<Warp> warps = new ArrayList<>(warpService.getWarps());
            warps.sort(Comparator.comparing(Warp::getName));

            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
            AtomicInteger index = new AtomicInteger(0);

            for (int i = 0; i < Math.min(AVAILABLE_SLOTS.size(), warps.size()); i++) {
                Warp warp = warps.get(i);
                
                Material mat = Material.matchMaterial(warp.getIconMaterial() != null ? warp.getIconMaterial().toUpperCase() : "ENDER_PEARL");
                if (mat == null) mat = Material.ENDER_PEARL;
                
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                
                meta.displayName(MiniMessage.miniMessage().deserialize(warp.getDisplayName()).decoration(TextDecoration.ITALIC, false));
                meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_layout_dummy");
                item.setItemMeta(meta);
                
                int slot = AVAILABLE_SLOTS.get(index.getAndIncrement());
                inv.setItem(slot, item);
            }

            int maxPages = (int) Math.ceil((double) warps.size() / AVAILABLE_SLOTS.size());
            if (maxPages > 1) {
                ItemStack nextBtn = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextBtn.getItemMeta();
                String nMsg = messages.get().warpEditNextPage() != null ? messages.get().warpEditNextPage() : "<red><bold>Página Siguiente ⮞";
                nextMeta.displayName(MiniMessage.miniMessage().deserialize(nMsg).decoration(TextDecoration.ITALIC, false));
                nextMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "warp_edit_layout_dummy");
                nextBtn.setItemMeta(nextMeta);
                inv.setItem(NEXT_PAGE_SLOT, nextBtn);
            }

            player.openInventory(inv);
        }, null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory topInv = event.getView().getTopInventory();
        
        if (editingLayouts.contains(player.getUniqueId())) {
            String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
            if (title.contains("Diseño")) {
                return;
            }
        }

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInv)) {
            boolean isWarpEdit = false;
            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
            for (ItemStack it : topInv.getContents()) {
                if (it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                    String type = it.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
                    if (type != null && type.startsWith("warp_edit_")) {
                        isWarpEdit = true;
                        break;
                    }
                }
            }
            if (isWarpEdit) event.setCancelled(true);
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            boolean isWarpEdit = false;
            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
            for (ItemStack it : topInv.getContents()) {
                if (it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                    String type = it.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
                    if (type != null && type.startsWith("warp_edit_")) {
                        isWarpEdit = true;
                        break;
                    }
                }
            }
            if (isWarpEdit) event.setCancelled(true);
            return;
        }

        NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
        PersistentDataContainer container = clicked.getItemMeta().getPersistentDataContainer();

        if (container.has(typeKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            String type = container.get(typeKey, PersistentDataType.STRING);
            
            if ("warp_edit_list".equals(type)) {
                NamespacedKey pageKey = new NamespacedKey(plugin, "list_page");
                NamespacedKey warpKey = new NamespacedKey(plugin, "warp_target");
                
                if (container.has(pageKey, PersistentDataType.INTEGER)) {
                    openList(player, container.get(pageKey, PersistentDataType.INTEGER));
                } else if (container.has(warpKey, PersistentDataType.STRING)) {
                    String warpName = container.get(warpKey, PersistentDataType.STRING);
                    Warp warp = warpService.getWarp(warpName);
                    if (warp != null) openEditor(player, warp);
                }
            } else if ("warp_edit_layout_dummy".equals(type)) {
                return;
            } else if ("warp_edit_layout_btn".equals(type)) {
                openLayoutEditor(player);
            } else if ("warp_edit_action".equals(type)) {
                NamespacedKey actionKey = new NamespacedKey(plugin, "edit_action");
                NamespacedKey warpKey = new NamespacedKey(plugin, "warp_target");
                String action = container.get(actionKey, PersistentDataType.STRING);
                String warpName = container.get(warpKey, PersistentDataType.STRING);
                Warp warp = warpService.getWarp(warpName);
                if (warp == null) return;

                if ("name".equals(action)) {
                    pendingActions.put(player.getUniqueId(), new EditAction(warp, "name"));
                    player.closeInventory();
                    String prompt = messages.get().warpEditPromptName() != null ? messages.get().warpEditPromptName() : "<yellow>Escribe el nuevo nombre en el chat (o 'cancelar'):";
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + prompt));
                } else if ("lore".equals(action)) {
                    pendingActions.put(player.getUniqueId(), new EditAction(warp, "lore"));
                    player.closeInventory();
                    String prompt = messages.get().warpEditPromptLore() != null ? messages.get().warpEditPromptLore() : "<yellow>Escribe una línea de lore en el chat (o 'cancelar'):";
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + prompt));
                } else if ("clear_lore".equals(action)) {
                    warp.setDescription(new ArrayList<>());
                    warpService.saveWarp(warp);
                    String cleared = messages.get().warpEditLoreCleared() != null ? messages.get().warpEditLoreCleared() : "<green>Lore limpiado.";
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + cleared));
                    openEditor(player, warp);
                } else if ("icon".equals(action)) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType() == Material.AIR) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + "<red>Debes tener un ítem en tu mano."));
                    } else {
                        warp.setIconMaterial(hand.getType().name());
                        warpService.saveWarp(warp);
                        player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + "<green>Ícono actualizado."));
                        openEditor(player, warp);
                    }
                } else if ("location".equals(action)) {
                    warp.setServer(RevengeCoreAPI.get().getServerName());
                    warp.setWorld(player.getWorld().getName());
                    warp.setX(player.getLocation().getX());
                    warp.setY(player.getLocation().getY());
                    warp.setZ(player.getLocation().getZ());
                    warp.setYaw(player.getLocation().getYaw());
                    warp.setPitch(player.getLocation().getPitch());
                    warpService.saveWarp(warp);
                    String succ = messages.get().warpEditLocationSuccess() != null ? messages.get().warpEditLocationSuccess() : "<green>Ubicación actualizada.";
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + succ));
                    openEditor(player, warp);
                } else if ("delete".equals(action)) {
                    warpService.deleteWarp(warp.getName());
                    String succ = messages.get().warpEditDeleteSuccess() != null ? messages.get().warpEditDeleteSuccess() : "<red>Warp eliminado.";
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + succ));
                    openList(player, 0);
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (editingLayouts.contains(player.getUniqueId())) {
            String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
            if (!title.contains("Diseño")) return;
            
            editingLayouts.remove(player.getUniqueId());
            Inventory inv = event.getInventory();
            Map<Integer, String> newDecoration = new ConcurrentHashMap<>();
            for (int i = 0; i < 54; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    boolean skip = false;
                    if (item.hasItemMeta()) {
                        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                        NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
                        if (pdc.has(typeKey, PersistentDataType.STRING)) skip = true;
                        for (NamespacedKey k : pdc.getKeys()) {
                            if (k.getKey().contains("warp") || k.getKey().contains("page") || k.getKey().contains("list") || k.getKey().contains("target")) skip = true;
                        }
                    }
                    if (skip) continue;
                    newDecoration.put(i, inventorySyncService.serializeItems(new ItemStack[]{item}));
                }
            }
            WarpGuiData guiData = warpService.getGuiData();
            if (guiData != null) {
                guiData.setDecorativeItems(newDecoration);
                warpService.saveGuiData();
            }
            String succ = messages.get().warpEditLayoutSaved() != null ? messages.get().warpEditLayoutSaved() : "<green>Diseño de Warp GUI guardado.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + succ));
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
            String cancel = messages.get().warpEditCancel() != null ? messages.get().warpEditCancel() : "<red>Edición cancelada.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + cancel));
            player.getScheduler().run(plugin, task -> openEditor(player, action.warp), null);
            return;
        }

        Warp warp = action.warp;
        if ("name".equals(action.type)) {
            warp.setDisplayName(msg);
        } else if ("lore".equals(action.type)) {
            List<String> lore = warp.getDescription();
            if (lore == null) lore = new ArrayList<>();
            lore.add(msg);
            warp.setDescription(lore);
        }
        
        warpService.saveWarp(warp);
        String succ = messages.get().warpSetSuccess() != null ? messages.get().warpSetSuccess() : "<green>Warp actualizado exitosamente.";
        player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + succ, Placeholder.parsed("warp", warp.getName())));
        player.getScheduler().run(plugin, task -> openEditor(player, warp), null);
    }
}
