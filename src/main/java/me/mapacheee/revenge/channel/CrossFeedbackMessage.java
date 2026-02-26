package me.mapacheee.revenge.channel;

import me.mapacheee.revenge.api.RevengeCoreAPI;

public class CrossFeedbackMessage extends ValkeyMessage {
    private String originalSender;
    private String serializedMessage;

    public CrossFeedbackMessage() {
        super("feedback", RevengeCoreAPI.get().getServerName());
    }

    public CrossFeedbackMessage(String originalSender, String serializedMessage) {
        super("feedback", RevengeCoreAPI.get().getServerName());
        this.originalSender = originalSender;
        this.serializedMessage = serializedMessage;
    }

    public String getOriginalSender() {
        return originalSender;
    }

    public String getSerializedMessage() {
        return serializedMessage;
    }
}
