package me.pafias.pranker;

import me.pafias.pranker.pranks.Prank;
import org.bukkit.entity.Player;

import java.util.Collection;

public class Victim {

    private final Pranker plugin = Pranker.get();

    private final Player player;

    public Victim(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Collection<Prank> getActivePranks() {
        return plugin.getPrankManager().getPranks(this);
    }

    public void stopPranks() {
        plugin.getPrankManager().getPranks().forEach(prank -> plugin.getPrankManager().removePlayer(this, prank));
    }

}
