package me.mapacheee.revenge.channel;

public class CrossInvseeResponseMessage extends ValkeyMessage {
    private String senderName;
    private String targetName;
    private String inventoryJson;
    private String serverName;

    public CrossInvseeResponseMessage() {
        super("INVSEE_RES", "unknown");
    }

    public CrossInvseeResponseMessage(String senderName, String targetName, String inventoryJson, String serverName) {
        super("INVSEE_RES", serverName);
        this.senderName = senderName;
        this.targetName = targetName;
        this.inventoryJson = inventoryJson;
        this.serverName = serverName;
    }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getInventoryJson() { return inventoryJson; }
    public void setInventoryJson(String inventoryJson) { this.inventoryJson = inventoryJson; }
}
