package me.mapacheee.revenge.channel;

public class CrossRtpResponseMessage extends ValkeyMessage {

    public String uuid;
    public String sourceServer;
    public String targetServer;
    public String targetWorld;
    public double x;
    public double y;
    public double z;
    public boolean success;

    public CrossRtpResponseMessage() {
        super("revenge:rtp:response", "");
    }

    public CrossRtpResponseMessage(String uuid, String sourceServer, String targetServer, String targetWorld, double x, double y, double z, boolean success) {
        super("revenge:rtp:response", targetServer);
        this.uuid = uuid;
        this.sourceServer = sourceServer;
        this.targetServer = targetServer;
        this.targetWorld = targetWorld;
        this.x = x;
        this.y = y;
        this.z = z;
        this.success = success;
    }
}
