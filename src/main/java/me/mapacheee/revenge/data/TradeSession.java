package me.mapacheee.revenge.data;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeSession {

    private final UUID playerA;
    private final UUID playerB;
    private final List<ItemStack> itemsA = new ArrayList<>();
    private final List<ItemStack> itemsB = new ArrayList<>();
    private double moneyA = 0;
    private double moneyB = 0;
    private boolean confirmedA = false;
    private boolean confirmedB = false;

    public TradeSession(UUID playerA, UUID playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
    }

    public UUID getPlayerA() { return playerA; }
    public UUID getPlayerB() { return playerB; }

    public List<ItemStack> getItemsA() { return itemsA; }
    public List<ItemStack> getItemsB() { return itemsB; }

    public double getMoneyA() { return moneyA; }
    public void setMoneyA(double moneyA) { this.moneyA = moneyA; this.confirmedA = false; this.confirmedB = false; }

    public double getMoneyB() { return moneyB; }
    public void setMoneyB(double moneyB) { this.moneyB = moneyB; this.confirmedA = false; this.confirmedB = false; }

    public boolean isConfirmedA() { return confirmedA; }
    public void setConfirmedA(boolean confirmed) { this.confirmedA = confirmed; }

    public boolean isConfirmedB() { return confirmedB; }
    public void setConfirmedB(boolean confirmed) { this.confirmedB = confirmed; }

    public boolean isBothConfirmed() { return confirmedA && confirmedB; }

    public void resetConfirmations() {
        this.confirmedA = false;
        this.confirmedB = false;
    }

    public boolean isParticipant(UUID uuid) {
        return playerA.equals(uuid) || playerB.equals(uuid);
    }

    public UUID getOtherPlayer(UUID uuid) {
        return playerA.equals(uuid) ? playerB : playerA;
    }

    public boolean isPlayerA(UUID uuid) { return playerA.equals(uuid); }
}
