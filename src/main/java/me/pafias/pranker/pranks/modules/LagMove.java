package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.concurrent.ThreadLocalRandom;

public class LagMove extends Prank {

    public LagMove() {
        super("lagmove", "Lag move", "Occasionally teleports the player a block back creating the illusion of lag");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo().getBlockX() != event.getFrom().getBlockX() || event.getTo().getBlockZ() != event.getFrom().getBlockZ()) {
            if (!getBukkitPlayers().contains(event.getPlayer())) return;
            if (ThreadLocalRandom.current().nextDouble() < 0.1)
                event.setTo(event.getFrom());
        }
    }

    @Override
    public void playerAdded(Victim player) {

    }

    @Override
    public void playerRemoved(Victim player) {

    }
}
