package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Random;

public class LagMove extends Prank {

    public LagMove(Victim victim) {
        super(Pranks.LAG_MOVE, victim);
        this.random = new Random();
    }

    private final Random random;

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo().getBlockX() != event.getFrom().getBlockX() || event.getTo().getBlockZ() != event.getFrom().getBlockZ()) {
            if (!isTarget(event.getPlayer())) return;
            if (random.nextDouble() < 0.1)
                event.setTo(event.getFrom());
        }
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }
}
