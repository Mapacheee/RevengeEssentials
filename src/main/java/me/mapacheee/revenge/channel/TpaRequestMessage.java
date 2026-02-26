package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class TpaRequestMessage extends ValkeyMessage {

    public String senderUuid;
    public String senderName;
    public String targetUuid;
    public String targetName;
    public boolean tpaHere;
    public String senderServer;
    public String reqWorld;
    public double reqX;
    public double reqY;
    public double reqZ;
    public float reqYaw;
    public float reqPitch;

    public TpaRequestMessage(String serverId, String senderUuid, String senderName, String targetUuid,
            String targetName, boolean tpaHere, String senderServer, String reqWorld, double reqX, double reqY, double reqZ, float reqYaw, float reqPitch) {
        super("essentials_tpa_request", serverId);
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.tpaHere = tpaHere;
        this.senderServer = senderServer;
        this.reqWorld = reqWorld;
        this.reqX = reqX;
        this.reqY = reqY;
        this.reqZ = reqZ;
        this.reqYaw = reqYaw;
        this.reqPitch = reqPitch;
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
        json.addProperty("reqWorld", reqWorld);
        json.addProperty("reqX", reqX);
        json.addProperty("reqY", reqY);
        json.addProperty("reqZ", reqZ);
        json.addProperty("reqYaw", reqYaw);
        json.addProperty("reqPitch", reqPitch);
        return json;
    }
}
