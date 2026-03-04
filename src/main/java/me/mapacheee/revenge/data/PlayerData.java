package me.mapacheee.revenge.data;

import me.mapacheee.revenge.identifiable.Identifiable;
import org.bson.types.ObjectId;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.annotations.SerializedName;

public class PlayerData implements Identifiable<ObjectId> {

    @SerializedName("_id")
    private ObjectId id;
    private String uuid;
    private String name;
    private int xp;
    private String lastLocation;
    private String inventory;
    private Boolean queuedGodmode;
    private String queuedGamemode;
    private Boolean queuedHeal;
    private Map<String, Long> kitCooldowns;
    private Map<String, Integer> pendingKits;
    private int kills;
    private int deaths;
    private List<String> offlineMessages;

    public PlayerData() {
    }

    public PlayerData(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.xp = 0;
        this.lastLocation = "{}";
        this.inventory = "{}";
        this.kitCooldowns = new ConcurrentHashMap<>();
        this.pendingKits = new ConcurrentHashMap<>();
        this.deaths = 0;
        this.kills = 0;
        this.offlineMessages = new ArrayList<>();
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

    public Boolean getQueuedGodmode() {
        return queuedGodmode;
    }

    public void setQueuedGodmode(Boolean queuedGodmode) {
        this.queuedGodmode = queuedGodmode;
    }

    public String getQueuedGamemode() {
        return queuedGamemode;
    }

    public void setQueuedGamemode(String queuedGamemode) {
        this.queuedGamemode = queuedGamemode;
    }

    public Boolean getQueuedHeal() {
        return queuedHeal;
    }

    public void setQueuedHeal(Boolean queuedHeal) {
        this.queuedHeal = queuedHeal;
    }

    public Map<String, Long> getKitCooldowns() {
        if (kitCooldowns == null) kitCooldowns = new ConcurrentHashMap<>();
        return kitCooldowns;
    }

    public void setKitCooldowns(Map<String, Long> kitCooldowns) {
        this.kitCooldowns = kitCooldowns;
    }

    public Map<String, Integer> getPendingKits() {
        if (pendingKits == null) pendingKits = new ConcurrentHashMap<>();
        return pendingKits;
    }

    public void setPendingKits(Map<String, Integer> pendingKits) {
        this.pendingKits = pendingKits;
    }

    public List<String> getOfflineMessages() {
        if (offlineMessages == null) offlineMessages = new ArrayList<>();
        return offlineMessages;
    }

    public void setOfflineMessages(List<String> offlineMessages) {
        this.offlineMessages = offlineMessages;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
}
