package me.mapacheee.revenge.data;

import me.mapacheee.revenge.channel.ValkeyMessage;

public class PendingTeleportMessage extends ValkeyMessage {

    public String uuid;
    public String targetServer;
    public String world;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public PendingTeleportMessage(String serverId, String uuid, String targetServer, String world, double x, double y,
            double z, float yaw, float pitch) {
        super("pending_teleport", serverId);
        this.uuid = uuid;
        this.targetServer = targetServer;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
