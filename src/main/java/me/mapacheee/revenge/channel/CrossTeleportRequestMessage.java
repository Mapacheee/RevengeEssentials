package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossTeleportRequestMessage extends ValkeyMessage {
    public String senderName;
    public String targetName; 
    public String senderServer;

    public CrossTeleportRequestMessage(String serverId, String senderName, String targetName, String senderServer) {
        super("essentials_tp_req", serverId);
        this.senderName = senderName;
        this.targetName = targetName;
        this.senderServer = senderServer;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("senderName", senderName);
        json.addProperty("targetName", targetName);
        json.addProperty("senderServer", senderServer);
        return json;
    }
}
