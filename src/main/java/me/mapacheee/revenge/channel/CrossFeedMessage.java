package me.mapacheee.revenge.channel;

import me.mapacheee.revenge.api.RevengeCoreAPI;

public class CrossFeedMessage extends ValkeyMessage {
    private String senderName;
    private String targetName;

    public CrossFeedMessage() {
        super("feed", RevengeCoreAPI.get().getServerName());
    }

    public CrossFeedMessage(String senderName, String targetName) {
        super("feed", RevengeCoreAPI.get().getServerName());
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
