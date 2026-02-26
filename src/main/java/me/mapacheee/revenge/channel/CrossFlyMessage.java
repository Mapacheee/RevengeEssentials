package me.mapacheee.revenge.channel;

public class CrossFlyMessage extends ValkeyMessage {
    private String senderName;
    private String targetName;
    private Boolean state;

    public CrossFlyMessage() {
        super("FLY", "unknown");
    }

    public CrossFlyMessage(String senderName, String targetName, Boolean state, String serverId) {
        super("FLY", serverId);
        this.senderName = senderName;
        this.targetName = targetName;
        this.state = state;
    }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public Boolean getState() { return state; }
    public void setState(Boolean state) { this.state = state; }
}
