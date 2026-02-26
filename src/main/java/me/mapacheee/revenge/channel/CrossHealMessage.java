package me.mapacheee.revenge.channel;

import me.mapacheee.revenge.api.RevengeCoreAPI;

public class CrossHealMessage extends ValkeyMessage {
    private String senderName;
    private String targetName;

    public CrossHealMessage() {
        super("heal", RevengeCoreAPI.get().getServerName());
    }

    public CrossHealMessage(String senderName, String targetName) {
        super("heal", RevengeCoreAPI.get().getServerName());
        this.senderName = senderName;
        this.targetName = targetName;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getTargetName() {
        return targetName;
    }
}
