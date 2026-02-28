package me.mapacheee.revenge.channel;

public class CrossEconomyUpdateMessage extends ValkeyMessage {

    private String playerUuid;
    private String playerName;
    private double newBalance;

    public CrossEconomyUpdateMessage(String sourceServer, String playerUuid, String playerName, double newBalance) {
        super("economy_update", sourceServer);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.newBalance = newBalance;
    }

    public String playerUuid() {
        return playerUuid;
    }

    public String playerName() {
        return playerName;
    }

    public double newBalance() {
        return newBalance;
    }
}
