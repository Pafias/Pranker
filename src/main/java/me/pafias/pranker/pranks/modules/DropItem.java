package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DropItem extends Prank {

    public DropItem() {
        super("dropitem", "Drop Item", "Makes the player occasionally drop the item they are holding");
        tasks = new HashMap<>();
    }

    Map<UUID, BukkitTask> tasks;

    @Override
    public void playerAdded(Victim player) {
        tasks.put(player.getPlayer().getUniqueId(), new BukkitRunnable() {
            @Override
            public void run() {
                player.getPlayer().dropItem(true);
            }
        }.runTaskTimer(plugin, 2, 50));
    }

    @Override
    public void playerRemoved(Victim player) {
        tasks.get(player.getPlayer().getUniqueId()).cancel();
        tasks.remove(player.getPlayer().getUniqueId());
    }

}
