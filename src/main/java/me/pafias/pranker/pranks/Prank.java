package me.pafias.pranker.pranks;

import lombok.Getter;
import me.pafias.pranker.Pranker;
import me.pafias.pranker.Victim;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class Prank implements Listener {

    protected final Pranker plugin = Pranker.get();

    private final Pranks type;
    private final Victim victim;
    private final String[] args;

    protected Prank(Pranks type, Victim victim) {
        this.type = type;
        this.victim = victim;
        this.args = null;
    }

    protected Prank(Pranks type, Victim victim, @NotNull String... args) {
        this.type = type;
        this.victim = victim;
        this.args = args;
    }

    public String getId() {
        return type.getId();
    }

    public String getName() {
        return type.getName();
    }

    public String getDescription() {
        return type.getDescription();
    }

    public Player getPlayer() {
        return victim != null ? victim.getPlayer() : null;
    }

    protected boolean isTarget(Player player) {
        return victim != null && victim.getPlayer().equals(player);
    }

    public void start() throws Exception {
        onStart();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void stop() throws Exception {
        HandlerList.unregisterAll(this);
        onStop();
    }

    protected boolean hasArgs() {
        return args != null && args.length > 0;
    }

    protected int intArg(int index, int def, int min, int max, String name) {
        if (args == null || args.length <= index)
            return def;
        try {
            final int value = Integer.parseInt(args[index]);
            if (value < min || value > max)
                throw new IllegalArgumentException(String.format("%s must be between %d and %d", name, min, max));
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(String.format("Invalid %s: %s", name, args[index]));
        }
    }

    protected double doubleArg(int index, double def, double min, double max, String name) {
        if (args == null || args.length <= index)
            return def;
        try {
            final double value = Double.parseDouble(args[index]);
            if (!(value >= min && value <= max))
                throw new IllegalArgumentException(String.format("%s must be between %s and %s", name, min, max));
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(String.format("Invalid %s: %s", name, args[index]));
        }
    }

    public abstract void onStart() throws Exception;

    public abstract void onStop() throws Exception;

}
