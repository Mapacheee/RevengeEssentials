package me.mapacheee.revenge.data;

import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.repository.DefaultRepository;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoCommandException;
import com.google.inject.Inject;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PlayerRepository extends DefaultRepository<PlayerData> {

    private static final Logger logger = LoggerFactory.getLogger(PlayerRepository.class);

    @Inject
    public PlayerRepository() {
        super(RevengeCoreAPI.get().getMongoService(), PlayerData.class, "players");

        try {
            collection.createIndex(Indexes.ascending("uuid"), new IndexOptions().unique(true));
            collection.createIndex(Indexes.ascending("name"));
        } catch (MongoCommandException | DuplicateKeyException e) {
            logger.error("[RevengeEssentials] Could not enforce unique UUID index in MongoDB. You may have duplicate player entries: {}", e.getMessage());
        }
    }
}
