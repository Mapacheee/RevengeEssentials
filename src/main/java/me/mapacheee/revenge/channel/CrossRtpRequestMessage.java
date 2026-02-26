package me.mapacheee.revenge.channel;

public class CrossRtpRequestMessage extends ValkeyMessage {

    public String uuid;
    public String name;
    public String currentServer;
    public String targetServer;
    public String targetWorld;
    public int radius;

    public CrossRtpRequestMessage() {
        super("revenge:rtp:request", "");
    }

    public CrossRtpRequestMessage(String uuid, String name, String currentServer, String targetServer, String targetWorld, int radius) {
        super("revenge:rtp:request", currentServer);
        this.uuid = uuid;
        this.name = name;
        this.currentServer = currentServer;
        this.targetServer = targetServer;
        this.targetWorld = targetWorld;
        this.radius = radius;
    }
}
