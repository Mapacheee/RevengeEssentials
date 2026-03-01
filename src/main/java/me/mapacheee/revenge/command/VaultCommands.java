package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import me.mapacheee.revenge.gui.VaultGui;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import org.checkerframework.checker.nullness.qual.Nullable;

@CommandComponent
public class VaultCommands {

    private final VaultGui vaultGui;

    @Inject
    public VaultCommands(VaultGui vaultGui) {
        this.vaultGui = vaultGui;
    }

    @Command("vault [page]")
    @Permission("revenge.vault")
    public void openVault(Source source, @Nullable @Argument("page") Integer page) {
        if (!(source.source() instanceof Player player)) return;
        int targetPage = page != null ? page : 1;
        vaultGui.open(player, targetPage);
    }
}
