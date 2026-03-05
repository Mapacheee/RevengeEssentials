package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossTeleportResponseMessage extends ValkeyMessage {
    public String senderName;
    public String targetName; 
    public boolean found;
    public String targetServer;
    public String targetWorld;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public CrossTeleportResponseMessage(String serverId, String senderName, String targetName, boolean found, String targetServer, String targetWorld, double x, double y, double z, float yaw, float pitch) {
        super("essentials_tp_res", serverId);
        this.senderName = senderName;
        this.targetName = targetName;
        this.found = found;
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
        json.addProperty("senderName", senderName);
        json.addProperty("targetName", targetName);
        json.addProperty("found", found);
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
