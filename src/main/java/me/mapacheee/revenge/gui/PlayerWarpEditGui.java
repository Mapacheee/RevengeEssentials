package me.mapacheee.revenge.gui;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.PlayerWarp;
import me.mapacheee.revenge.service.PlayerWarpService;
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
public class PlayerWarpEditGui implements Listener {

    private final PlayerWarpService pwarpService;
    private final Container<Messages> messages;
    private final Plugin plugin;
    
    private final Map<UUID, EditAction> pendingActions = new ConcurrentHashMap<>();
    
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;

    private static final List<Integer> AVAILABLE_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );

    private record EditAction(PlayerWarp warp, String type) {}

    @Inject
    public PlayerWarpEditGui(PlayerWarpService pwarpService, Container<Messages> messages, Plugin plugin) {
        this.pwarpService = pwarpService;
        this.messages = messages;
        this.plugin = plugin;
    }

    public void openList(Player player, int page) {
        player.getScheduler().run(plugin, task -> {
            boolean isAdmin = player.hasPermission("revenge.pwarp.admin");
            String tMsg = messages.get().pwarpEditListTitle() != null ? messages.get().pwarpEditListTitle() : "<dark_aqua><bold>Mis PWarps</bold> <gray>- Pág <page>";
            Component title = MiniMessage.miniMessage().deserialize(tMsg, Placeholder.parsed("page", String.valueOf(page + 1)));
            Inventory inv = Bukkit.createInventory(null, 54, title);

            List<PlayerWarp> warps;
            if (isAdmin) {
                warps = new ArrayList<>(pwarpService.getPwarps());
            } else {
                warps = new ArrayList<>(pwarpService.getPwarpsByOwner(player.getUniqueId()));
            }
            warps.sort(Comparator.comparing(PlayerWarp::getName));

            int maxPages = (int) Math.ceil((double) warps.size() / AVAILABLE_SLOTS.size());
            if (maxPages == 0) maxPages = 1;

            int actualPage = page;
            if (actualPage < 0) actualPage = 0;
            if (actualPage >= maxPages) actualPage = maxPages - 1;

            int startIndex = actualPage * AVAILABLE_SLOTS.size();
            int endIndex = Math.min(startIndex + AVAILABLE_SLOTS.size(), warps.size());

            AtomicInteger index = new AtomicInteger(0);
            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
            NamespacedKey warpKey = new NamespacedKey(plugin, "pwarp_target");

            for (int i = startIndex; i < endIndex; i++) {
                PlayerWarp warp = warps.get(i);
                Material mat = Material.matchMaterial(warp.getIconMaterial() != null ? warp.getIconMaterial().toUpperCase() : "ENDER_EYE");
                if (mat == null) mat = Material.ENDER_EYE;
                
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(MiniMessage.miniMessage().deserialize(warp.getDisplayName()).decoration(TextDecoration.ITALIC, false));
                
                List<Component> lore = new ArrayList<>();
                lore.add(MiniMessage.miniMessage().deserialize("<gray>ID: " + warp.getName()).decoration(TextDecoration.ITALIC, false));
                if (isAdmin) {
                    lore.add(MiniMessage.miniMessage().deserialize("<gray>Dueño: " + (warp.getOwnerUuid() != null ? org.bukkit.Bukkit.getOfflinePlayer(warp.getOwnerUuid()).getName() : "Desconocido")).decoration(TextDecoration.ITALIC, false));
                }
                lore.add(Component.empty());
                String clickToEditMsg = messages.get().pwarpClickTeleport() != null ? messages.get().pwarpClickTeleport() : "<yellow>Click para editar";
                lore.add(MiniMessage.miniMessage().deserialize(clickToEditMsg).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                
                meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "pwarp_edit_list");
                meta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
                item.setItemMeta(meta);
                
                int slot = AVAILABLE_SLOTS.get(index.getAndIncrement());
                inv.setItem(slot, item);
            }

            if (actualPage > 0) {
                ItemStack prevBtn = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevBtn.getItemMeta();
                String pMsg = messages.get().warpEditPrevPage() != null ? messages.get().warpEditPrevPage() : "<aqua><bold>⮜ Página Anterior";
                prevMeta.displayName(MiniMessage.miniMessage().deserialize(pMsg).decoration(TextDecoration.ITALIC, false));
                NamespacedKey pageKey = new NamespacedKey(plugin, "list_page");
                prevMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, actualPage - 1);
                prevMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "pwarp_edit_list");
                prevBtn.setItemMeta(prevMeta);
                inv.setItem(PREV_PAGE_SLOT, prevBtn);
            }

            if (actualPage < maxPages - 1) {
                ItemStack nextBtn = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextBtn.getItemMeta();
                String nMsg = messages.get().warpEditNextPage() != null ? messages.get().warpEditNextPage() : "<aqua><bold>Página Siguiente ⮞";
                nextMeta.displayName(MiniMessage.miniMessage().deserialize(nMsg).decoration(TextDecoration.ITALIC, false));
                NamespacedKey pageKey = new NamespacedKey(plugin, "list_page");
                nextMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, actualPage + 1);
                nextMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "pwarp_edit_list");
                nextBtn.setItemMeta(nextMeta);
                inv.setItem(NEXT_PAGE_SLOT, nextBtn);
            }

            player.openInventory(inv);
        }, null);
    }

    public void openEditor(Player player, PlayerWarp warp) {
        player.getScheduler().run(plugin, task -> {
            String eMsg = messages.get().pwarpEditEditorTitle() != null ? messages.get().pwarpEditEditorTitle() : "<dark_aqua><bold>Editar:</bold> <gray><warp>";
            Component title = MiniMessage.miniMessage().deserialize(eMsg, Placeholder.parsed("warp", warp.getName()));
            Inventory inv = Bukkit.createInventory(null, 54, title);

            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
            NamespacedKey actionKey = new NamespacedKey(plugin, "edit_action");
            NamespacedKey warpKey = new NamespacedKey(plugin, "pwarp_target");

            ItemStack nameBtn = new ItemStack(Material.NAME_TAG);
            ItemMeta nameMeta = nameBtn.getItemMeta();
            String nameMsg = messages.get().warpEditBtnName() != null ? messages.get().warpEditBtnName() : "<yellow>Cambiar Nombre Visible";
            nameMeta.displayName(MiniMessage.miniMessage().deserialize(nameMsg).decoration(TextDecoration.ITALIC, false));
            nameMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "pwarp_edit_action");
            nameMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "name");
            nameMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            nameBtn.setItemMeta(nameMeta);
            inv.setItem(11, nameBtn);

            ItemStack loreBtn = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta loreMeta = loreBtn.getItemMeta();
            String loreMsg = messages.get().warpEditBtnLore() != null ? messages.get().warpEditBtnLore() : "<yellow>Añadir línea de Descripción";
            loreMeta.displayName(MiniMessage.miniMessage().deserialize(loreMsg).decoration(TextDecoration.ITALIC, false));
            loreMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "pwarp_edit_action");
            loreMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "lore");
            loreMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            loreBtn.setItemMeta(loreMeta);
            inv.setItem(13, loreBtn);

            ItemStack clearBtn = new ItemStack(Material.BARRIER);
            ItemMeta clearMeta = clearBtn.getItemMeta();
            String clearMsg = messages.get().warpEditBtnClearLore() != null ? messages.get().warpEditBtnClearLore() : "<red>Limpiar Descripción";
            clearMeta.displayName(MiniMessage.miniMessage().deserialize(clearMsg).decoration(TextDecoration.ITALIC, false));
            clearMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "pwarp_edit_action");
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
            iconMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "pwarp_edit_action");
            iconMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "icon");
            iconMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            iconBtn.setItemMeta(iconMeta);
            inv.setItem(29, iconBtn);

            ItemStack locBtn = new ItemStack(Material.ENDER_PEARL);
            ItemMeta locMeta = locBtn.getItemMeta();
            String locMsg = messages.get().warpEditBtnLocation() != null ? messages.get().warpEditBtnLocation() : "<aqua>Actualizar Ubicación";
            String locLoreMsg = messages.get().pwarpEditBtnLocationLore() != null ? messages.get().pwarpEditBtnLocationLore() : "<gray>Actualiza el pwarp a tu posición actual.";
            locMeta.displayName(MiniMessage.miniMessage().deserialize(locMsg).decoration(TextDecoration.ITALIC, false));
            locMeta.lore(List.of(MiniMessage.miniMessage().deserialize(locLoreMsg).decoration(TextDecoration.ITALIC, false)));
            locMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "pwarp_edit_action");
            locMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "location");
            locMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            locBtn.setItemMeta(locMeta);
            inv.setItem(31, locBtn);

            ItemStack delBtn = new ItemStack(Material.LAVA_BUCKET);
            ItemMeta delMeta = delBtn.getItemMeta();
            String delMsg = messages.get().pwarpEditBtnDelete() != null ? messages.get().pwarpEditBtnDelete() : "<dark_red><bold>Borrar Player Warp";
            delMeta.displayName(MiniMessage.miniMessage().deserialize(delMsg).decoration(TextDecoration.ITALIC, false));
            delMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "pwarp_edit_action");
            delMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "delete");
            delMeta.getPersistentDataContainer().set(warpKey, PersistentDataType.STRING, warp.getName());
            delBtn.setItemMeta(delMeta);
            inv.setItem(33, delBtn);

            player.openInventory(inv);
        }, null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory topInv = event.getView().getTopInventory();
        
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInv)) {
            boolean isEditGui = false;
            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
            for (ItemStack it : topInv.getContents()) {
                if (it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                    String type = it.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
                    if (type != null && type.startsWith("pwarp_edit_")) {
                        isEditGui = true;
                        break;
                    }
                }
            }
            if (isEditGui) event.setCancelled(true);
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            boolean isEditGui = false;
            NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
            for (ItemStack it : topInv.getContents()) {
                if (it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                    String type = it.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
                    if (type != null && type.startsWith("pwarp_edit_")) {
                        isEditGui = true;
                        break;
                    }
                }
            }
            if (isEditGui) event.setCancelled(true);
            return;
        }

        NamespacedKey typeKey = new NamespacedKey(plugin, "gui_type");
        PersistentDataContainer container = clicked.getItemMeta().getPersistentDataContainer();

        if (container.has(typeKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            String type = container.get(typeKey, PersistentDataType.STRING);
            
            if ("pwarp_edit_list".equals(type)) {
                NamespacedKey pageKey = new NamespacedKey(plugin, "list_page");
                NamespacedKey warpKey = new NamespacedKey(plugin, "pwarp_target");
                
                if (container.has(pageKey, PersistentDataType.INTEGER)) {
                    openList(player, container.get(pageKey, PersistentDataType.INTEGER));
                } else if (container.has(warpKey, PersistentDataType.STRING)) {
                    String warpName = container.get(warpKey, PersistentDataType.STRING);
                    PlayerWarp warp = pwarpService.getPwarp(warpName);
                    if (warp != null) {
                        if (warp.getOwnerUuid().equals(player.getUniqueId()) || player.hasPermission("revenge.pwarp.admin")) {
                            openEditor(player, warp);
                        }
                    }
                }
            } else if ("pwarp_edit_action".equals(type)) {
                NamespacedKey actionKey = new NamespacedKey(plugin, "edit_action");
                NamespacedKey warpKey = new NamespacedKey(plugin, "pwarp_target");
                String action = container.get(actionKey, PersistentDataType.STRING);
                String warpName = container.get(warpKey, PersistentDataType.STRING);
                PlayerWarp warp = pwarpService.getPwarp(warpName);
                if (warp == null) return;
                
                if (!warp.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("revenge.pwarp.admin")) return;

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
                    pwarpService.savePwarp(warp);
                    String cleared = messages.get().warpEditLoreCleared() != null ? messages.get().warpEditLoreCleared() : "<green>Lore limpiado.";
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + cleared));
                    openEditor(player, warp);
                } else if ("icon".equals(action)) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType() == Material.AIR) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + "<red>Debes tener un ítem en tu mano."));
                    } else {
                        warp.setIconMaterial(hand.getType().name());
                        pwarpService.savePwarp(warp);
                        player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + "<green>Ícono actualizado."));
                        openEditor(player, warp);
                    }
                } else if ("location".equals(action)) {
                    warp.setServer(me.mapacheee.revenge.api.RevengeCoreAPI.get().getServerName());
                    warp.setWorld(player.getWorld().getName());
                    warp.setX(player.getLocation().getX());
                    warp.setY(player.getLocation().getY());
                    warp.setZ(player.getLocation().getZ());
                    warp.setYaw(player.getLocation().getYaw());
                    warp.setPitch(player.getLocation().getPitch());
                    pwarpService.savePwarp(warp);
                    String succ = messages.get().warpEditLocationSuccess() != null ? messages.get().warpEditLocationSuccess() : "<green>Ubicación actualizada.";
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + succ));
                    openEditor(player, warp);
                } else if ("delete".equals(action)) {
                    pwarpService.deletePwarp(warp.getName());
                    String succ = messages.get().pwarpEditDeleteSuccess() != null ? messages.get().pwarpEditDeleteSuccess() : "<red>Player Warp eliminado.";
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + succ));
                    openList(player, 0);
                }
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
            String cancel = messages.get().warpEditCancel() != null ? messages.get().warpEditCancel() : "<red>Edición cancelada.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + cancel));
            player.getScheduler().run(plugin, task -> openEditor(player, action.warp), null);
            return;
        }

        PlayerWarp warp = action.warp;
        if ("name".equals(action.type)) {
            warp.setDisplayName(msg);
        } else if ("lore".equals(action.type)) {
            List<String> lore = warp.getDescription();
            if (lore == null) lore = new ArrayList<>();
            lore.add(msg);
            warp.setDescription(lore);
        }
        
        pwarpService.savePwarp(warp);
        String succ = messages.get().pwarpSetSuccess() != null ? messages.get().pwarpSetSuccess() : "<green>Player Warp actualizado exitosamente.";
        player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + succ, Placeholder.parsed("warp", warp.getName())));
        player.getScheduler().run(plugin, task -> openEditor(player, warp), null);
    }
}
