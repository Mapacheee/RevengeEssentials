package me.mapacheee.revenge.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.ReloadServiceManager;
import me.mapacheee.revenge.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

@CommandComponent
public class AdminCommands {

    private final ReloadServiceManager reloadServiceManager;
    private final Container<Messages> messages;

    @Inject
    public AdminCommands(ReloadServiceManager reloadServiceManager, Container<Messages> messages) {
        this.reloadServiceManager = reloadServiceManager;
        this.messages = messages;
    }

    @Command("ressentials reload")
    @Permission("revenge.admin")
    public void reload(Source source) {
        reloadServiceManager.reload();
        source.source().sendMessage(MiniMessage.miniMessage().deserialize(messages.get().reloaded()));
    }
}
