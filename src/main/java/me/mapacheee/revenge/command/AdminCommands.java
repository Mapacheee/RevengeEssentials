package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.ReloadServiceManager;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;

import me.mapacheee.revenge.service.BroadcastService;
import me.mapacheee.revenge.service.MaintenanceService;
import me.mapacheee.revenge.service.PlayerDataService;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.net.URI;
import java.util.stream.Collectors;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@CommandComponent
public class AdminCommands {

    private final ReloadServiceManager reloadServiceManager;
    private final Container<Messages> messages;
    private final BroadcastService broadcastService;
    private final MaintenanceService maintenanceService;
    private final PlayerDataService playerDataService;

    @Inject
    public AdminCommands(ReloadServiceManager reloadServiceManager, Container<Messages> messages, BroadcastService broadcastService, MaintenanceService maintenanceService, PlayerDataService playerDataService) {
        this.reloadServiceManager = reloadServiceManager;
        this.messages = messages;
        this.broadcastService = broadcastService;
        this.maintenanceService = maintenanceService;
        this.playerDataService = playerDataService;
    }

    @Command("ressentials reload")
    @Permission("revenge.admin")
    public void reload(Source source) {
        reloadServiceManager.reload();
        source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().reloaded()));
    }

    @Suggestions("broadcast_scopes")
    public java.util.List<String> broadcastScopes(CommandContext<Source> context, CommandInput input) {
        return java.util.List.of("global", "local");
    }

    @Suggestions("players")
    public List<String> players(CommandContext<Source> context, CommandInput input) {
        return playerDataService.getAllPlayerNames()
                .stream()
                .collect(Collectors.toList());
    }

    @Command("broadcast <scope> <message>")
    @Permission("revenge.broadcast")
    public void broadcast(Source source, @Argument(value = "scope", suggestions = "broadcast_scopes") String scope, @Argument("message") String[] messageArgs) {
        String message = String.join(" ", messageArgs);
        if (scope.equalsIgnoreCase("global")) {
            broadcastService.sendGlobalBroadcast(message);
        } else if (scope.equalsIgnoreCase("local")) {
            broadcastService.sendLocalBroadcast(message);
        } else {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().broadcastUsage()));
        }
    }

    @Command("maintenance <seconds>")
    @Permission("revenge.maintenance")
    public void maintenance(Source source, @org.incendo.cloud.annotations.Argument("seconds") int seconds) {
        if (seconds <= 0) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().maintenanceInvalidTime()));
            return;
        }
        maintenanceService.triggerMaintenanceNetwork(seconds);
        source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().maintenanceStart().replace("<seconds>", String.valueOf(seconds))));
    }

    @Command("whois <player>")
    @Permission("revenge.whois")
    public void whois(Source source, @Argument(value = "player", suggestions = "players") String playerName) {
        playerDataService.getUUIDFromName(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().whoisNotFound().replace("<player>", playerName)));
                return;
            }
            playerDataService.getPlayerData(uuid, playerName).thenAccept(data -> {
                String ip = data.getLastIp() != null ? data.getLastIp() : "Desconocida";
                String aliases = data.getKnownNames() != null ? String.join(", ", data.getKnownNames()) : data.getName();
                String lastConn = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(data.getLastConnection()));
                
                String country = "Desconocido";
                if (data.getLastIp() != null) {
                    try {
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("http://ip-api.com/json/" + data.getLastIp() + "?fields=country"))
                                .build();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        if (json.has("country")) {
                            country = json.get("country").getAsString();
                        }
                    } catch (Exception e) {}
                }
                
                String fc = country;
                source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().whoisFormat()
                        .replace("<player>", data.getName())
                        .replace("<ip>", ip)
                        .replace("<country>", fc)
                        .replace("<last_connection>", lastConn)
                        .replace("<known_names>", aliases)
                ));
            });
        });
    }
}
