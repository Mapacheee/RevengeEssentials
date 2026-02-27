package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossClearMessage;
import me.mapacheee.revenge.config.Messages;
import io.papermc.paper.registry.RegistryKey;
import me.mapacheee.revenge.service.PlayerDataService;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.checkerframework.checker.nullness.qual.Nullable;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;

import java.util.List;
import java.util.stream.Collectors;

@CommandComponent
public class UtilityCommands {

    private final Container<Messages> messages;
    private final PlayerDataService playerDataService;

    @Inject
    public UtilityCommands(Container<Messages> messages, PlayerDataService playerDataService) {
        this.messages = messages;
        this.playerDataService = playerDataService;
    }

    @Suggestions("players")
    public List<String> players(CommandContext<Source> context, CommandInput input) {
        return playerDataService.getAllPlayerNames()
                .stream()
                .collect(Collectors.toList());
    }
    
    @Suggestions("enchantments")
    public List<String> enchantments(CommandContext<Source> context, CommandInput input) {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .stream()
                .map(ench -> ench.getKey().getKey())
                .collect(Collectors.toList());
    }

    @Command("craft [target]")
    @Permission("revenge.craft")
    public void craft(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player) && target == null) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        Player pTarget;
        if (target == null) {
            pTarget = (Player) source.source();
        } else {
            pTarget = Bukkit.getPlayer(target);
            if (pTarget == null) {
                source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>El jugador no está conectado en este servidor.</red>"));
                return;
            }
        }
        
        Inventory workbench = Bukkit.createInventory(pTarget, InventoryType.WORKBENCH);
        pTarget.openInventory(workbench);
        if (source.source() instanceof Player && source.source().equals(pTarget)) {
            pTarget.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandCraftOpened()));
        }
    }

    @Command("anvil [target]")
    @Permission("revenge.anvil")
    public void anvil(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player) && target == null) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }
        
        Player pTarget;
        if (target == null) {
            pTarget = (Player) source.source();
        } else {
            pTarget = Bukkit.getPlayer(target);
            if (pTarget == null) {
                source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>El jugador no está conectado en este servidor.</red>"));
                return;
            }
        }

        Inventory anvil = Bukkit.createInventory(pTarget, InventoryType.ANVIL);
        pTarget.openInventory(anvil);
        
        if (source.source() instanceof Player && source.source().equals(pTarget)) {
            pTarget.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandAnvilOpened()));
        }
    }

    @Command("clear [target]")
    @Permission("revenge.clear")
    public void clear(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player) && target == null) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        if (target == null) {
            Player p = (Player) source.source();
            p.getInventory().clear();
            p.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandClearSelf()));
            return;
        }

        Player localTarget = Bukkit.getPlayerExact(target);
        if (localTarget != null) {
            localTarget.getInventory().clear();
            if (source.source() instanceof Player) {
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandClearOther(), 
                    Placeholder.parsed("target", localTarget.getName())));
            }
            localTarget.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandClearOtherReceived()));
        } else {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:clear",
                new CrossClearMessage(
                    RevengeCoreAPI.get().getServerName(),
                    target
                )
            );
            
            if (source.source() instanceof Player) {
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandClearOther(), 
                    Placeholder.parsed("target", target)));
            }
        }
    }

    @Command("enchant <enchantment> <level>")
    @Permission("revenge.enchant")
    public void enchant(Source source, @Argument(value = "enchantment", suggestions = "enchantments") String enchantArg, @Argument("level") int level) {
        if (!(source.source() instanceof Player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Este comando solo puede ejecutarse en el juego.</red>"));
            return;
        }
        Player player = (Player) source.source();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandEnchantNoItem()));
            return;
        }

        Enchantment enchantment = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft(enchantArg.toLowerCase()));
        
        if (enchantment == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandEnchantNotFound()));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);
            
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().commandEnchantSuccess(),
                Placeholder.parsed("enchant", enchantArg),
                Placeholder.parsed("level", String.valueOf(level))
            ));
        }
    }
}
