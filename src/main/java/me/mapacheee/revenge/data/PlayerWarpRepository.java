package me.mapacheee.revenge.data;

import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.repository.DefaultRepository;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import com.google.inject.Inject;

@Service
public class PlayerWarpRepository extends DefaultRepository<PlayerWarp> {

    @Inject
    public PlayerWarpRepository() {
        super(RevengeCoreAPI.get().getMongoService(), PlayerWarp.class, "revenge_pwarps");
    }
}
