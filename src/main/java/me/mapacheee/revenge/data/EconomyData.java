package me.mapacheee.revenge.data;

import com.google.gson.annotations.SerializedName;
import org.bson.types.ObjectId;
import me.mapacheee.revenge.identifiable.Identifiable;

public class EconomyData implements Identifiable<ObjectId> {

    @SerializedName("_id")
    private ObjectId id;
    private String uuid;
    private String name;
    private double balance;

    public EconomyData() {}

    public EconomyData(String uuid, String name, double balance) {
        this.uuid = uuid;
        this.name = name;
        this.balance = balance;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public ObjectId id() {
        return this.id;
    }

    @Override
    public void id(ObjectId id) {
        this.id = id;
    }
}
