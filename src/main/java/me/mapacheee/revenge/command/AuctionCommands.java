package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.gui.AuctionGui;
import me.mapacheee.revenge.service.AuctionService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import com.thewinterframework.command.CommandComponent;

@CommandComponent
public class AuctionCommands {

    private final AuctionService auctionService;
    private final AuctionGui auctionGui;
    private final Container<Messages> messages;

    @Inject
    public AuctionCommands(AuctionService auctionService, AuctionGui auctionGui, Container<Messages> messages) {
        this.auctionService = auctionService;
        this.auctionGui = auctionGui;
        this.messages = messages;
    }

    @Command("ah")
    @Permission("revenge.ah")
    public void openAh(Source source) {
        Player player = (Player) source.source();
        auctionGui.open(player, 0);
    }

    @Command("ah sell <price>")
    @Permission("revenge.ah.sell")
    public void sellItem(Source source, @Argument("price") double price) {
        Player player = (Player) source.source();

        if (price <= 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().ahPriceInvalid()));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().ahNoItemInHand()));
            return;
        }

        auctionService.listItem(player, item, price).thenAccept(success -> {
            if (success) {
                player.getInventory().setItemInMainHand(null);
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        messages.get().ahSuccessListing(),
                        Placeholder.unparsed("price", String.valueOf(price))
                ));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().ahListingError()));
            }
        });
    }
}
