package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossSpeedAllMessage extends ValkeyMessage {
    public String senderName;
    public int value;
    public String type;

    public CrossSpeedAllMessage(String serverId, String senderName, int value, String type) {
        super("essentials_speed_all", serverId);
        this.senderName = senderName;
        this.value = value;
        this.type = type;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("senderName", senderName);
        json.addProperty("value", value);
        json.addProperty("type", type);
        return json;
    }
}
