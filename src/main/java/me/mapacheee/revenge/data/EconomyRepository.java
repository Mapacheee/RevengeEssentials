package me.mapacheee.revenge.data;

import com.google.inject.Inject;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.config.Config;
import me.mapacheee.revenge.repository.DefaultRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@Service
public class EconomyRepository extends DefaultRepository<EconomyData> {

    private final Container<Config> config;
    private final Plugin plugin;

    @Inject
    public EconomyRepository(Container<Config> config, Plugin plugin) {
        super(RevengeCoreAPI.get().getMongoService(), EconomyData.class, "revenge_economy");
        this.config = config;
        this.plugin = plugin;
    }

    @OnEnable
    public void setupIndexes() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                this.collection.createIndex(Indexes.ascending("uuid"), new IndexOptions().unique(true));
                this.collection.createIndex(Indexes.text("name"));
            } catch (Exception e) {
                plugin.getSLF4JLogger().warn("Failed to create EconomyRepository indexes: {}", e.getMessage());
            }
        });
    }
}
