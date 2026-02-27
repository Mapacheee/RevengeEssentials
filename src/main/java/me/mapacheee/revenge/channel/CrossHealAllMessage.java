package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossHealAllMessage extends ValkeyMessage {
    public String senderName;

    public CrossHealAllMessage(String serverId, String senderName) {
        super("essentials_heal_all", serverId);
        this.senderName = senderName;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("senderName", senderName);
        return json;
    }
}
