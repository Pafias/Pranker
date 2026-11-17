package me.pafias.pranker.pranks.modules;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class Reach extends Prank {

    public Reach(Victim victim, String... args) {
        super(Pranks.REACH, victim, args);
        reachAmount = intArg(0, 6, 0, 100, "reach distance");
    }

    private final int reachAmount;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmSwing(PlayerArmSwingEvent event) {
        if (!isTarget(event.getPlayer())) return;
        Entity target = event.getPlayer().getTargetEntity(reachAmount);
        if (target == null) return;
        event.getPlayer().attack(target);
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }
}
