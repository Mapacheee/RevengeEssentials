package me.mapacheee.revenge.data;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.repository.DefaultRepository;

@Service
public class AuctionRepository extends DefaultRepository<AuctionItem> {

    @Inject
    public AuctionRepository() {
        super(RevengeCoreAPI.get().getMongoService(), AuctionItem.class, "revenge_auctions");
    }
}
