package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossGodModeAllMessage extends ValkeyMessage {
    public String senderName;
    public Boolean enabled;

    public CrossGodModeAllMessage(String serverId, String senderName, Boolean enabled) {
        super("essentials_godmode_all", serverId);
        this.senderName = senderName;
        this.enabled = enabled;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("senderName", senderName);
        if (enabled != null) {
            json.addProperty("enabled", enabled);
        }
        return json;
    }
}
