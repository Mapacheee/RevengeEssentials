package me.mapacheee.revenge.listener;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.entity.Pose;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.thewinterframework.configurate.Container;
import me.mapacheee.revenge.config.Messages;

@ListenerComponent
public class SitListener implements Listener {

    private final Map<UUID, ArmorStand> sittingPlayers = new HashMap<>();
    private final Map<UUID, Boolean> layingPlayers = new HashMap<>();
    private final Container<Messages> messages;

    @Inject
    public SitListener(Container<Messages> messages) {
        this.messages = messages;
    }

    public boolean sit(Player player) {
        if (sittingPlayers.containsKey(player.getUniqueId())) {
            return false;
        }

        if (!((Entity) player).isOnGround()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().sitNotOnGround()));
            return false;
        }

        Location loc = player.getLocation().clone();
        loc.setX(loc.getBlockX() + 0.5);
        loc.setZ(loc.getBlockZ() + 0.5);
        loc.setY(loc.getY() - 1.7); 

        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class, armorStand -> {
            armorStand.setInvisible(true);
            armorStand.setInvulnerable(true);
            armorStand.setGravity(false);
            armorStand.setCollidable(false);
            armorStand.addPassenger(player);
        });

        sittingPlayers.put(player.getUniqueId(), stand);
        return true;
    }

    public boolean lay(Player player) {
        if (sittingPlayers.containsKey(player.getUniqueId())) {
            return false;
        }

        if (!((Entity) player).isOnGround()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(messages.get().layNotOnGround()));
            return false;
        }

        Location loc = player.getLocation().clone();
        loc.setX(loc.getBlockX() + 0.5);
        loc.setZ(loc.getBlockZ() + 0.5);
        loc.setY(loc.getY() - 1.7); 

        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class, armorStand -> {
            armorStand.setInvisible(true);
            armorStand.setInvulnerable(true);
            armorStand.setGravity(false);
            armorStand.setCollidable(false);
            armorStand.addPassenger(player);
        });

        player.setPose(Pose.SWIMMING);
        layingPlayers.put(player.getUniqueId(), true);
        sittingPlayers.put(player.getUniqueId(), stand);
        return true;
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            ArmorStand stand = sittingPlayers.remove(player.getUniqueId());
            if (stand != null && event.getDismounted().equals(stand)) {
                stand.remove();
                if (layingPlayers.remove(player.getUniqueId()) != null) {
                    player.setPose(Pose.STANDING);
                }
                player.teleportAsync(player.getLocation().add(0, 1.7, 0));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ArmorStand stand = sittingPlayers.remove(event.getPlayer().getUniqueId());
        if (stand != null) {
            stand.remove();
        }
        layingPlayers.remove(event.getPlayer().getUniqueId());
    }
}
