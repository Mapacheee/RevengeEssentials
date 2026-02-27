package me.mapacheee.revenge.data;

import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.repository.DefaultRepository;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import com.google.inject.Inject;

@Service
public class KitRepository extends DefaultRepository<Kit> {

    @Inject
    public KitRepository() {
        super(RevengeCoreAPI.get().getMongoService(), Kit.class, "revenge_kits");
    }
}
