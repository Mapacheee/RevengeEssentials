package me.mapacheee.revenge.channel;

public class CrossInvseeResponseMessage extends ValkeyMessage {
    private String senderName;
    private String targetName;
    private String inventoryJson;

    public CrossInvseeResponseMessage() {
        super("INVSEE_RES", "unknown");
    }

    public CrossInvseeResponseMessage(String senderName, String targetName, String inventoryJson, String currentServer) {
        super("INVSEE_RES", currentServer);
        this.senderName = senderName;
        this.targetName = targetName;
        this.inventoryJson = inventoryJson;
    }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getInventoryJson() { return inventoryJson; }
    public void setInventoryJson(String inventoryJson) { this.inventoryJson = inventoryJson; }
}
