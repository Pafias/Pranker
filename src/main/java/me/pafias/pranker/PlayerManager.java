package me.pafias.pranker;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerManager {

    private final Pranker plugin;

    public PlayerManager(Pranker plugin) {
        this.plugin = plugin;
    }

    private final Set<Victim> players = new HashSet<>();

    public Set<Victim> getPlayers() {
        return players;
    }

    public Victim getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public Victim getPlayer(UUID uuid) {
        return players.stream().filter(p -> p.getPlayer().getUniqueId().equals(uuid)).findAny().orElse(null);
    }

    public Victim getPlayer(String name) {
        return players.stream().filter(p -> p.getPlayer().getName().toLowerCase().startsWith(name.toLowerCase().trim())).findAny().orElse(null);
    }

    public void addPlayer(Player player) {
        players.add(new Victim(player));
    }

    public void removePlayer(Player player) {
        Victim victim = getPlayer(player);
        victim.stopPranks();
        players.remove(victim);
    }

}
