package me.mapacheee.revenge.data;

import me.mapacheee.revenge.identifiable.Identifiable;
import org.bson.types.ObjectId;
import com.google.gson.annotations.SerializedName;

public class PlayerData implements Identifiable<ObjectId> {

    @SerializedName("_id")
    private ObjectId id;
    private String uuid;
    private String name;
    private int xp;
    private String lastLocation;
    private String inventory;

    public PlayerData() {
    }

    public PlayerData(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.xp = 0;
        this.lastLocation = "{}";
        this.inventory = "{}";
    }

    @Override
    public ObjectId id() {
        return id;
    }

    @Override
    public void id(ObjectId id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public String getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(String lastLocation) {
        this.lastLocation = lastLocation;
    }

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }
}
