package me.mapacheee.revenge.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventorySyncService {

    private final PlayerDataService playerDataService;
    private final Plugin plugin;
    private final Gson gson = new Gson();
    private final Set<UUID> syncingPlayers = ConcurrentHashMap.newKeySet();

    @Inject
    public InventorySyncService(Plugin plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
    }

    private PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public boolean isSyncing(UUID uuid) {
        return syncingPlayers.contains(uuid);
    }

    public void savePlayerData(Player player) {
        if (syncingPlayers.contains(player.getUniqueId()))
            return;
        syncingPlayers.add(player.getUniqueId());

        try {
            JsonObject data = new JsonObject();
            data.addProperty("inventory", serializeItems(player.getInventory().getContents()));
            data.addProperty("armor", serializeItems(player.getInventory().getArmorContents()));
            data.addProperty("enderChest", serializeItems(player.getEnderChest().getContents()));
            data.addProperty("offhand", serializeItems(new ItemStack[] { player.getInventory().getItemInOffHand() }));
            data.addProperty("xp", player.getExp());
            data.addProperty("xpLevel", player.getLevel());
            data.addProperty("totalXp", player.getTotalExperience());
            data.addProperty("flyEnabled", player.getAllowFlight());
            data.addProperty("walkSpeed", player.getWalkSpeed());
            data.addProperty("flySpeed", player.getFlySpeed());
            data.addProperty("gamemode", player.getGameMode().name());
            data.addProperty("godmode", player.isInvulnerable());

            String json = gson.toJson(data);

            getPlayerDataService().getPlayerData(player.getUniqueId(), player.getName()).thenAccept(playerData -> {
                playerData.setInventory(json);
                getPlayerDataService().savePlayerData(playerData).thenRun(() -> {
                    getPlayerDataService().setPlayerXp(player.getUniqueId(), player.getName(),
                            player.getTotalExperience());
                    syncingPlayers.remove(player.getUniqueId());
                });
            }).exceptionally(ex -> {
                syncingPlayers.remove(player.getUniqueId());
                return null;
            });
        } catch (Exception e) {
            syncingPlayers.remove(player.getUniqueId());
        }
    }

    public void loadPlayerData(Player player) {
        if (syncingPlayers.contains(player.getUniqueId()))
            return;
        syncingPlayers.add(player.getUniqueId());

        getPlayerDataService().getPlayerData(player.getUniqueId(), player.getName()).thenAccept(playerData -> {
            String inventoryJson = playerData.getInventory();
            if (inventoryJson == null || inventoryJson.equals("{}") || inventoryJson.isEmpty()) {
                syncingPlayers.remove(player.getUniqueId());
                return;
            }

            try {
                JsonObject data = gson.fromJson(inventoryJson, JsonObject.class);

                Bukkit.getAsyncScheduler().runDelayed(plugin, delayedTask -> {
                    player.getScheduler().run(plugin, task -> {
                        try {
                            if (data.has("inventory")) {
                                ItemStack[] items = deserializeItems(data.get("inventory").getAsString());
                                if (items != null)
                                    player.getInventory().setContents(items);
                            }
                            if (data.has("armor")) {
                                ItemStack[] armor = deserializeItems(data.get("armor").getAsString());
                                if (armor != null)
                                    player.getInventory().setArmorContents(armor);
                            }
                            if (data.has("enderChest")) {
                                ItemStack[] ender = deserializeItems(data.get("enderChest").getAsString());
                                if (ender != null)
                                    player.getEnderChest().setContents(ender);
                            }
                            if (data.has("offhand")) {
                                ItemStack[] offhand = deserializeItems(data.get("offhand").getAsString());
                                if (offhand != null && offhand.length > 0 && offhand[0] != null) {
                                    player.getInventory().setItemInOffHand(offhand[0]);
                                }
                            }
                            if (data.has("xpLevel")) player.setLevel(data.get("xpLevel").getAsInt());
                            if (data.has("xp")) player.setExp(data.get("xp").getAsFloat());
                            if (data.has("totalXp")) player.setTotalExperience(data.get("totalXp").getAsInt());
                            if (data.has("flyEnabled")) {
                                boolean fly = data.get("flyEnabled").getAsBoolean();
                                player.setAllowFlight(fly);
                                boolean inAir = player.getLocation().clone().subtract(0, 0.01, 0).getBlock()
                                        .isPassable();
                                player.setFlying(fly && inAir);
                            }
                            if (data.has("walkSpeed")) player.setWalkSpeed(data.get("walkSpeed").getAsFloat());
                            if (data.has("flySpeed")) player.setFlySpeed(data.get("flySpeed").getAsFloat());
                            if (data.has("gamemode")) {
                                try {
                                    player.setGameMode(GameMode.valueOf(data.get("gamemode").getAsString()));
                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                            if (data.has("godmode")) {
                                player.setInvulnerable(data.get("godmode").getAsBoolean());
                            }
                            
                            boolean needsSave = false;
                            if (playerData.getQueuedGodmode() != null) {
                                player.setInvulnerable(playerData.getQueuedGodmode());
                                playerData.setQueuedGodmode(null);
                                needsSave = true;
                            }
                            if (playerData.getQueuedGamemode() != null) {
                                try {
                                    player.setGameMode(GameMode.valueOf(playerData.getQueuedGamemode()));
                                } catch (IllegalArgumentException ignored) {}
                                playerData.setQueuedGamemode(null);
                                needsSave = true;
                            }
                            if (playerData.getQueuedHeal() != null && playerData.getQueuedHeal()) {
                                double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null 
                                    ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
                                player.setHealth(maxHealth);
                                player.setFoodLevel(20);
                                player.setFireTicks(0);
                                player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
                                playerData.setQueuedHeal(null);
                                needsSave = true;
                            }
                            
                            if (playerData.getOfflineMessages() != null && !playerData.getOfflineMessages().isEmpty()) {
                                for (String msg : playerData.getOfflineMessages()) {
                                    player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                                }
                                playerData.getOfflineMessages().clear();
                                needsSave = true;
                            }
                            
                            if (needsSave) {
                                getPlayerDataService().savePlayerData(playerData);
                            }
                            
                        } finally {
                            syncingPlayers.remove(player.getUniqueId());
                        }
                    }, () -> syncingPlayers.remove(player.getUniqueId()));
                }, 500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                syncingPlayers.remove(player.getUniqueId());
            }
        });
    }

    public String serializeItems(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutput = new DataOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                if (item == null || item.getType() == Material.AIR) {
                    dataOutput.writeInt(0);
                } else {
                    byte[] bytes = item.serializeAsBytes();
                    dataOutput.writeInt(bytes.length);
                    dataOutput.write(bytes);
                }
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public ItemStack[] deserializeItems(String data) {
        if (data == null || data.isEmpty())
            return null;

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            DataInputStream dataInput = new DataInputStream(inputStream);
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                int length = dataInput.readInt();
                if (length == 0) {
                    items[i] = null;
                } else {
                    byte[] bytes = new byte[length];
                    dataInput.readFully(bytes);
                    items[i] = ItemStack.deserializeBytes(bytes);
                }
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            return null;
        }
    }
}
