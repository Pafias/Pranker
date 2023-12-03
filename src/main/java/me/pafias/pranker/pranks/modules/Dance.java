package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class Dance extends Prank {

    public Dance() {
        super("dance", "Dance", "Forces the player to dance :catvibe: (Fortnite boogie bomb)");
        balls = new HashMap<>();
        tasks = new HashMap<>();
    }

    Map<UUID, ArmorStand> balls;
    Map<UUID, BukkitTask> tasks;

    @Override
    public void playerAdded(Victim player) {
        player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000000, 3, false, false, false));
        ArmorStand armorStand = (ArmorStand) player.getPlayer().getWorld().spawnEntity(player.getPlayer().getLocation().add(0, 3, 0), EntityType.ARMOR_STAND);
        balls.put(player.getPlayer().getUniqueId(), armorStand);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setSmall(false);
        List<Material> glasses = Arrays.stream(Material.values()).filter(m -> m.name().contains("STAINED_GLASS") && !m.name().contains("PANE") && !m.name().contains("LEGACY")).toList();
        armorStand.setHelmet(new ItemStack(glasses.get(new Random().nextInt(glasses.size()))));
        tasks.put(player.getPlayer().getUniqueId(), new BukkitRunnable() {
            int time = 200;
            int step = 0;
            boolean drum;

            @Override
            public void run() {
                armorStand.setHeadPose(armorStand.getHeadPose().add(0, 0.2, 0));
                armorStand.teleport(player.getPlayer().getLocation().add(0, 3, 0));
                armorStand.setHelmet(new ItemStack(glasses.get(new Random().nextInt(glasses.size()))));
                Location loc = armorStand.getEyeLocation();
                loc.getWorld().spawnParticle(Particle.SPELL, loc, 64);
                loc.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 64);
                loc.getWorld().spawnParticle(Particle.NOTE, loc.clone().add((new Random().nextDouble() - 0.5) * 4, (new Random().nextDouble() - 0.5) * 3, (new Random().nextDouble() - 0.5) * 4), 1);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, 0.7f, new Random().nextFloat());
                if (time % 5 == 0) {
                    armorStand.getWorld().playSound(armorStand.getLocation(), drum ? Sound.BLOCK_NOTE_BLOCK_BASEDRUM : Sound.BLOCK_NOTE_BLOCK_SNARE, 0.7f, 0.5f);
                    player.getPlayer().setSneaking(drum);
                    drum = !drum;
                }
                step++;
                float length = 10.0F;
                int min = -5;
                int max = 5;
                int rz = (int) (Math.random() * max - min + min);
                int rx = (int) (Math.random() * max - min + min);
                double miny = -5.0D;
                double maxy = -1.0D;
                double ry = Math.random() * (maxy - miny) + miny;
                Vector direction = new Vector(rx, ry, rz).normalize();
                double rotation = step * 0.07853981633974483D;
                direction.setY(-Math.abs(direction.getY()));

                for (int i = 0; i < 80; i++) {
                    float ratio = i * length / 80.0F;
                    Vector v = direction.clone().multiply(ratio);
                    rotateVector(v, rotation, rotation, rotation);
                    loc.getWorld().spawnParticle(Particle.REDSTONE, loc.add(v), 128, new Random().nextFloat(), new Random().nextFloat(), new Random().nextFloat(), new Particle.DustOptions(Color.fromRGB(new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)), 1));
                    loc.subtract(v);
                }
                time--;
                if (time < 0)
                    time = 200;
            }
        }.runTaskTimer(plugin, 1, 1));
    }

    private Vector rotateAroundAxisX(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double y = v.getY() * cos - v.getZ() * sin;
        double z = v.getY() * sin + v.getZ() * cos;
        return v.setY(y).setZ(z);
    }

    private Vector rotateAroundAxisY(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos + v.getZ() * sin;
        double z = v.getX() * -sin + v.getZ() * cos;
        return v.setX(x).setZ(z);
    }

    private Vector rotateAroundAxisZ(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos - v.getY() * sin;
        double y = v.getX() * sin + v.getY() * cos;
        return v.setX(x).setY(y);
    }

    private Vector rotateVector(Vector v, double angleX, double angleY, double angleZ) {
        rotateAroundAxisX(v, angleX);
        rotateAroundAxisY(v, angleY);
        rotateAroundAxisZ(v, angleZ);
        return v;
    }

    @Override
    public void playerRemoved(Victim player) {
        player.getPlayer().removePotionEffect(PotionEffectType.SLOW);
        tasks.get(player.getPlayer().getUniqueId()).cancel();
        tasks.remove(player.getPlayer().getUniqueId());
        player.getPlayer().setSneaking(false);
        balls.get(player.getPlayer().getUniqueId()).remove();
        balls.remove(player.getPlayer().getUniqueId());
    }

}
