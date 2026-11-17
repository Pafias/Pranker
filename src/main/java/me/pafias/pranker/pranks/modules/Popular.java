package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.CC;
import me.pafias.putils.Tasks;
import org.bukkit.Location;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Popular extends Prank {

    public Popular(Victim victim) {
        super(Pranks.POPULAR, victim);
    }

    private List<Villager> villagers = new ArrayList<>();
    private BukkitTask task;
    private BukkitTask pathFinding;

    @Override
    public void onStart() throws Exception {
        task = Tasks.runRepeatingSync(2, 20, () -> {
            if (villagers.size() < 10) {
                final Location playerLocation = getPlayer().getLocation();
                final Vector playerDirection = getPlayer().getLocation().getDirection();
                final Location spawnLocation = playerLocation.add(playerDirection.multiply(-5));
                final int highestY = spawnLocation.getWorld().getHighestBlockYAt(spawnLocation);
                spawnLocation.setY(highestY);
                spawnLocation.getWorld().spawn(spawnLocation, Villager.class, villager -> {
                    villagers.add(villager);
                    villager.setCustomNameVisible(true);
                    villager.customName(CC.a("&7&lFan of " + getPlayer().getName()));
                });
            }
        });
        pathFinding = Tasks.runRepeatingSync(20, 10, () -> {
            villagers.removeIf(villager -> !villager.isValid());
            for (Villager villager : villagers) {
                if (!villager.getPathfinder().hasPath())
                    villager.getPathfinder().moveTo(getPlayer());
            }
        });
    }

    @EventHandler
    public void onVillager(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof Villager villager && villagers.contains(villager)) {
            event.setCancelled(true);
            villager.shakeHead();
        }
    }

    @Override
    public void onStop() throws Exception {
        task.cancel();
        pathFinding.cancel();
        for (Villager villager : villagers) {
            villager.remove();
        }
    }

}
