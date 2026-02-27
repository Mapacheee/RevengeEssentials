package me.mapacheee.revenge.channel;

import me.mapacheee.revenge.api.RevengeCoreAPI;

public class CrossPlayerWarpUpdateMessage extends ValkeyMessage {

    public String serverName;
    public String warpName;
    public boolean deleted;

    public CrossPlayerWarpUpdateMessage() {
        super("pwarp_update", RevengeCoreAPI.get().getServerName());
    }

    public CrossPlayerWarpUpdateMessage(String serverName, String warpName, boolean deleted) {
        super("pwarp_update", serverName);
        this.serverName = serverName;
        this.warpName = warpName;
        this.deleted = deleted;
    }
}
