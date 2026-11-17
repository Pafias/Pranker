package me.pafias.pranker.pranks;

import me.pafias.pranker.Pranker;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.modules.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PrankManager {

    private final Pranker plugin;

    public PrankManager(Pranker plugin) {
        this.plugin = plugin;
    }

    private final Map<Victim, EnumMap<Pranks, Prank>> activePranks = new HashMap<>();

    public Collection<Prank> getActivePranks(Victim victim) {
        final EnumMap<Pranks, Prank> victimPranks = activePranks.get(victim);
        if (victimPranks == null)
            return Collections.emptyList();
        return victimPranks.values();
    }

    public Pranks getPrankType(String prank) {
        return Pranks.parse(prank);
    }

    public boolean hasPrank(Victim victim, Pranks prankType) {
        final EnumMap<Pranks, Prank> pranks = activePranks.get(victim);
        return pranks != null && pranks.containsKey(prankType);
    }

    public void addPlayer(Victim victim, Pranks prankType, @NotNull String... args) throws Exception {
        if (victim == null || prankType == null)
            return;

        final EnumMap<Pranks, Prank> victimPranks = activePranks.computeIfAbsent(victim, key -> new EnumMap<>(Pranks.class));
        if (victimPranks.containsKey(prankType))
            return;

        final Prank prank = createPrank(prankType, victim, args);
        victimPranks.put(prankType, prank);

        try {
            prank.start();
            // "Single-use" pranks may self-remove inside start
            if (prank.getType() == null || !victimPranks.containsKey(prankType))
                victimPranks.remove(prankType);
        } catch (Exception ex) {
            victimPranks.remove(prankType);
            throw ex;
        }

        if (victimPranks.isEmpty())
            activePranks.remove(victim);
    }

    public void removePlayer(Victim victim, Pranks prankType) throws Exception {
        if (victim == null || prankType == null)
            return;

        final EnumMap<Pranks, Prank> victimPranks = activePranks.get(victim);
        if (victimPranks == null)
            return;

        final Prank prank = victimPranks.remove(prankType);
        if (prank != null)
            prank.stop();

        if (victimPranks.isEmpty())
            activePranks.remove(victim);
    }

    public void removeAll(Victim victim) {
        if (victim == null)
            return;

        final EnumMap<Pranks, Prank> victimPranks = activePranks.remove(victim);
        if (victimPranks == null)
            return;

        for (Prank prank : victimPranks.values()) {
            try {
                prank.stop();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to stop prank " + prank.getId() + " for " + victim.getPlayer().getName());
                ex.printStackTrace();
            }
        }
    }

    private Prank createPrank(Pranks prankType, Victim victim, @NotNull String... args) throws Exception {
        return switch (prankType) {
            case CAT -> new Cat(victim);
            case CHRISTMAS_HAT -> new ChristmasHat(victim);
            case CREDITS -> new Credits(victim);
            case DANCE -> new Dance(victim);
            case DEMO -> new Demo(victim);
            case DROP_ITEM -> new DropItem(victim);
            case HOLE -> new Hole(victim);
            case LAG_MOVE -> new LagMove(victim);
            case MIDAS_TOUCH -> new MidasTouch(victim);
            case MINIGUN -> new Minigun(victim);
            case NO_DAMAGE -> new NoDamage(victim);
            case REACH -> new Reach(victim, args);
            case SCHIZO -> new Schizo(victim);
            case SELF_SHOOT -> new SelfShoot(victim);
            case SPAM -> new Spam(victim);
            case SWIM -> new Swim(victim);
            case SHAKE -> new Shake(victim);
            case POSE -> new Pose(victim, args);
            case TNT_WALK -> new TNTWalk(victim);
            case UNDERCOVER -> new Undercover(victim);
            case UPSIDE_DOWN -> new UpsideDown(victim);
            case WITNESS_PROTECTION -> new WitnessProtection(victim, args);
            case CAGE -> new Cage(victim, args);
            case POPULAR -> new Popular(victim);
            case JUMPSCARE -> new Jumpscare(victim);
            case DISK_SPACE_WARNING -> new DiskSpaceWarning(victim);
            case TIMELAPSE -> new Timelapse(victim);
            case FORCEFIELD -> new Forcefield(victim, args);
            case WEAK_ARMOR -> new WeakArmor(victim);
            case RESPECT -> new Respect(victim, args);
            case MORPH -> new Morph(victim, args);
            case FORCE -> new Force(victim);
        };
    }

}
