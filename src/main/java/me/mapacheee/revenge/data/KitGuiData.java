package me.mapacheee.revenge.data;

import com.google.gson.annotations.SerializedName;
import me.mapacheee.revenge.identifiable.Identifiable;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;

public class KitGuiData implements Identifiable<ObjectId> {

    @SerializedName("_id")
    private ObjectId id;
    private String guiId;
    private Map<Integer, String> kitSlots;
    private Map<Integer, String> decorationSlots;

    public KitGuiData() {
        this("global");
    }

    public KitGuiData(String guiId) {
        this.guiId = guiId;
        this.kitSlots = new HashMap<>();
        this.decorationSlots = new HashMap<>();
    }

    @Override
    public ObjectId id() {
        return id;
    }

    @Override
    public void id(ObjectId id) {
        this.id = id;
    }

    public String getGuiId() {
        return guiId;
    }

    public void setGuiId(String guiId) {
        this.guiId = guiId;
    }

    public Map<Integer, String> getKitSlots() {
        return kitSlots;
    }

    public void setKitSlots(Map<Integer, String> kitSlots) {
        this.kitSlots = kitSlots;
    }

    public Map<Integer, String> getDecorationSlots() {
        return decorationSlots;
    }

    public void setDecorationSlots(Map<Integer, String> decorationSlots) {
        this.decorationSlots = decorationSlots;
    }
}
