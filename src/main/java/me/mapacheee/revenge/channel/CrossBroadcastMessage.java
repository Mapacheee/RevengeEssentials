package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossBroadcastMessage extends ValkeyMessage {
    public String message;

    public CrossBroadcastMessage(String serverId, String message) {
        super("essentials_broadcast", serverId);
        this.message = message;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("message", message);
        return json;
    }
}
