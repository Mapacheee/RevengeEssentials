package me.mapacheee.revenge.channel;

import com.google.gson.JsonObject;
import me.mapacheee.revenge.api.RevengeCoreAPI;

public class CrossTradeMessage extends ValkeyMessage {

    public enum Action {
        REQUEST, ACCEPT, DENY, CANCEL, COMPLETED,
        SYNC_SLOT, SYNC_MONEY, SYNC_CONFIRM,
        READY_TO_CLOSE
    }

    public String action;
    public String senderUuid;
    public String targetUuid;
    public String senderName;
    public String targetName;
    public int slot;
    public String itemData;
    public double amount;
    public boolean state;

    public CrossTradeMessage() {
        super("trade", RevengeCoreAPI.get().getServerName());
    }

    public CrossTradeMessage(Action action, String serverId, String senderUuid, String targetUuid, String senderName, String targetName) {
        super("trade", serverId);
        this.action = action.name();
        this.senderUuid = senderUuid;
        this.targetUuid = targetUuid;
        this.senderName = senderName;
        this.targetName = targetName;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("action", action);
        json.addProperty("senderUuid", senderUuid);
        json.addProperty("targetUuid", targetUuid);
        json.addProperty("senderName", senderName);
        json.addProperty("targetName", targetName);
        json.addProperty("slot", slot);
        json.addProperty("itemData", itemData);
        json.addProperty("amount", amount);
        json.addProperty("state", state);
        return json;
    }
}
