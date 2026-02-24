package me.mapacheee.revenge.channel;

public class CrossJoinMessage extends ValkeyMessage {
    private String playerName;
    private String serverName;
    private String parsedMessage;

    public CrossJoinMessage() {
        super("cross_join", "");
    }

    public CrossJoinMessage(String playerName, String serverName, String parsedMessage) {
        super("cross_join", serverName);
        this.playerName = playerName;
        this.serverName = serverName;
        this.parsedMessage = parsedMessage;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getParsedMessage() {
        return parsedMessage;
    }

    public void setParsedMessage(String parsedMessage) {
        this.parsedMessage = parsedMessage;
    }
}
