package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.mongodb.client.model.Filters;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossAuctionUpdateMessage;
import me.mapacheee.revenge.data.AuctionItem;
import me.mapacheee.revenge.data.AuctionRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final EconomyService economyService;
    private final InventorySyncService inventorySyncService;
    private final Plugin plugin;
    private ChannelService channelService;

    private final ConcurrentMap<String, AuctionItem> activeAuctions = new ConcurrentHashMap<>();

    @Inject
    public AuctionService(AuctionRepository auctionRepository, EconomyService economyService, InventorySyncService inventorySyncService, Plugin plugin) {
        this.auctionRepository = auctionRepository;
        this.economyService = economyService;
        this.inventorySyncService = inventorySyncService;
        this.plugin = plugin;
    }

    private ChannelService getChannelService() {
        if (channelService == null) {
            channelService = RevengeCoreAPI.get().getChannelService();
        }
        return channelService;
    }

    private String getServerName() {
        return RevengeCoreAPI.get().getServerName();
    }

    @OnEnable
    public void onEnable() {
        plugin.getSLF4JLogger().info("Loading Auction House items from MongoDB...");
        CompletableFuture.runAsync(() -> {
            auctionRepository.findAll().forEach(item -> activeAuctions.put(item.id().toHexString(), item));
            plugin.getSLF4JLogger().info("Loaded {} active auctions.", activeAuctions.size());
        }).exceptionally(e -> {
            plugin.getSLF4JLogger().error("Failed to load auctions", e);
            return null;
        });

        getChannelService().subscribe("revenge:ah:update", CrossAuctionUpdateMessage.class, this::onAuctionUpdate, plugin.getSLF4JLogger());
    }

    public List<AuctionItem> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public Optional<AuctionItem> getAuction(String hexId) {
        return Optional.ofNullable(activeAuctions.get(hexId));
    }

    public CompletableFuture<Boolean> listItem(Player seller, ItemStack item, double price) {
        return CompletableFuture.supplyAsync(() -> {
            String base64 = inventorySyncService.serializeItems(new ItemStack[]{item});
            AuctionItem auction = new AuctionItem(seller.getUniqueId(), seller.getName(), price, base64, System.currentTimeMillis());
            auctionRepository.save(auction);
            return auction;
        }).thenApply(savedAuction -> {
            if (savedAuction != null && savedAuction.id() != null) {
                String hex = savedAuction.id().toHexString();
                activeAuctions.put(hex, savedAuction);
                
                CrossAuctionUpdateMessage msg = new CrossAuctionUpdateMessage(getServerName(), hex, "LIST");
                getChannelService().publish("revenge:ah:update", msg);
                
                return true;
            } else {
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> buyItem(Player buyer, String auctionId) {
        AuctionItem item = activeAuctions.get(auctionId);
        
        if (item == null || item.getSellerUuid().equals(buyer.getUniqueId())) {
            return CompletableFuture.completedFuture(false);
        }

        if (!economyService.hasBalance(buyer.getUniqueId(), buyer.getName(), item.getPrice())) {
            return CompletableFuture.completedFuture(false);
        }

        return economyService.removeBalance(buyer.getUniqueId(), buyer.getName(), item.getPrice()).thenCompose(removed -> {
            if (!removed) {
                return CompletableFuture.completedFuture(false);
            }

            return economyService.addBalance(item.getSellerUuid(), item.getSellerName(), item.getPrice()).thenCompose(added -> {
                return CompletableFuture.runAsync(() -> auctionRepository.delete(item)).thenApply(v -> {
                    activeAuctions.remove(auctionId);
                    
                    CrossAuctionUpdateMessage msg = new CrossAuctionUpdateMessage(getServerName(), auctionId, "BUY");
                    getChannelService().publish("revenge:ah:update", msg);
                    
                    return true;
                });
            });
        });
    }

    public CompletableFuture<Boolean> cancelListing(Player seller, String auctionId) {
        AuctionItem item = activeAuctions.get(auctionId);

        if (item == null || !item.getSellerUuid().equals(seller.getUniqueId())) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.runAsync(() -> auctionRepository.delete(item)).thenApply(v -> {
            activeAuctions.remove(auctionId);
            
            CrossAuctionUpdateMessage msg = new CrossAuctionUpdateMessage(getServerName(), auctionId, "CANCEL");
            getChannelService().publish("revenge:ah:update", msg);
            
            return true;
        });
    }

    public void onAuctionUpdate(CrossAuctionUpdateMessage msg) {
        if (msg.serverId.equalsIgnoreCase(getServerName())) {
            return;
        }

        if (msg.getAction().equals("LIST")) {
            CompletableFuture.runAsync(() -> {
                AuctionItem item = auctionRepository.findOne(Filters.eq("_id", new org.bson.types.ObjectId(msg.getAuctionId())));
                if (item != null) {
                    activeAuctions.put(item.id().toHexString(), item);
                }
            });
        } else if (msg.getAction().equals("BUY") || msg.getAction().equals("CANCEL") || msg.getAction().equals("EXPIRE")) {
            activeAuctions.remove(msg.getAuctionId());
        }
    }
}
