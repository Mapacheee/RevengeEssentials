package me.mapacheee.revenge.data;

import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.repository.DefaultRepository;
import me.mapacheee.revenge.api.RevengeCoreAPI;

import com.google.inject.Inject;

@Service
public class PlayerRepository extends DefaultRepository<PlayerData> {

    @Inject
    public PlayerRepository() {
        super(RevengeCoreAPI.get().getMongoService(), PlayerData.class, "players");
    }
}
