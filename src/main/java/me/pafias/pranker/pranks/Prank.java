package me.pafias.pranker.pranks;

import me.pafias.pranker.Pranker;
import me.pafias.pranker.Victim;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Prank implements Listener {

    public final Pranker plugin = Pranker.get();

    private final String id;
    private final String name;
    private final String description;

    private final Set<Victim> players;

    public Prank(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        players = new HashSet<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<Victim> getPlayers() {
        return players;
    }

    public Set<Player> getBukkitPlayers() {
        return getPlayers().stream().map(Victim::getPlayer).collect(Collectors.toSet());
    }

    public abstract void playerAdded(Victim player);

    protected void addPlayer(Victim player) throws Exception {
        if (players.contains(player)) return;
        players.add(player);
        playerAdded(player);
    }

    public abstract void playerRemoved(Victim player);

    protected void removePlayer(Victim player) throws Exception {
        if (!players.contains(player)) return;
        players.remove(player);
        playerRemoved(player);
    }

}
