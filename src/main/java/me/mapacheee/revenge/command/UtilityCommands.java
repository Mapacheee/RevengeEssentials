package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.api.RevengeCoreAPI;
import me.mapacheee.revenge.channel.CrossClearMessage;
import me.mapacheee.revenge.config.Messages;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.format.TextDecoration;
import me.mapacheee.revenge.channel.CrossGodModeAllMessage;
import me.mapacheee.revenge.channel.CrossFlyAllMessage;
import me.mapacheee.revenge.channel.CrossSpeedAllMessage;
import me.mapacheee.revenge.channel.CrossGamemodeAllMessage;
import me.mapacheee.revenge.channel.CrossHealAllMessage;
import me.mapacheee.revenge.channel.CrossFeedAllMessage;
import me.mapacheee.revenge.service.PlayerStateService;
import me.mapacheee.revenge.service.FlyService;
import me.mapacheee.revenge.service.SpeedService;
import me.mapacheee.revenge.service.BackService;
import me.mapacheee.revenge.service.RtpService;
import me.mapacheee.revenge.service.SpawnService;
import me.mapacheee.revenge.service.PlayerDataService;
import me.mapacheee.revenge.listener.SitListener;
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
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CommandComponent
public class UtilityCommands {

    private final Container<Messages> messages;
    private final PlayerDataService playerDataService;
    private final PlayerStateService playerStateService;
    private final FlyService flyService;
    private final SpeedService speedService;
    private final BackService backService;
    private final RtpService rtpService;
    private final SpawnService spawnService;
    private final SitListener sitListener;
    private final Plugin plugin;

    @Inject
    public UtilityCommands(
            Container<Messages> messages, 
            PlayerDataService playerDataService,
            PlayerStateService playerStateService,
            FlyService flyService,
            SpeedService speedService,
            BackService backService,
            RtpService rtpService,
            SpawnService spawnService,
            SitListener sitListener,
            Plugin plugin
    ) {
        this.messages = messages;
        this.playerDataService = playerDataService;
        this.playerStateService = playerStateService;
        this.flyService = flyService;
        this.speedService = speedService;
        this.backService = backService;
        this.rtpService = rtpService;
        this.spawnService = spawnService;
        this.sitListener = sitListener;
        this.plugin = plugin;
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

    @Command("god [target]")
    @Permission("revenge.god")
    public void god(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        if (target == null && !(source.source() instanceof Player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        if (target != null && target.equalsIgnoreCase("all")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:godmode_all",
                new CrossGodModeAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console",
                    null
                )
            );
            
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().godmodeUpdatedAll()
            ));
            return;
        }

