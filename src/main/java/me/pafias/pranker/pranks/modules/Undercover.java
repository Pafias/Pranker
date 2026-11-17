package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;

public class Undercover extends Prank {

    public Undercover(Victim victim) {
        super(Pranks.UNDERCOVER, victim);
    }

    private ArmorStand[] armorStands;
    private BukkitTask task;

    @Override
    public void onStart() {
        armorStands = new ArmorStand[13];
        for (int i = 0; i < 13; i++) {
            ArmorStand armorStand = (ArmorStand) getPlayer().getWorld().spawnEntity(getPlayer().getLocation(), EntityType.ARMOR_STAND);
            armorStand.setVisible(false);
            armorStand.setSmall(false);
            armorStand.setGravity(false);
            armorStand.setHelmet(new ItemStack(Material.OAK_LEAVES));
            armorStands[i] = armorStand;
        }
        task = Tasks.runRepeatingSync(2, 1, () -> {
            if (armorStands[0].isDead()) task.cancel();
            armorStands[0].teleport(getPlayer().getEyeLocation().add(0, -1.7, 0));
            armorStands[1].teleport(getPlayer().getEyeLocation().add(0.1, -2.3, 0.1));
            armorStands[2].teleport(getPlayer().getEyeLocation().add(-0.1, -2.3, 0.1));
            armorStands[3].teleport(getPlayer().getEyeLocation().add(-0.1, -2.3, -0.1));
            armorStands[4].teleport(getPlayer().getEyeLocation().add(0.1, -2.3, -0.1));
            armorStands[5].teleport(getPlayer().getLocation().add(0.2, -1.2, 0.2));
            armorStands[6].teleport(getPlayer().getLocation().add(0.2, -1.2, 0));
            armorStands[7].teleport(getPlayer().getLocation().add(0.2, -1.2, -0.2));
            armorStands[8].teleport(getPlayer().getLocation().add(0, -1.2, 0.2));
            armorStands[9].teleport(getPlayer().getLocation().add(0, -1.2, -0.2));
            armorStands[10].teleport(getPlayer().getLocation().add(-0.2, -1.2, 0.2));
            armorStands[11].teleport(getPlayer().getLocation().add(-0.2, -1.2, 0));
            armorStands[12].teleport(getPlayer().getLocation().add(-0.2, -1.2, -0.2));
        });
    }

    @Override
    public void onStop() {
        if (armorStands != null) {
            Arrays.stream(armorStands).forEach(ArmorStand::remove);
            armorStands = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

}
