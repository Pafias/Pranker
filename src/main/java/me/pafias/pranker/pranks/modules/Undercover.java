package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Undercover extends Prank {

    public Undercover() {
        super("undercover", "Undercover bush", "Turn the player into a bush (Fortnite bush)");
        armorstands = new HashMap<>();
        tasks = new HashMap<>();
    }

    Map<UUID, ArmorStand[]> armorstands;
    Map<UUID, BukkitTask> tasks;

    @Override
    public void playerAdded(Victim player) {
        ArmorStand[] armorStands = new ArmorStand[13];
        for (int i = 0; i < 13; i++) {
            ArmorStand armorstand = (ArmorStand) player.getPlayer().getWorld().spawnEntity(player.getPlayer().getLocation(), EntityType.ARMOR_STAND);
            armorstand.setVisible(false);
            armorstand.setSmall(false);
            armorstand.setGravity(false);
            armorstand.setHelmet(new ItemStack(Material.OAK_LEAVES));
            armorStands[i] = armorstand;
        }
        armorstands.put(player.getPlayer().getUniqueId(), armorStands);
        tasks.put(player.getPlayer().getUniqueId(), new BukkitRunnable() {
            @Override
            public void run() {
                if (armorStands[0].isDead()) cancel();
                armorStands[0].teleport(player.getPlayer().getEyeLocation().add(0, -1.7, 0));
                armorStands[1].teleport(player.getPlayer().getEyeLocation().add(0.1, -2.3, 0.1));
                armorStands[2].teleport(player.getPlayer().getEyeLocation().add(-0.1, -2.3, 0.1));
                armorStands[3].teleport(player.getPlayer().getEyeLocation().add(-0.1, -2.3, -0.1));
                armorStands[4].teleport(player.getPlayer().getEyeLocation().add(0.1, -2.3, -0.1));
                armorStands[5].teleport(player.getPlayer().getLocation().add(0.2, -1.2, 0.2));
                armorStands[6].teleport(player.getPlayer().getLocation().add(0.2, -1.2, 0));
                armorStands[7].teleport(player.getPlayer().getLocation().add(0.2, -1.2, -0.2));
                armorStands[8].teleport(player.getPlayer().getLocation().add(0, -1.2, 0.2));
                armorStands[9].teleport(player.getPlayer().getLocation().add(0, -1.2, -0.2));
                armorStands[10].teleport(player.getPlayer().getLocation().add(-0.2, -1.2, 0.2));
                armorStands[11].teleport(player.getPlayer().getLocation().add(-0.2, -1.2, 0));
                armorStands[12].teleport(player.getPlayer().getLocation().add(-0.2, -1.2, -0.2));
            }
        }.runTaskTimer(plugin, 2, 1));
    }

    @Override
    public void playerRemoved(Victim player) {
        Arrays.stream(armorstands.get(player.getPlayer().getUniqueId())).forEach(ArmorStand::remove);
        armorstands.remove(player.getPlayer().getUniqueId());
        tasks.get(player.getPlayer().getUniqueId()).cancel();
        tasks.remove(player.getPlayer().getUniqueId());
    }

}
