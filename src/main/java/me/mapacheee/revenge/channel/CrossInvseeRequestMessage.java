package me.mapacheee.revenge.channel;

public class CrossInvseeRequestMessage extends ValkeyMessage {
    private String senderName;
    private String targetName;
    private String senderServer;

    public CrossInvseeRequestMessage() {
        super("INVSEE_REQ", "unknown");
    }

    public CrossInvseeRequestMessage(String senderName, String targetName, String senderServer) {
        super("INVSEE_REQ", senderServer);
        this.senderName = senderName;
        this.targetName = targetName;
        this.senderServer = senderServer;
    }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getSenderServer() { return senderServer; }
    public void setSenderServer(String senderServer) { this.senderServer = senderServer; }
}
