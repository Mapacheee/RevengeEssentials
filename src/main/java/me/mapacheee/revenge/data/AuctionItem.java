package me.mapacheee.revenge.data;

import com.google.gson.annotations.SerializedName;
import me.mapacheee.revenge.identifiable.Identifiable;
import org.bson.types.ObjectId;

import java.util.UUID;

public class AuctionItem implements Identifiable<ObjectId> {

    @SerializedName("_id")
    private ObjectId id;
    
    private UUID sellerUuid;
    private String sellerName;
    private double price;
    private String itemBase64;
    private long dateAdded;

    public AuctionItem() {}

    public AuctionItem(UUID sellerUuid, String sellerName, double price, String itemBase64, long dateAdded) {
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.price = price;
        this.itemBase64 = itemBase64;
        this.dateAdded = dateAdded;
    }

    @Override
    public ObjectId id() {
        return id;
    }

    @Override
    public void id(ObjectId id) {
        this.id = id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public void setSellerUuid(UUID sellerUuid) {
        this.sellerUuid = sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getItemBase64() {
        return itemBase64;
    }

    public void setItemBase64(String itemBase64) {
        this.itemBase64 = itemBase64;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }
}
