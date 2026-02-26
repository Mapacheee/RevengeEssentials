package me.mapacheee.revenge.channel;

import me.mapacheee.revenge.api.RevengeCoreAPI;

public class CrossGamemodeMessage extends ValkeyMessage {
    private String senderName;
    private String targetName;
    private String gameMode;

    public CrossGamemodeMessage() {
        super("gamemode", RevengeCoreAPI.get().getServerName());
    }

    public CrossGamemodeMessage(String senderName, String targetName, String gameMode) {
        super("gamemode", RevengeCoreAPI.get().getServerName());
        this.senderName = senderName;
        this.targetName = targetName;
        this.gameMode = gameMode;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getGameMode() {
        return gameMode;
    }
}
