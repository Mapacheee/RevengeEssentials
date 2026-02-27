package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossPlayerWarpUpdateMessage;
import com.mongodb.client.model.Filters;
import me.mapacheee.revenge.data.PlayerWarp;
import me.mapacheee.revenge.data.PlayerWarpRepository;
import me.mapacheee.revenge.data.PlayerWarpGuiData;
import me.mapacheee.revenge.data.PlayerWarpGuiRepository;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.redisson.api.RTopic;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PlayerWarpService {

    private final PlayerWarpRepository pwarpRepository;
    private final PlayerWarpGuiRepository pwarpGuiRepository;
    private final Plugin plugin;
    
    private final Map<String, PlayerWarp> cachedPWarps = new ConcurrentHashMap<>();
    private PlayerWarpGuiData guiData;

    @Inject
    public PlayerWarpService(PlayerWarpRepository pwarpRepository, PlayerWarpGuiRepository pwarpGuiRepository, Plugin plugin) {
        this.pwarpRepository = pwarpRepository;
        this.pwarpGuiRepository = pwarpGuiRepository;
        this.plugin = plugin;
    }

    @OnEnable
    public void onEnable() {
        loadGuiData();

        pwarpRepository.findAll().forEach(warp -> {
            cachedPWarps.put(warp.getName().toLowerCase(), warp);
        });

        RevengeCoreAPI.get().getChannelService().subscribe("revenge:pwarp_update", CrossPlayerWarpUpdateMessage.class, msg -> {
            if (msg.serverName.equals(RevengeCoreAPI.get().getServerName())) return;
            
            if (msg.deleted) {
                cachedPWarps.remove(msg.warpName.toLowerCase());
            } else {
                PlayerWarp warp = pwarpRepository.findOne(Filters.eq("name", msg.warpName));
                if (warp != null) {
                    cachedPWarps.put(warp.getName().toLowerCase(), warp);
                }
            }
        }, plugin.getSLF4JLogger());

        RTopic topic = RevengeCoreAPI.get().getRedisService().client().getTopic("revenge:pwarp_gui");
        topic.addListener(String.class, (channel, msg) -> {
            plugin.getSLF4JLogger().info("Received cross-server PlayerWarp GUI Layout update. Reloading from MongoDB...");
            loadGuiData();
        });
    }

    private void loadGuiData() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            Collection<PlayerWarpGuiData> guiDatas = pwarpGuiRepository.findAll();
            if (guiDatas.isEmpty()) {
                guiData = new PlayerWarpGuiData();
                pwarpGuiRepository.save(guiData);
            } else {
                guiData = guiDatas.iterator().next();
            }
        });
    }

    public void savePwarp(PlayerWarp warp) {
        cachedPWarps.put(warp.getName().toLowerCase(), warp);
        pwarpRepository.save(warp);
        RevengeCoreAPI.get().getChannelService().publish("revenge:pwarp_update", new CrossPlayerWarpUpdateMessage(RevengeCoreAPI.get().getServerName(), warp.getName(), false));
    }

    public void deletePwarp(String name) {
        PlayerWarp warp = getPwarp(name);
        if (warp != null) {
            cachedPWarps.remove(name.toLowerCase());
            pwarpRepository.delete(warp);
            RevengeCoreAPI.get().getChannelService().publish("revenge:pwarp_update", new CrossPlayerWarpUpdateMessage(RevengeCoreAPI.get().getServerName(), name, true));
        }
    }

    public PlayerWarp getPwarp(String name) {
        return cachedPWarps.get(name.toLowerCase());
    }

    public Collection<PlayerWarp> getPwarps() {
        return cachedPWarps.values();
    }

    public Collection<PlayerWarp> getPwarpsByOwner(UUID ownerId) {
        return cachedPWarps.values().stream()
                .filter(w -> w.getOwnerUuid().equals(ownerId))
                .collect(Collectors.toList());
    }

    public PlayerWarpGuiData getGuiData() {
        return guiData;
    }

    public void saveGuiData() {
        if (guiData != null) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                pwarpGuiRepository.save(guiData);
                RevengeCoreAPI.get().getRedisService().client().getTopic("revenge:pwarp_gui").publish("update");
            });
        }
    }
}
