package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.scheduler.BukkitTask;

public class DropItem extends Prank {

    public DropItem(Victim victim) {
        super(Pranks.DROP_ITEM, victim);
    }

    private BukkitTask task;

    @Override
    public void onStart() {
        task = Tasks.runRepeatingSync(2, 50, () -> getPlayer().dropItem(true));
    }

    @Override
    public void onStop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

}
