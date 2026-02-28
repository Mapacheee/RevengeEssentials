package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class VaultEconomyService implements Economy {

    private final EconomyService economyService;
    private final Plugin plugin;

    @Inject
    public VaultEconomyService(EconomyService economyService, Plugin plugin) {
        this.economyService = economyService;
        this.plugin = plugin;
    }

    @OnEnable
    public void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            Bukkit.getServicesManager().register(Economy.class, this, plugin, ServicePriority.Highest);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "RevengeEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return String.format("$%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return "Coins";
    }

    @Override
    public String currencyNameSingular() {
        return "Coin";
    }

    @Override
    public boolean hasAccount(String playerName) {
        try {
            UUID id = economyService.playerDataService.getUUIDFromName(playerName).get(3, TimeUnit.SECONDS);
            if (id == null) return false;
            return hasAccount(Bukkit.getOfflinePlayer(id));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        try {
            return economyService.getBalance(player.getUniqueId(), player.getName()).get(3, TimeUnit.SECONDS) != null;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        try {
            UUID id = economyService.playerDataService.getUUIDFromName(playerName).get(3, TimeUnit.SECONDS);
            if (id == null) return 0.0;
            return getBalance(Bukkit.getOfflinePlayer(id));
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        try {
            return economyService.getBalance(player.getUniqueId(), player.getName()).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        try {
            UUID id = economyService.playerDataService.getUUIDFromName(playerName).get(3, TimeUnit.SECONDS);
            if (id == null) return false;
            return has(Bukkit.getOfflinePlayer(id), amount);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economyService.hasBalance(player.getUniqueId(), player.getName(), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        try {
            UUID id = economyService.playerDataService.getUUIDFromName(playerName).get(3, TimeUnit.SECONDS);
            if (id == null) return new EconomyResponse(amount, 0.0, EconomyResponse.ResponseType.FAILURE, "Player offline or no data");
            return withdrawPlayer(Bukkit.getOfflinePlayer(id), amount);
        } catch (Exception e) {
            return new EconomyResponse(amount, 0.0, EconomyResponse.ResponseType.FAILURE, "Backend timeout");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (!has(player, amount)) {
            return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
        }
        try {
            boolean success = economyService.removeBalance(player.getUniqueId(), player.getName(), amount).get(3, TimeUnit.SECONDS);
            return new EconomyResponse(amount, getBalance(player), success ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, "");
        } catch (Exception e) {
            return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Timeout");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        try {
            UUID id = economyService.playerDataService.getUUIDFromName(playerName).get(3, TimeUnit.SECONDS);
            if (id == null) return new EconomyResponse(amount, 0.0, EconomyResponse.ResponseType.FAILURE, "User offline or no data");
            return depositPlayer(Bukkit.getOfflinePlayer(id), amount);
        } catch (Exception e) {
            return new EconomyResponse(amount, 0.0, EconomyResponse.ResponseType.FAILURE, "Timeout");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        try {
            boolean success = economyService.addBalance(player.getUniqueId(), player.getName(), amount).get(3, TimeUnit.SECONDS);
            return new EconomyResponse(amount, getBalance(player), success ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, "");
        } catch (Exception e) {
            return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Timeout");
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override public EconomyResponse createBank(String name, String player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse deleteBank(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse bankBalance(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse bankHas(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No Banks"); }
    @Override public List<String> getBanks() { return List.of(); }
    @Override public boolean createPlayerAccount(String playerName) { return false; }
    @Override public boolean createPlayerAccount(OfflinePlayer player) { return false; }
    @Override public boolean createPlayerAccount(String playerName, String worldName) { return false; }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return false; }
}
