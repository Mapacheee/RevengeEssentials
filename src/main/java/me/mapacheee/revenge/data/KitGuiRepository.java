package me.mapacheee.revenge.data;

import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.repository.DefaultRepository;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import com.google.inject.Inject;

@Service
public class KitGuiRepository extends DefaultRepository<KitGuiData> {

    @Inject
    public KitGuiRepository() {
        super(RevengeCoreAPI.get().getMongoService(), KitGuiData.class, "revenge_kit_gui");
    }
}
