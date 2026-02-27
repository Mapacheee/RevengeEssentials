package me.mapacheee.revenge.channel;

import me.mapacheee.revenge.api.RevengeCoreAPI;

public class CrossWarpUpdateMessage extends ValkeyMessage {

    public String serverName;
    public String warpName;
    public boolean deleted;

    public CrossWarpUpdateMessage() {
        super("warp_update", RevengeCoreAPI.get().getServerName());
    }

    public CrossWarpUpdateMessage(String serverName, String warpName, boolean deleted) {
        super("warp_update", serverName);
        this.serverName = serverName;
        this.warpName = warpName;
        this.deleted = deleted;
    }
}
