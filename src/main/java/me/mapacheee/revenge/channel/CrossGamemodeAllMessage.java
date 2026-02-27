package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossGamemodeAllMessage extends ValkeyMessage {
    public String senderName;
    public String gameMode;

    public CrossGamemodeAllMessage(String serverId, String senderName, String gameMode) {
        super("essentials_gamemode_all", serverId);
        this.senderName = senderName;
        this.gameMode = gameMode;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("senderName", senderName);
        json.addProperty("gameMode", gameMode);
        return json;
    }
}
