package me.mapacheee.revenge.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.mapacheee.revenge.service.PlayerDataService;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EssentialsPlaceholderExpansion extends PlaceholderExpansion {

    private final PlayerDataService playerDataService;

    public EssentialsPlaceholderExpansion(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "revenge";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Mapacheee";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("essentials_kills")) {
            try {
                return String.valueOf(playerDataService.getPlayerKills(player.getUniqueId(), player.getName()).join());
            } catch (Exception e) {
                return "0";
            }
        }

        if (params.equalsIgnoreCase("essentials_deaths")) {
            try {
                return String.valueOf(playerDataService.getPlayerDeaths(player.getUniqueId(), player.getName()).join());
            } catch (Exception e) {
                return "0";
            }
        }

        return null;
    }
}
