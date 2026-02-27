package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossClearMessage extends ValkeyMessage {
    public String targetName;

    public CrossClearMessage(String serverId, String targetName) {
        super("essentials_clear", serverId);
        this.targetName = targetName;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("targetName", targetName);
        return json;
    }
}
