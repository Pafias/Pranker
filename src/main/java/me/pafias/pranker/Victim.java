package me.pafias.pranker;

import lombok.Getter;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.entity.Player;

import java.util.Collection;

public class Victim {

    private final Pranker plugin = Pranker.get();

    @Getter
    private final Player player;

    public Victim(Player player) {
        this.player = player;
    }

    public Collection<Prank> getActivePranks() {
        return plugin.getPrankManager().getActivePranks(this);
    }

    public void stopPranks() {
        plugin.getPrankManager().removeAll(this);
    }

}
