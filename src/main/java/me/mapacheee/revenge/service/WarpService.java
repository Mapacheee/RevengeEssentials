package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossWarpUpdateMessage;
import com.mongodb.client.model.Filters;
import me.mapacheee.revenge.data.Warp;
import me.mapacheee.revenge.data.WarpRepository;
import me.mapacheee.revenge.data.WarpGuiData;
import me.mapacheee.revenge.data.WarpGuiRepository;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.redisson.api.RTopic;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WarpService {

    private final WarpRepository warpRepository;
    private final WarpGuiRepository warpGuiRepository;
    private final Plugin plugin;
    
    private final Map<String, Warp> cachedWarps = new ConcurrentHashMap<>();
    private WarpGuiData guiData;

    @Inject
    public WarpService(WarpRepository warpRepository, WarpGuiRepository warpGuiRepository, Plugin plugin) {
        this.warpRepository = warpRepository;
        this.warpGuiRepository = warpGuiRepository;
        this.plugin = plugin;
    }

    @OnEnable
    public void onEnable() {
        loadGuiData();

        warpRepository.findAll().forEach(warp -> {
            cachedWarps.put(warp.getName().toLowerCase(), warp);
        });
        plugin.getSLF4JLogger().info("Warps globales cargados en caché: {}", cachedWarps.size());

        RevengeCoreAPI.get().getChannelService().subscribe("revenge:warp_update", CrossWarpUpdateMessage.class, msg -> {
            if (msg.serverName.equals(RevengeCoreAPI.get().getServerName())) return;
            
            if (msg.deleted) {
                cachedWarps.remove(msg.warpName.toLowerCase());
            } else {
                Warp warp = warpRepository.findOne(Filters.eq("name", msg.warpName));
                if (warp != null) {
                    cachedWarps.put(warp.getName().toLowerCase(), warp);
                }
            }
        }, plugin.getSLF4JLogger());

        RTopic topic = RevengeCoreAPI.get().getRedisService().client().getTopic("revenge:warp_gui");
        topic.addListener(String.class, (channel, msg) -> {
            plugin.getSLF4JLogger().info("Received cross-server Warp GUI Layout update. Reloading from MongoDB...");
            loadGuiData();
        });
    }

    private void loadGuiData() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            Collection<WarpGuiData> guiDatas = warpGuiRepository.findAll();
            if (guiDatas.isEmpty()) {
                guiData = new WarpGuiData();
                warpGuiRepository.save(guiData);
            } else {
                guiData = guiDatas.iterator().next();
            }
        });
    }

    public void saveWarp(Warp warp) {
        cachedWarps.put(warp.getName().toLowerCase(), warp);
        warpRepository.save(warp);
        RevengeCoreAPI.get().getChannelService().publish("revenge:warp_update", new CrossWarpUpdateMessage(RevengeCoreAPI.get().getServerName(), warp.getName(), false));
    }

    public void deleteWarp(String name) {
        Warp warp = getWarp(name);
        if (warp != null) {
            cachedWarps.remove(name.toLowerCase());
            warpRepository.delete(warp);
            RevengeCoreAPI.get().getChannelService().publish("revenge:warp_update", new CrossWarpUpdateMessage(RevengeCoreAPI.get().getServerName(), name, true));
        }
    }

    public Warp getWarp(String name) {
        return cachedWarps.get(name.toLowerCase());
    }

    public Collection<Warp> getWarps() {
        return cachedWarps.values();
    }

    public WarpGuiData getGuiData() {
        return guiData;
    }

    public void saveGuiData() {
        if (guiData != null) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                warpGuiRepository.save(guiData);
                RevengeCoreAPI.get().getRedisService().client().getTopic("revenge:warp_gui").publish("update");
            });
        }
    }
}
