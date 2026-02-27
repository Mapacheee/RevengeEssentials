package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossKitGiveAllMessage extends ValkeyMessage {
    public String kitName;
    public String senderName;

    public CrossKitGiveAllMessage(String serverId, String kitName, String senderName) {
        super("essentials_kit_give_all", serverId);
        this.kitName = kitName;
        this.senderName = senderName;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("kitName", kitName);
        json.addProperty("senderName", senderName);
        return json;
    }
}
