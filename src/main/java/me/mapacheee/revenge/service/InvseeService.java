package me.mapacheee.revenge.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossInvseeRequestMessage;
import me.mapacheee.revenge.channel.CrossInvseeResponseMessage;
import me.mapacheee.revenge.channel.CrossInvseeUpdateMessage;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.inventory.InvseeHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InvseeService implements Listener {

    private static final Logger logger = LoggerFactory.getLogger(InvseeService.class);

    private final ChannelService channelService;
    private final PlayerDataService playerDataService;
    private final InventorySyncService inventorySyncService;
    private final Container<Messages> messages;
    private final Plugin plugin;
    private final Gson gson = new Gson();

    @Inject
    public InvseeService(PlayerDataService playerDataService, InventorySyncService inventorySyncService,
                         Container<Messages> messages, Plugin plugin) {
        this.playerDataService = playerDataService;
        this.inventorySyncService = inventorySyncService;
        this.messages = messages;
        this.plugin = plugin;
        this.channelService = RevengeCoreAPI.get().getChannelService();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerChannels();
    }

    private void registerChannels() {
        channelService.subscribe("revenge:invsee_req", CrossInvseeRequestMessage.class, msg -> {
            Player target = Bukkit.getPlayerExact(msg.getTargetName());
            if (target != null && target.isOnline()) {
                JsonObject data = new JsonObject();
                data.addProperty("inventory", inventorySyncService.serializeItems(target.getInventory().getContents()));
                data.addProperty("armor", inventorySyncService.serializeItems(target.getInventory().getArmorContents()));
                data.addProperty("offhand", inventorySyncService.serializeItems(new ItemStack[]{target.getInventory().getItemInOffHand()}));
                channelService.publish("revenge:invsee_res", new CrossInvseeResponseMessage(
                        msg.getSenderName(),
                        msg.getTargetName(),
                        gson.toJson(data),
                        RevengeCoreAPI.get().getServerName()
                ));
            }
        }, plugin.getSLF4JLogger());

        channelService.subscribe("revenge:invsee_res", CrossInvseeResponseMessage.class, msg -> {
            Player sender = Bukkit.getPlayerExact(msg.getSenderName());
            if (sender != null && sender.isOnline()) {
                RedissonClient redisson = RevengeCoreAPI.get().getRedisService().client();
                RBucket<String> bucket = redisson.getBucket("revenge:online:" + msg.getTargetName());
                String targetServer = bucket.get();

                if (targetServer != null) {
                    processOpening(sender, msg.getTargetName(), targetServer, msg.getInventoryJson());
                }
            }
        }, plugin.getSLF4JLogger());

        channelService.subscribe("revenge:invsee_upd", CrossInvseeUpdateMessage.class, msg -> {
            if (msg.getTargetServer().equals(RevengeCoreAPI.get().getServerName())) {
                Player target = Bukkit.getPlayerExact(msg.getTargetName());
                if (target != null && target.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        JsonObject data = gson.fromJson(msg.getInventoryJson(), JsonObject.class);
                        try {
                            if (data.has("inventory")) {
                                target.getInventory().setContents(inventorySyncService.deserializeItems(data.get("inventory").getAsString()));
                            }
                            if (data.has("armor")) {
                                target.getInventory().setArmorContents(inventorySyncService.deserializeItems(data.get("armor").getAsString()));
                            }
                            if (data.has("offhand")) {
                                ItemStack[] offhand = inventorySyncService.deserializeItems(data.get("offhand").getAsString());
                                if (offhand.length > 0) target.getInventory().setItemInOffHand(offhand[0]);
                            }
                        } catch (Exception e) {
                            logger.error("Error updating live inventory via cross-server message", e);
                        }
                    });
                }
            }
        }, plugin.getSLF4JLogger());
    }

    public void openInvsee(Player viewer, String targetName) {
        RedissonClient redisson = RevengeCoreAPI.get().getRedisService().client();
        RBucket<String> bucket = redisson.getBucket("revenge:online:" + targetName);
        String targetServer = bucket.get();

        if (targetServer != null) {
            if (targetServer.equals(RevengeCoreAPI.get().getServerName())) {
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    JsonObject data = new JsonObject();
                    data.addProperty("inventory", inventorySyncService.serializeItems(target.getInventory().getContents()));
                    data.addProperty("armor", inventorySyncService.serializeItems(target.getInventory().getArmorContents()));
                    data.addProperty("offhand", inventorySyncService.serializeItems(new ItemStack[]{target.getInventory().getItemInOffHand()}));
                    processOpening(viewer, targetName, targetServer, gson.toJson(data));
                }
            } else {
                channelService.publish("revenge:invsee_req", new CrossInvseeRequestMessage(
                        viewer.getName(),
                        targetName,
                        RevengeCoreAPI.get().getServerName()
                ));
            }
        } else {
            playerDataService.getUUIDFromName(targetName).thenAccept(uuid -> {
                if (uuid == null) {
                    viewer.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().playerNotOnline()));
                    return;
                }
                playerDataService.getPlayerData(uuid, targetName).thenAccept(data -> {
                    String json = data.getInventory();
                    processOpening(viewer, targetName, null, json);
                });
            });
        }
    }

    private void processOpening(Player viewer, String targetName, String targetServer, String inventoryJson) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Component title = MiniMessage.miniMessage().deserialize("<dark_gray>Invesee: <blue>" + targetName);
            InvseeHolder holder = new InvseeHolder(targetName, targetServer);
            Inventory inv = Bukkit.createInventory(holder, 54, title);
            holder.setInventory(inv);

            if (inventoryJson != null && !inventoryJson.equals("{}") && !inventoryJson.isEmpty()) {
                try {
                    JsonObject data = gson.fromJson(inventoryJson, JsonObject.class);
                    if (data.has("inventory")) {
                        ItemStack[] mainInv = inventorySyncService.deserializeItems(data.get("inventory").getAsString());
                        for (int i = 0; i < Math.min(mainInv.length, 36); i++) {
                            inv.setItem(i, mainInv[i]);
                        }
                    }
                    if (data.has("armor")) {
                        ItemStack[] armor = inventorySyncService.deserializeItems(data.get("armor").getAsString());
                        for (int i = 0; i < Math.min(armor.length, 4); i++) {
                            inv.setItem(36 + i, armor[i]);
                        }
                    }
                    if (data.has("offhand")) {
                        ItemStack[] offhand = inventorySyncService.deserializeItems(data.get("offhand").getAsString());
                        if (offhand.length > 0) {
                            inv.setItem(40, offhand[0]);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error loading inventory from JSON", e);
                }
            }

            ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            for (int i = 41; i < 54; i++) {
                inv.setItem(i, border);
            }

            viewer.openInventory(inv);
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof InvseeHolder) {
            if (event.getRawSlot() >= 41 && event.getRawSlot() < 54) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof InvseeHolder holder) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                Inventory inv = event.getInventory();
                
                ItemStack[] mainInv = new ItemStack[36];
                for(int i = 0; i < 36; i++) {
                    mainInv[i] = inv.getItem(i);
                }
                
                ItemStack[] armor = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    armor[i] = inv.getItem(36 + i);
                }
                
                ItemStack[] offhand = new ItemStack[1];
                offhand[0] = inv.getItem(40);

                String mainJson = inventorySyncService.serializeItems(mainInv);
                String armorJson = inventorySyncService.serializeItems(armor);
                String offhandJson = inventorySyncService.serializeItems(offhand);

                JsonObject dataToMerge = new JsonObject();
                dataToMerge.addProperty("inventory", mainJson);
                dataToMerge.addProperty("armor", armorJson);
                dataToMerge.addProperty("offhand", offhandJson);

                if (holder.getTargetServer() != null) {
                    channelService.publish("revenge:invsee_upd", new CrossInvseeUpdateMessage(
                            holder.getTargetName(),
                            holder.getTargetServer(),
                            gson.toJson(dataToMerge),
                            RevengeCoreAPI.get().getServerName()
                    ));
                } else {
                    playerDataService.getUUIDFromName(holder.getTargetName()).thenAccept(uuid -> {
                        if (uuid != null) {
                            playerDataService.getPlayerData(uuid, holder.getTargetName()).thenAccept(playerData -> {
                                JsonObject fullJson;
                                if (playerData.getInventory() != null && !playerData.getInventory().equals("{}")) {
                                    fullJson = gson.fromJson(playerData.getInventory(), JsonObject.class);
                                } else {
                                    fullJson = new JsonObject();
                                }
                                fullJson.addProperty("inventory", mainJson);
                                fullJson.addProperty("armor", armorJson);
                                fullJson.addProperty("offhand", offhandJson);
                                
                                playerData.setInventory(gson.toJson(fullJson));
                                playerDataService.savePlayerData(playerData);
                            });
                        }
                    });
                }
            });
        }
    }
}
