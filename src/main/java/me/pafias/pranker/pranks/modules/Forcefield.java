package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class Forcefield extends Prank {

    public Forcefield(Victim victim, String... args) {
        super(Pranks.FORCEFIELD, victim, args);
        radius = intArg(0, 5, 1, 50, "radius");
        if (args != null)
            for (int i = 1; i < args.length; i++)
                exempt.add(args[i].toLowerCase());
    }

    private final int radius;
    private final Set<String> exempt = new HashSet<>();
    private final Map<UUID, Long> lastFling = new HashMap<>();
    private BukkitTask task;

    @Override
    public void onStart() throws Exception {
        task = Tasks.runRepeatingSync(0, 2, () -> {
            final Player victim = getPlayer();
            if (victim == null || !victim.isOnline()) return;
            final Location center = victim.getLocation();
            final double radiusSquared = radius * radius;
            final long now = System.currentTimeMillis();

            for (Player player : center.getWorld().getPlayers()) {
                if (player.equals(victim)) continue;
                if (player.getGameMode() == GameMode.SPECTATOR) continue;
                if (exempt.contains(player.getName().toLowerCase())) continue;
                if (player.getLocation().distanceSquared(center) > radiusSquared) continue;
                // Give the fling time to carry them out before pushing again
                final Long last = lastFling.get(player.getUniqueId());
                if (last != null && now - last < 250) continue;

                Vector direction = player.getLocation().toVector().subtract(center.toVector()).setY(0);
                if (direction.lengthSquared() < 1.0E-4)
                    direction = player.getLocation().getDirection().multiply(-1).setY(0);
                if (direction.lengthSquared() < 1.0E-4)
                    direction = new Vector(1, 0, 0);

                player.setVelocity(direction.normalize().multiply(1.2).setY(0.4));
                lastFling.put(player.getUniqueId(), now);
            }
        });
    }

    @Override
    public void onStop() throws Exception {
        if (task != null)
            task.cancel();
        lastFling.clear();
    }

}
