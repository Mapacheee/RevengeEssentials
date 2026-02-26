package me.mapacheee.revenge.listener;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.TpaRequestMessage;
import me.mapacheee.revenge.channel.TpaResponseMessage;
import me.mapacheee.revenge.service.ChannelService;
import me.mapacheee.revenge.service.TeleportService;
import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

@Service
public class EssentialsChannelListener {

    private final TeleportService teleportService;
    private ChannelService channelService;
    private final Plugin plugin;

    @Inject
    public EssentialsChannelListener(TeleportService teleportService, Plugin plugin) {
        this.teleportService = teleportService;
        this.plugin = plugin;
    }

    private ChannelService getChannelService() {
        if (channelService == null) {
            channelService = RevengeCoreAPI.get().getChannelService();
        }
        return channelService;
    }

    @OnEnable
    void subscribeChannels(Logger logger) {
        org.bukkit.Bukkit.getAsyncScheduler().runDelayed(plugin,
                task -> {
                    getChannelService().subscribe("essentials:tpa:request", TpaRequestMessage.class,
                            this::handleTpaRequest, logger);
                    getChannelService().subscribe("essentials:tpa:response", TpaResponseMessage.class,
                            this::handleTpaResponse, logger);
                }, 2, TimeUnit.SECONDS);
    }

    private void handleTpaRequest(TpaRequestMessage req) {
        teleportService.handleIncomingTpaRequest(req.senderUuid, req.senderName, req.targetName, req.tpaHere,
                req.senderServer, req.reqWorld, req.reqX, req.reqY, req.reqZ, req.reqYaw, req.reqPitch);
    }

    private void handleTpaResponse(TpaResponseMessage resp) {
        teleportService.handleTpaResponse(resp.senderUuid, resp.targetName, resp.accepted, resp.tpaHere, resp.targetServer,
                resp.targetWorld, resp.x, resp.y, resp.z, resp.yaw, resp.pitch);
    }
}
