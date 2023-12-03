package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChristmasHat extends Prank {

    public ChristmasHat() {
        super("christmashat", "Christmas Hat", "Gives the player a fancy christmas hat made of particles (beware fps drops)");
        tasks = new HashMap<>();
    }

    Map<UUID, BukkitTask> tasks;

    @Override
    public void playerAdded(Victim player) {
        tasks.put(player.getPlayer().getUniqueId(), new BukkitRunnable() {
            @Override
            public void run() {
                Location loc = player.getPlayer().getEyeLocation().add(0, 0.3, 0);
                for (int i = 0; i < 12; i++) {
                    double cos = Math.cos(Math.toRadians(360 / 12.0 * i));
                    double sin = Math.sin(Math.toRadians(360 / 12.0 * i));
                    particle(loc.clone().add(0.25 * cos, 0.1, 0.25 * sin), true);
                    particle(loc.clone().add(0.16 * cos, 0.2, 0.16 * sin), true);
                    particle(loc.clone().add(0.07 * cos, 0.3, 0.07 * sin), true);
                    particle(loc.clone().add(0.07 * cos, 0.4, 0.07 * sin), true);
                    particle(loc.clone().add(0.35 * cos, 0, 0.35 * sin), false);
                }
                for (int i = 0; i < 5; i++) {
                    particle(loc.clone().add((Math.random() - 0.5) / 10, 0.46, (Math.random() - 0.5) / 10), false);
                }
            }
        }.runTaskTimer(plugin, 2, 1));
    }

    private void particle(Location loc, boolean red) {
        loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 128, new Particle.DustOptions(red ? Color.RED : Color.WHITE, 1));
    }

    @Override
    public void playerRemoved(Victim player) {
        tasks.get(player.getPlayer().getUniqueId()).cancel();
        tasks.remove(player.getPlayer().getUniqueId());
    }

}
