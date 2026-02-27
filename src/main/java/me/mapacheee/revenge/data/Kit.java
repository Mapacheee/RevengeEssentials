package me.mapacheee.revenge.data;

import com.google.gson.annotations.SerializedName;
import me.mapacheee.revenge.identifiable.Identifiable;
import org.bson.types.ObjectId;

import java.util.Collections;
import java.util.List;

public class Kit implements Identifiable<ObjectId> {

    @SerializedName("_id")
    private ObjectId id;
    private String name;
    private String displayName;
    private List<String> lore;
    private double cost;
    private long cooldownSeconds;
    private String inventoryBase64;
    private int guiSlot;
    private String permission;
    private String iconMaterial;

    public Kit() {
    }

    public Kit(String name) {
        this.name = name;
        this.displayName = "<green>" + name;
        this.lore = Collections.singletonList("<gray>Un kit por defecto.");
        this.cost = 0;
        this.cooldownSeconds = 86400;
        this.inventoryBase64 = "";
        this.guiSlot = -1;
        this.permission = "revenge.kit." + name.toLowerCase();
        this.iconMaterial = "CHEST";
    }

    @Override
    public ObjectId id() {
        return id;
    }

    @Override
    public void id(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getInventoryBase64() {
        return inventoryBase64;
    }

    public void setInventoryBase64(String inventoryBase64) {
        this.inventoryBase64 = inventoryBase64;
    }

    public int getGuiSlot() {
        return guiSlot;
    }

    public void setGuiSlot(int guiSlot) {
        this.guiSlot = guiSlot;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }

    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }
}
