package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossFeedAllMessage extends ValkeyMessage {
    public String senderName;

    public CrossFeedAllMessage(String serverId, String senderName) {
        super("essentials_feed_all", serverId);
        this.senderName = senderName;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("senderName", senderName);
        return json;
    }
}
