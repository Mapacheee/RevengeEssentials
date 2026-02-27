package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossTeleportAllMessage extends ValkeyMessage {
    public String excludedPlayerName; 
    public String destinationServer;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public String worldName;

    public CrossTeleportAllMessage(String serverId, String excludedPlayerName, String destinationServer, double x, double y, double z, float yaw, float pitch, String worldName) {
        super("essentials_tphere_all", serverId);
        this.excludedPlayerName = excludedPlayerName;
        this.destinationServer = destinationServer;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.worldName = worldName;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("excludedPlayerName", excludedPlayerName);
        json.addProperty("destinationServer", destinationServer);
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("z", z);
        json.addProperty("yaw", yaw);
        json.addProperty("pitch", pitch);
        json.addProperty("worldName", worldName);
        return json;
    }
}