        String effectiveTarget = target != null ? target : ((Player) source.source()).getName();
        playerStateService.setGodMode(source, effectiveTarget, null); 
    }

    @Command("randomtp|rtp")
    @Permission("revenge.rtp")
    public void rtpCommand(Source source) {
        if (!(source.source() instanceof Player player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Solo los jugadores pueden usar este comando.</red>"));
            return;
        }

        rtpService.startRtp(player);
    }
    
    @Command("speed <value> [type]")
    @Permission("revenge.speed")
    public void speed(
            Source source, 
            @Argument("value") int value, 
            @Nullable @Argument("type") String type
    ) {
        if (!(source.source() instanceof Player player)) {
            source.source().sendMessage("Console must specify a player using /speedother.");
            return;
        }

        SpeedService.SpeedType speedType = SpeedService.SpeedType.BOTH;
        if (type != null) {
            speedType = type.equalsIgnoreCase("walk") ? SpeedService.SpeedType.WALK : SpeedService.SpeedType.FLY;
        }

        speedService.setSpeed(source, player.getName(), value, speedType);
    }

    @Command("speedother <target> <value> [type]")
    @Permission("revenge.speed.others")
    public void speedOther(
            Source source,
            @Argument(value = "target", suggestions = "players") String target,
            @Argument("value") int value,
            @Nullable @Argument("type") String type
    ) {
        SpeedService.SpeedType speedType = SpeedService.SpeedType.BOTH;
        if (type != null) {
            speedType = type.equalsIgnoreCase("walk") ? SpeedService.SpeedType.WALK : SpeedService.SpeedType.FLY;
        }

        if (target.equalsIgnoreCase("all")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:speed_all",
                new CrossSpeedAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console",
                    value,
                    speedType.name()
                )
            );
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().speedSetAll()
            ));
            return;
        }

        speedService.setSpeed(source, target, value, speedType);
    }

    @Command("back")
    @Permission("revenge.back")
    public void back(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        backService.teleportBack(player);
    }

    @Command("fly")
    @Permission("revenge.fly")
    public void flySelf(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        flyService.toggleFly(source, player.getName());
    }

    @Command("fly <target>")
    @Permission("revenge.fly.others")
    public void flyOther(Source source, @Argument(value = "target", suggestions = "players") String target) {
        if (target.equalsIgnoreCase("all")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:fly_all",
                new CrossFlyAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console",
                    null
                )
            );
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().flyEnabledAll()));
            return;
        }
        flyService.toggleFly(source, target);
    }

    @Command("heal [target]")
    @Permission("revenge.heal")
    public void heal(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        if (target == null && !(source.source() instanceof Player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        if (target != null && target.equalsIgnoreCase("all")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:heal_all",
                new CrossHealAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console"
                )
            );
            
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().healAll()
            ));
            return;
        }

        String effectiveTarget = target != null ? target : ((Player) source.source()).getName();
        playerStateService.healPlayer(source, effectiveTarget);
    }

    @Command("feed [target]")
    @Permission("revenge.feed")
    public void feed(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        if (target == null && !(source.source() instanceof Player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        if (target != null && target.equalsIgnoreCase("all")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:feed_all",
                new CrossFeedAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console"
                )
            );

            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().feedAll()
            ));
            return;
        }

        String effectiveTarget = target != null ? target : ((Player) source.source()).getName();
        playerStateService.feedPlayer(source, effectiveTarget);
    }

    @Command("spawn")
    @Permission("revenge.spawn")
    public void spawn(Source source) {
        if (!(source.source() instanceof Player player))
            return;
        spawnService.teleportToSpawn(player);
    }

    @Command("setspawn")
    @Permission("revenge.admin")
    public void setSpawn(Source source) {
        if (!(source.source() instanceof Player player))
            return;

        spawnService.setSpawn(player).thenRun(() -> player.getScheduler().run(plugin,
                task -> player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().spawnSet())), null));
    }

    @Command("gmc [target]")
    @Permission("revenge.gamemode")
    public void gmc(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, GameMode.CREATIVE);
    }

    @Command("gms [target]")
    @Permission("revenge.gamemode")
    public void gms(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, GameMode.SURVIVAL);
    }

    @Command("gmsp [target]")
    @Permission("revenge.gamemode")
    public void gmsp(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, GameMode.SPECTATOR);
    }

    @Command("gma [target]")
    @Permission("revenge.gamemode")
    public void gma(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, GameMode.ADVENTURE);
    }
    
    @Command("gamemode <mode> [target]")
    @Permission("revenge.gamemode")
    public void gm(Source source, @Argument("mode") GameMode mode, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        processGamemode(source, target, mode);
    }

    private void processGamemode(Source source, @Nullable String targetName, GameMode mode) {
        if (targetName == null && !(source.source() instanceof Player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        if (targetName != null && targetName.equalsIgnoreCase("all")) {
            RevengeCoreAPI.get().getChannelService().publish(
                "revenge:gamemode_all",
                new CrossGamemodeAllMessage(
                    RevengeCoreAPI.get().getServerName(),
                    source.source() instanceof Player ? ((Player) source.source()).getName() : "Console",
                    mode.name()
                )
            );
            
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().gamemodeUpdatedAll(),
                Placeholder.parsed("mode", mode.name())
            ));
            return;
        }
        
        String effectiveTarget = targetName != null ? targetName : ((Player) source.source()).getName();
        playerStateService.setGameMode(source, effectiveTarget, mode);
    }

    @Command("itemrename [name]")
    @Permission("revenge.itemrename")
    public void itemRename(Source source, @Nullable @Argument("name") String[] nameArgs) {
        if (!(source.source() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            String noItemBox = messages.get().itemNoItem() != null ? messages.get().itemNoItem() : "<red>Debes sostener un objeto en tu mano.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(noItemBox));
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (nameArgs == null || nameArgs.length == 0 || String.join(" ", nameArgs).trim().isEmpty()) {
            meta.displayName(null);
            item.setItemMeta(meta);
            String msg = messages.get().itemRenameClearSuccess() != null ? messages.get().itemRenameClearSuccess() : "<green>Nombre del objeto restaurado.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
            return;
        }
        String name = String.join(" ", nameArgs);
        meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        String msg = messages.get().itemRenameSuccess() != null ? messages.get().itemRenameSuccess() : "<green>Nombre del objeto actualizado.";
        player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
    }

    @Command("itemlore add <text>")
    @Permission("revenge.itemlore")
    public void itemLoreAdd(Source source, @Argument("text") String[] textArgs) {
        if (!(source.source() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            String noItemBox = messages.get().itemNoItem() != null ? messages.get().itemNoItem() : "<red>Debes sostener un objeto en tu mano.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(noItemBox));
            return;
        }
        ItemMeta meta = item.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? Objects.requireNonNullElse(meta.lore(), new java.util.ArrayList<>()) : new java.util.ArrayList<>();
        String text = String.join(" ", textArgs);
        lore.add(MiniMessage.miniMessage().deserialize(text).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        String msg = messages.get().itemLoreAddSuccess() != null ? messages.get().itemLoreAddSuccess() : "<green>Línea de lore añadida.";
        player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
    }

    @Command("itemlore remove <index>")
    @Permission("revenge.itemlore")
    public void itemLoreRemove(Source source, @Argument("index") int index) {
        if (!(source.source() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            String noItemBox = messages.get().itemNoItem() != null ? messages.get().itemNoItem() : "<red>Debes sostener un objeto en tu mano.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(noItemBox));
            return;
        }
        ItemMeta meta = item.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? Objects.requireNonNullElse(meta.lore(), new java.util.ArrayList<>()) : new java.util.ArrayList<>();
        if (index < 1 || index > lore.size()) {
            String invalidMsg = messages.get().itemLoreInvalidIndex() != null ? messages.get().itemLoreInvalidIndex() : "<red>Índice de lore inválido.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(invalidMsg));
            return;
        }
        lore.remove(index - 1);
        meta.lore(lore);
        item.setItemMeta(meta);
        String msg = messages.get().itemLoreRemoveSuccess() != null ? messages.get().itemLoreRemoveSuccess() : "<green>Línea de lore <index> eliminada.";
        player.sendMessage(MiniMessage.miniMessage().deserialize(msg, Placeholder.unparsed("index", String.valueOf(index))));
    }

    @Command("itemlore set <index> <text>")
    @Permission("revenge.itemlore")
    public void itemLoreSet(Source source, @Argument("index") int index, @Argument("text") String[] textArgs) {
        if (!(source.source() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            String noItemBox = messages.get().itemNoItem() != null ? messages.get().itemNoItem() : "<red>Debes sostener un objeto en tu mano.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(noItemBox));
            return;
        }
        ItemMeta meta = item.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? Objects.requireNonNullElse(meta.lore(), new java.util.ArrayList<>()) : new java.util.ArrayList<>();
        if (index < 1 || index > lore.size()) {
            String invalidMsg = messages.get().itemLoreInvalidIndex() != null ? messages.get().itemLoreInvalidIndex() : "<red>Índice de lore inválido.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(invalidMsg));
            return;
        }
        String text = String.join(" ", textArgs);
        lore.set(index - 1, MiniMessage.miniMessage().deserialize(text).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        String msg = messages.get().itemLoreSetSuccess() != null ? messages.get().itemLoreSetSuccess() : "<green>Línea de lore <index> actualizada.";
        player.sendMessage(MiniMessage.miniMessage().deserialize(msg, Placeholder.unparsed("index", String.valueOf(index))));
    }

    @Command("itemlore clear")
    @Permission("revenge.itemlore")
    public void itemLoreClear(Source source) {
        if (!(source.source() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            String noItemBox = messages.get().itemNoItem() != null ? messages.get().itemNoItem() : "<red>Debes sostener un objeto en tu mano.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(noItemBox));
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.lore(null);
        item.setItemMeta(meta);
        String msg = messages.get().itemLoreClearSuccess() != null ? messages.get().itemLoreClearSuccess() : "<green>Lore del objeto borrado.";
        player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
    }

    @Command("enderchest|echest [target]")
    @Permission("revenge.enderchest")
    public void enderchest(Source source, @Nullable @Argument(value = "target", suggestions = "players") String target) {
        if (!(source.source() instanceof Player) && target == null) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Se debe especificar un jugador en consola.</red>"));
            return;
        }

        Player pTarget;
        if (target == null) {
            pTarget = (Player) source.source();
        } else {
            pTarget = Bukkit.getPlayerExact(target);
            if (pTarget == null) {
                source.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>El jugador no está conectado en este servidor.</red>"));
                return;
            }
        }

        if (source.source() instanceof Player player) {
            player.openInventory(pTarget.getEnderChest());
        }
    }

    @Command("sit")
    @Permission("revenge.sit")
    public void sit(Source source) {
        if (!(source.source() instanceof Player player)) return;
        sitListener.sit(player);
    }

    @Command("lay")
    @Permission("revenge.lay")
    public void lay(Source source) {
        if (!(source.source() instanceof Player player)) return;
        sitListener.lay(player);
    }

}
