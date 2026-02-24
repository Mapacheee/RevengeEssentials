package me.mapacheee.revenge;

import com.thewinterframework.paper.PaperWinterPlugin;
import com.thewinterframework.plugin.WinterBootPlugin;

@WinterBootPlugin
public class RevengeEssentialsPlugin extends PaperWinterPlugin {

    @Override
    public void onPluginEnable() {
        super.onPluginEnable();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }

    @Override
    public void onPluginDisable() {
        super.onPluginDisable();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
    }
}