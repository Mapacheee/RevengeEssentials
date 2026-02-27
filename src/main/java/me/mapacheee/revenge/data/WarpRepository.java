package me.mapacheee.revenge.data;

import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.repository.DefaultRepository;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import com.google.inject.Inject;

@Service
public class WarpRepository extends DefaultRepository<Warp> {

    @Inject
    public WarpRepository() {
        super(RevengeCoreAPI.get().getMongoService(), Warp.class, "revenge_warps");
    }
}
