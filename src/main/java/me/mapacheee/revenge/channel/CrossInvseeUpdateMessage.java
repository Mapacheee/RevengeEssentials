package me.mapacheee.revenge.channel;

public class CrossInvseeUpdateMessage extends ValkeyMessage {
    private String targetName;
    private String targetServer;
    private String inventoryJson;

    public CrossInvseeUpdateMessage() {
        super("INVSEE_UPD", "unknown");
    }

    public CrossInvseeUpdateMessage(String targetName, String targetServer, String inventoryJson, String currentServer) {
        super("INVSEE_UPD", currentServer);
        this.targetName = targetName;
        this.targetServer = targetServer;
        this.inventoryJson = inventoryJson;
    }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getTargetServer() { return targetServer; }
    public void setTargetServer(String targetServer) { this.targetServer = targetServer; }

    public String getInventoryJson() { return inventoryJson; }
    public void setInventoryJson(String inventoryJson) { this.inventoryJson = inventoryJson; }
}
