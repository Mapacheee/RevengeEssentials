package me.mapacheee.revenge.data;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import org.bson.Document;
import org.bson.conversions.Bson;

@Service
public class BackRepository {

    private MongoCollection<Document> collection;
    private final Gson gson = new Gson();

    private MongoCollection<Document> getCollection() {
        if (collection == null) {
            collection = RevengeCoreAPI.get().getMongoService().database().getCollection("essentials_backs");
        }
        return collection;
    }

    public BackData findOne(Bson filter) {
        Document doc = getCollection().find(filter).first();
        return doc != null ? gson.fromJson(doc.toJson(), BackData.class) : null;
    }

    public void save(BackData model) {
        Document doc = Document.parse(gson.toJson(model));
        if (model.id() == null) {
            getCollection().insertOne(doc);
            model.id(doc.getObjectId("_id"));
        } else {
            getCollection().replaceOne(Filters.eq("_id", model.id()), doc);
        }
    }

    public void delete(Bson filter) {
        getCollection().deleteMany(filter);
    }
}
