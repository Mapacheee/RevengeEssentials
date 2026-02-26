package me.mapacheee.revenge.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class InvseeHolder implements InventoryHolder {

    private final String targetName;
    private final String targetServer;
    
    private Inventory inventory;

    public InvseeHolder(String targetName, String targetServer) {
        this.targetName = targetName;
        this.targetServer = targetServer;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetServer() {
        return targetServer;
    }
}
