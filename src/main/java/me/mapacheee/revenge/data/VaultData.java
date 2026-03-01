package me.mapacheee.revenge.data;

import com.google.gson.annotations.SerializedName;
import org.bson.types.ObjectId;

public class VaultData {

    @SerializedName("_id")
    private ObjectId id;
    private String uuid;
    private int page;
    private String contentsBase64;

    public VaultData() {}

    public VaultData(String uuid, int page, String contentsBase64) {
        this.uuid = uuid;
        this.page = page;
        this.contentsBase64 = contentsBase64;
    }

    public ObjectId id() { return id; }
    public void id(ObjectId id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public String getContentsBase64() { return contentsBase64; }
    public void setContentsBase64(String contentsBase64) { this.contentsBase64 = contentsBase64; }
}
