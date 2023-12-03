package me.pafias.pranker.pranks.modules;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class Reach extends Prank {

    public Reach() {
        super("reach", "Reach \"hacks\"", "Gives the player a reach of 6 blocks");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmSwing(PlayerArmSwingEvent event) {
        if (!getBukkitPlayers().contains(event.getPlayer())) return;
        Entity target = event.getPlayer().getTargetEntity(6);
        if (target == null) return;
        event.getPlayer().attack(target);
    }

    @Override
    public void playerAdded(Victim player) {

    }

    @Override
    public void playerRemoved(Victim player) {

    }
}
