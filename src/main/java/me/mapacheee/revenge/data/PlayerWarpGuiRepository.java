package me.mapacheee.revenge.data;

import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.repository.DefaultRepository;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import com.google.inject.Inject;

@Service
public class PlayerWarpGuiRepository extends DefaultRepository<PlayerWarpGuiData> {

    @Inject
    public PlayerWarpGuiRepository() {
        super(RevengeCoreAPI.get().getMongoService(), PlayerWarpGuiData.class, "revenge_pwarp_gui");
    }
}
