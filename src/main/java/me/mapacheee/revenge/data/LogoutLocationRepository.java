package me.mapacheee.revenge.data;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class LogoutLocationRepository {

    private MongoCollection<Document> collection;
    private final Gson gson = new Gson();

    private MongoCollection<Document> getCollection() {
        if (collection == null) {
            collection = RevengeCoreAPI.get().getMongoService().database().getCollection("essentials_logout_locations");
        }
        return collection;
    }

    public LogoutLocation findOne(Bson filter) {
        Document doc = getCollection().find(filter).first();
        return doc != null ? gson.fromJson(doc.toJson(), LogoutLocation.class) : null;
    }

    public Collection<LogoutLocation> find(Bson filter) {
        List<LogoutLocation> results = new ArrayList<>();
        for (Document doc : getCollection().find(filter)) {
            results.add(gson.fromJson(doc.toJson(), LogoutLocation.class));
        }
        return results;
    }

    public void save(LogoutLocation model) {
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
