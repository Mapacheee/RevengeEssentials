package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class TpaResponseMessage extends ValkeyMessage {

    public String senderUuid;
    public String targetUuid;
    public String targetName;
    public boolean accepted;
    public boolean tpaHere;
    public String targetServer;
    public String targetWorld;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public TpaResponseMessage(String serverId, String senderUuid, String targetUuid, String targetName,
            boolean accepted, boolean tpaHere,
            String targetServer, String targetWorld, double x, double y, double z, float yaw, float pitch) {
        super("essentials_tpa_response", serverId);
        this.senderUuid = senderUuid;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.accepted = accepted;
        this.tpaHere = tpaHere;
        this.targetServer = targetServer;
        this.targetWorld = targetWorld;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("senderUuid", senderUuid);
        json.addProperty("targetUuid", targetUuid);
        json.addProperty("targetName", targetName);
        json.addProperty("accepted", accepted);
        json.addProperty("tpaHere", tpaHere);
        json.addProperty("targetServer", targetServer);
        json.addProperty("targetWorld", targetWorld);
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("z", z);
        json.addProperty("yaw", yaw);
        json.addProperty("pitch", pitch);
        return json;
    }
}
