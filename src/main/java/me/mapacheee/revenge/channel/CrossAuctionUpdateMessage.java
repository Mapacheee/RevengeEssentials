package me.mapacheee.revenge.channel;

public class CrossAuctionUpdateMessage extends ValkeyMessage {

    private String auctionId;
    private String action;

    public CrossAuctionUpdateMessage() {
        super("ah:update", "");
    }

    public CrossAuctionUpdateMessage(String sourceServer, String auctionId, String action) {
        super("ah:update", sourceServer);
        this.auctionId = auctionId;
        this.action = action;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
