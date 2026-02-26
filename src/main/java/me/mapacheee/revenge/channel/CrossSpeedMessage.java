package me.mapacheee.revenge.channel;

public class CrossSpeedMessage extends ValkeyMessage {
    private String senderName;
    private String targetName;
    private int speed;
    private String type;

    public CrossSpeedMessage() {
        super("SPEED", "unknown");
    }

    public CrossSpeedMessage(String senderName, String targetName, int speed, String type, String serverId) {
        super("SPEED", serverId);
        this.senderName = senderName;
        this.targetName = targetName;
        this.speed = speed;
        this.type = type;
    }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
