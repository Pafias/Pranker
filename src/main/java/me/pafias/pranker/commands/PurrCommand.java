package me.pafias.pranker.commands;

import me.pafias.pranker.Pranker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PurrCommand implements CommandExecutor {

    private static final float DEFAULT_VOLUME = 0.15F;
    private static final long DEFAULT_INTERVAL_SECONDS = 30;
    private static final double HEARING_RANGE_SQUARED = 5 * 5;

    private final Pranker plugin;
    private final Map<UUID, PurrState> purring = new HashMap<>();

    public PurrCommand(Pranker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can purr.", NamedTextColor.RED));
            return true;
        }

        if (args.length > 2) {
            player.sendMessage(Component.text("/" + label + " [volume] [seconds]", NamedTextColor.RED));
            return true;
        }

        Float volume = null;
        if (args.length >= 1) {
            try {
                volume = Float.parseFloat(args[0]);
            } catch (NumberFormatException ex) {
                player.sendMessage(Component.text("Invalid volume.", NamedTextColor.RED));
                return true;
            }
            if (volume < 0F || volume > 1F) {
                player.sendMessage(Component.text("Volume must be between 0 and 1.", NamedTextColor.RED));
                return true;
            }
        }

        Long intervalTicks = null;
        if (args.length == 2) {
            final double seconds;
            try {
                seconds = Double.parseDouble(args[1]);
            } catch (NumberFormatException ex) {
                player.sendMessage(Component.text("Invalid time.", NamedTextColor.RED));
                return true;
            }
            if (seconds <= 0) {
                player.sendMessage(Component.text("Time must be greater than 0 seconds.", NamedTextColor.RED));
                return true;
            }
            intervalTicks = Math.max(1L, Math.round(seconds * 20D));
        }

        final UUID uuid = player.getUniqueId();
        final PurrState current = purring.get(uuid);

        if (current != null) {
            if (args.length == 0) {
                current.task.cancel();
                purring.remove(uuid);
                return true;
            }

            current.volume = volume;

            if (intervalTicks != null) {
                current.task.cancel();
                current.intervalTicks = intervalTicks;
                current.task = schedule(player, current);
            }
            return true;
        }

        final float purrVolume = volume == null ? DEFAULT_VOLUME : volume;
        final long purrInterval = intervalTicks == null ? DEFAULT_INTERVAL_SECONDS * 20L : intervalTicks;

        playPurr(player, purrVolume);

        final PurrState state = new PurrState(purrVolume, purrInterval);
        purring.put(uuid, state);
        state.task = schedule(player, state);
        return true;
    }

    private BukkitTask schedule(Player player, PurrState state) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (purring.get(player.getUniqueId()) != state || !player.isOnline()) {
                    purring.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                playPurr(player, state.volume);
            }
        }.runTaskTimer(plugin, state.intervalTicks, state.intervalTicks);
    }

    private void playPurr(Player player, float volume) {
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(player.getLocation()) <= HEARING_RANGE_SQUARED)
                nearby.playSound(player.getLocation(), Sound.ENTITY_CAT_PURR, volume, 1F);
        }
    }

    private static class PurrState {

        private float volume;
        private long intervalTicks;
        private BukkitTask task;

        private PurrState(float volume, long intervalTicks) {
            this.volume = volume;
            this.intervalTicks = intervalTicks;
        }

    }

}
