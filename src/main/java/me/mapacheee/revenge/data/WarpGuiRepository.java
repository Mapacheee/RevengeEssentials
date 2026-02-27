package me.mapacheee.revenge.data;

import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.repository.DefaultRepository;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import com.google.inject.Inject;

@Service
public class WarpGuiRepository extends DefaultRepository<WarpGuiData> {

    @Inject
    public WarpGuiRepository() {
        super(RevengeCoreAPI.get().getMongoService(), WarpGuiData.class, "revenge_warp_gui");
    }
}
