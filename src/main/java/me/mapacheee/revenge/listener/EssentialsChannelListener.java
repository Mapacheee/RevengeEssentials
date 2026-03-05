package me.mapacheee.revenge.listener;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.TpaRequestMessage;
import me.mapacheee.revenge.channel.TpaResponseMessage;
import me.mapacheee.revenge.service.ChannelService;
import me.mapacheee.revenge.service.TeleportService;
import me.mapacheee.revenge.channel.CrossTeleportRequestMessage;
import me.mapacheee.revenge.channel.CrossTeleportResponseMessage;
import me.mapacheee.revenge.channel.CrossTeleportHereMessage;
import me.mapacheee.revenge.channel.CrossTeleportAllMessage;
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
                    
                    getChannelService().subscribe("revenge:tp_req", CrossTeleportRequestMessage.class,
                            this::handleCrossTeleportRequest, logger);
                    getChannelService().subscribe("revenge:tp_res", CrossTeleportResponseMessage.class,
                            this::handleCrossTeleportResponse, logger);
                    getChannelService().subscribe("revenge:tphere", CrossTeleportHereMessage.class,
                            this::handleCrossTeleportHere, logger);
                    getChannelService().subscribe("revenge:tphere_all", CrossTeleportAllMessage.class,
                            this::handleCrossTeleportAll, logger);
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

    private void handleCrossTeleportRequest(CrossTeleportRequestMessage msg) {
        teleportService.handleCrossTeleportRequest(msg.senderName, msg.targetName, msg.senderServer);
    }

    private void handleCrossTeleportResponse(CrossTeleportResponseMessage msg) {
        teleportService.handleCrossTeleportResponse(msg.senderName, msg.targetName, msg.found, msg.targetServer, msg.targetWorld, msg.x, msg.y, msg.z, msg.yaw, msg.pitch);
    }

    private void handleCrossTeleportHere(CrossTeleportHereMessage msg) {
        teleportService.handleCrossTeleportHere(msg.targetName, msg.destinationServer, msg.worldName, msg.x, msg.y, msg.z, msg.yaw, msg.pitch);
    }

    private void handleCrossTeleportAll(CrossTeleportAllMessage msg) {
        teleportService.handleCrossTeleportAll(msg.excludedPlayerName, msg.destinationServer, msg.worldName, msg.x, msg.y, msg.z, msg.yaw, msg.pitch);
    }
}
