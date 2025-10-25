package me.pafias.pranker.pranks;

import me.pafias.pranker.Pranker;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.modules.*;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PrankManager {

    private final Pranker plugin;

    public PrankManager(Pranker plugin) {
        this.plugin = plugin;
        pranks = new HashSet<>();
        pranks.add(new Reach());
        pranks.add(new LagMove());
        pranks.add(new NoDamage());
        pranks.add(new Cat());
        pranks.add(new DropItem());
        pranks.add(new TNTWalk());
        pranks.add(new ChristmasHat());
        pranks.add(new Undercover());
        pranks.add(new Minigun());
        pranks.add(new Dance());
        pranks.add(new UpsideDown());
        pranks.add(new SelfShoot());
        pranks.add(new MidasTouch());
        pranks.add(new WitnessProtection());
        pranks.add(new Swim());
        pranks.add(new Demo());
        pranks.add(new Credits());
    }

    private final Set<Prank> pranks;

    public Set<Prank> getPranks() {
        return pranks;
    }

    public Prank getPrank(String s) {
        return pranks.stream().filter(p -> p.getID().toLowerCase().startsWith(s.toLowerCase().trim()) || p.getName().toLowerCase().startsWith(s.toLowerCase().trim())).findAny().orElse(null);
    }

    public Prank getPrankByID(String id) {
        return pranks.stream().filter(p -> p.getID().toLowerCase().startsWith(id.toLowerCase().trim())).findAny().orElse(null);
    }

    public Prank getPrankByName(String name) {
        return pranks.stream().filter(p -> p.getName().toLowerCase().startsWith(name.toLowerCase().trim())).findAny().orElse(null);
    }

    public Set<Prank> getPranks(Victim victim) {
        return pranks.stream().filter(p -> p.getPlayers().contains(victim)).collect(Collectors.toSet());
    }

    public Set<Prank> getPranks(Player player) {
        return pranks.stream().filter(p -> p.getPlayers().stream().map(Victim::getPlayer).anyMatch(pp -> pp.getUniqueId().equals(player.getUniqueId()))).collect(Collectors.toSet());
    }

    public void addPlayer(Victim player, Prank prank) throws Exception {
        prank.addPlayer(player);
    }

    public void removePlayer(Victim player, Prank prank) throws Exception {
        prank.removePlayer(player);
    }

}
