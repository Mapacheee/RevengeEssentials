package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;

public class CrossMaintenanceMessage extends ValkeyMessage {
    public int countdown;

    public CrossMaintenanceMessage(String serverId, int countdown) {
        super("essentials_maintenance", serverId);
        this.countdown = countdown;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("countdown", countdown);
        return json;
    }
}
