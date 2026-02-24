package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class TpaRequestMessage extends ValkeyMessage {

    public String senderUuid;
    public String senderName;
    public String targetUuid;
    public String targetName;
    public boolean tpaHere;
    public String senderServer;

    public TpaRequestMessage(String serverId, String senderUuid, String senderName, String targetUuid,
            String targetName, boolean tpaHere, String senderServer) {
        super("essentials_tpa_request", serverId);
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.tpaHere = tpaHere;
        this.senderServer = senderServer;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("senderUuid", senderUuid);
        json.addProperty("senderName", senderName);
        json.addProperty("targetUuid", targetUuid);
        json.addProperty("targetName", targetName);
        json.addProperty("tpaHere", tpaHere);
        json.addProperty("senderServer", senderServer);
        return json;
    }
}
