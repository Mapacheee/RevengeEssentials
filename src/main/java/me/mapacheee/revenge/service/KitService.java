package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.data.Kit;
import org.bukkit.Bukkit;
import me.mapacheee.revenge.data.KitRepository;
import me.mapacheee.revenge.data.KitGuiData;
import me.mapacheee.revenge.data.KitGuiRepository;
import org.bukkit.plugin.Plugin;
import org.redisson.api.RTopic;
import org.bukkit.Material;
import java.util.UUID;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.ConcurrentHashMap;
import me.mapacheee.revenge.data.PlayerData;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.ArrayList;
import com.mongodb.client.model.Filters;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import me.mapacheee.revenge.channel.CrossFeedbackMessage;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

@Service
public class KitService {

    private final KitRepository kitRepository;
    private final KitGuiRepository kitGuiRepository;
    private final ValkeyService valkeyService;
    private final Plugin plugin;
    private final Container<Messages> messages;
    
    private final Map<String, Kit> cachedKits = new ConcurrentHashMap<>();
    private KitGuiData guiData;

    @Inject
    public KitService(KitRepository kitRepository, KitGuiRepository kitGuiRepository, Plugin plugin, Container<Messages> messages) {
        this.kitRepository = kitRepository;
        this.kitGuiRepository = kitGuiRepository;
        this.valkeyService = RevengeCoreAPI.get().getRedisService();
        this.plugin = plugin;
        this.messages = messages;
    }

    @OnEnable
    public void onEnable() {
        kitRepository.findAll().forEach(kit -> {
            cachedKits.put(kit.getName().toLowerCase(), kit);
        });
        plugin.getSLF4JLogger().info("Loaded {} Kits from mongodb.", cachedKits.size());

        loadGuiData();

        RTopic topic = valkeyService.client().getTopic("revenge:kit_gui");
        topic.addListener(String.class, (channel, msg) -> {
            plugin.getSLF4JLogger().info("Received cross-server Kit GUI Layout update. Reloading from MongoDB...");
            loadGuiData();
        });
    }

    private void loadGuiData() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            KitGuiData data = kitGuiRepository.findOne(Filters.eq("guiId", "global"));
            if (data == null) {
                data = new KitGuiData();
                kitGuiRepository.save(data);
            }
            this.guiData = data;
        });
    }

    public void saveGuiData() {
        if (guiData != null) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                kitGuiRepository.save(guiData);
                valkeyService.client().getTopic("revenge:kit_gui").publish("update");
            });
        }
    }

    public KitGuiData getGuiData() {
        if (guiData == null) {
            guiData = new KitGuiData();
        }
        return guiData;
    }

    public void saveKit(Kit kit) {
        cachedKits.put(kit.getName().toLowerCase(), kit);
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            kitRepository.save(kit);
        });
    }

    public void deleteKit(String kitName) {
        Kit kit = cachedKits.remove(kitName.toLowerCase());
        if (kit != null) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                kitRepository.delete(Filters.eq("_id", kit.id()));
            });
        }
    }

    public Kit getKit(String name) {
        return cachedKits.get(name.toLowerCase());
    }

    public List<Kit> getAllKits() {
        return new ArrayList<>(cachedKits.values());
    }

    public long getRemainingCooldown(Player player, Kit kit, PlayerData data) {
        if (data == null) return kit.getCooldownSeconds();

        if (data.getPendingKits().getOrDefault(kit.getName().toLowerCase(), 0) > 0) {
            return 0;
        }

        if (!player.hasPermission(kit.getPermission())) {
            return -1;
        }

        Long lastClaim = data.getKitCooldowns().get(kit.getName().toLowerCase());
        if (lastClaim == null) return 0;

        long elapsedSeconds = (System.currentTimeMillis() - lastClaim) / 1000;
        long remaining = kit.getCooldownSeconds() - elapsedSeconds;

        return remaining > 0 ? remaining : 0;
    }

    public void processKitClaim(Player player, Kit kit, PlayerData data, InventorySyncService inventorySyncService) {
        int pending = data.getPendingKits().getOrDefault(kit.getName().toLowerCase(), 0);
        if (pending > 0 && (!player.hasPermission(kit.getPermission()) || getRemainingCooldown(player, kit, data) > 0)) {
            data.getPendingKits().put(kit.getName().toLowerCase(), pending - 1);
        } else {
            data.getKitCooldowns().put(kit.getName().toLowerCase(), System.currentTimeMillis());
        }

        try {
            ItemStack[] items = inventorySyncService.deserializeItems(kit.getInventoryBase64());
            if (items != null) {
                for (ItemStack item : items) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                    if (!leftover.isEmpty()) {
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("Failed to decode Kit {} inventory.", kit.getName());
        }
    }

    public void giveVoucher(String targetName, Kit kit, PlayerDataService dataService) {
        dataService.getUUIDFromName(targetName).thenAccept(uuidOpt -> {
            if (uuidOpt != null) {
                String uuid = uuidOpt.toString();
                dataService.getPlayerData(UUID.fromString(uuid), targetName).thenAccept(data -> {
                    if (data != null) {
                        Map<String, Integer> map = data.getPendingKits();
                        map.put(kit.getName().toLowerCase(), map.getOrDefault(kit.getName().toLowerCase(), 0) + 1);
                        
                        String onlinePayload = messages.get().prefix() + messages.get().kitReceivedOnline();
                        onlinePayload = onlinePayload.replace("<kit>", kit.getDisplayName() != null ? kit.getDisplayName() : kit.getName());
                        
                        String offlinePayload = messages.get().prefix() + messages.get().kitReceivedOffline();
                        offlinePayload = offlinePayload.replace("<kit>", kit.getDisplayName() != null ? kit.getDisplayName() : kit.getName());
                        
                        String trueName = data.getName() != null ? data.getName() : targetName;
                        RedissonClient redisson = valkeyService.client();
                        RBucket<String> bucket = redisson.getBucket("revenge:online:" + trueName);
                        String serverInRedis = bucket.get();

                        if (serverInRedis != null) {
                            if (serverInRedis.equals(RevengeCoreAPI.get().getServerName())) {
                                Player targetPlayer = Bukkit.getPlayerExact(trueName);
                                if (targetPlayer != null && targetPlayer.isOnline()) {
                                    targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(onlinePayload, Placeholder.parsed("kit", kit.getName())));
                                }
                            } else {
                                RevengeCoreAPI.get().getChannelService().publish("revenge:feedback", new CrossFeedbackMessage(trueName, onlinePayload));
                            }
                        } else {
                            data.getOfflineMessages().add(offlinePayload);
                        }

                        dataService.savePlayerData(data);
                    }
                });
            }
        });
    }

}
