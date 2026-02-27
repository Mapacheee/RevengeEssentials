package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossKitGiveAllMessage;
import me.mapacheee.revenge.config.Messages;
import me.mapacheee.revenge.data.Kit;
import me.mapacheee.revenge.service.KitService;
import me.mapacheee.revenge.service.InventorySyncService;
import me.mapacheee.revenge.service.PlayerDataService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;

import java.util.List;
import java.util.stream.Collectors;

@CommandComponent
public class KitCommands {

    private final KitService kitService;
    private final InventorySyncService inventorySyncService;
    private final PlayerDataService playerDataService;
    private final me.mapacheee.revenge.gui.KitGui kitGui;
    private final me.mapacheee.revenge.gui.KitEditGui kitEditGui;
    private final Container<Messages> messages;

    @Inject
    public KitCommands(KitService kitService, InventorySyncService inventorySyncService, PlayerDataService playerDataService, me.mapacheee.revenge.gui.KitGui kitGui, me.mapacheee.revenge.gui.KitEditGui kitEditGui, Container<Messages> messages) {
        this.kitService = kitService;
        this.inventorySyncService = inventorySyncService;
        this.playerDataService = playerDataService;
        this.kitGui = kitGui;
        this.kitEditGui = kitEditGui;
        this.messages = messages;
    }

    @Suggestions("kits")
    public List<String> kits(CommandContext<Source> context, CommandInput input) {
        return kitService.getAllKits().stream()
                .map(Kit::getName)
                .collect(Collectors.toList());
    }

    @Suggestions("players")
    public List<String> players(CommandContext<Source> context, CommandInput input) {
        return playerDataService.getAllPlayerNames()
                .stream()
                .collect(Collectors.toList());
    }

    @Command("kit create <name>")
    @Permission("revenge.admin")
    public void createKit(Source source, @Argument("name") String name) {
        if (!(source.source() instanceof Player player)) return;

        Kit kit = kitService.getKit(name);
        if (kit != null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitAlreadyExists()));
            return;
        }

        kit = new Kit(name);
        ItemStack[] contents = player.getInventory().getContents();
        kit.setInventoryBase64(inventorySyncService.serializeItems(contents));
        
        kitService.saveKit(kit);
        player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitCreated(), Placeholder.parsed("kit", name)));
    }

    @Command("kit give <kit> <target> [amount]")
    @Permission("revenge.admin")
    public void giveKitVoucher(Source source, @Argument(value = "kit", suggestions = "kits") String kitName, @Argument(value = "target", suggestions = "players") String target, @Default("1") @Argument("amount") int amount) {
        Kit kit = kitService.getKit(kitName);
        if (kit == null) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitNotFound()));
            return;
        }

        if (target.equalsIgnoreCase("@a")) {
            for (int i = 0; i < amount; i++) {
                RevengeCoreAPI.get().getChannelService().publish(
                    "revenge:kit_give_all",
                    new CrossKitGiveAllMessage(
                        RevengeCoreAPI.get().getServerName(),
                        kitName,
                        source.source() instanceof Player ? ((Player) source.source()).getName() : "Console"
                    )
                );
            }
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitGiveAll(), net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)), net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("kit", kitName)));
            return;
        }

        for (int i = 0; i < amount; i++) {
            kitService.giveVoucher(target, kit, playerDataService);
        }
        
        source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().prefix() + messages.get().kitVoucherGiven(), Placeholder.parsed("amount", String.valueOf(amount)), Placeholder.parsed("kit", kitName), Placeholder.parsed("player", target)));
    }

    @Command("kit")
    public void kitRoot(Source source) {
        if (!(source.source() instanceof Player player)) return;
        kitGui.open(player);
    }

    @Command("kits")
    public void kitsAlias(Source source) {
        kitRoot(source);
    }

    @Command("kit edit")
    @Permission("revenge.admin")
    public void kitEdit(Source source) {
        if (!(source.source() instanceof Player player)) return;
        kitEditGui.openList(player);
    }
}
