package me.mapacheee.revenge.data;

import com.google.gson.annotations.SerializedName;
import me.mapacheee.revenge.identifiable.Identifiable;
import org.bson.types.ObjectId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerWarpGuiData implements Identifiable<ObjectId> {

    @SerializedName("_id")
    private ObjectId id;

    private Map<Integer, String> decorativeItems = new ConcurrentHashMap<>();

    public PlayerWarpGuiData() {
    }

    @Override
    public ObjectId id() {
        return id;
    }

    @Override
    public void id(ObjectId id) {
        this.id = id;
    }

    public Map<Integer, String> getDecorativeItems() {
        return decorativeItems;
    }

    public void setDecorativeItems(Map<Integer, String> decorativeItems) {
        this.decorativeItems = decorativeItems;
    }
}
