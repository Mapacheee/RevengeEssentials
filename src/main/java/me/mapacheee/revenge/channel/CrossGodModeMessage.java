package me.mapacheee.revenge.channel;

import me.mapacheee.revenge.api.RevengeCoreAPI;

public class CrossGodModeMessage extends ValkeyMessage {
    private String senderName;
    private String targetName;
    private Boolean enabled;

    public CrossGodModeMessage() {
        super("godmode", RevengeCoreAPI.get().getServerName());
    }

    public CrossGodModeMessage(String senderName, String targetName, Boolean enabled) {
        super("godmode", RevengeCoreAPI.get().getServerName());
        this.senderName = senderName;
        this.targetName = targetName;
        this.enabled = enabled;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getTargetName() {
        return targetName;
    }

    public Boolean getEnabled() {
        return enabled;
    }
}
