package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Dance extends Prank {

    public Dance(Victim victim) {
        super(Pranks.DANCE, victim);
        random = new Random();
    }

    private final Random random;
    private ArmorStand ball;
    private BukkitTask task;

    @Override
    public void onStart() {
        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1000000, 3, false, false, false));
        ball = (ArmorStand) getPlayer().getWorld().spawnEntity(getPlayer().getLocation().add(0, 3, 0), EntityType.ARMOR_STAND);
        ball.setVisible(false);
        ball.setGravity(false);
        ball.setSmall(false);
        List<Material> glasses = Arrays.stream(Material.values()).filter(m -> m.name().contains("STAINED_GLASS") && !m.name().contains("PANE") && !m.name().contains("LEGACY")).toList();
        ball.setHelmet(new ItemStack(glasses.get(new Random().nextInt(glasses.size()))));
        task = new BukkitRunnable() {
            int time = 200;
            int step = 0;
            boolean drum;

            @Override
            public void run() {
                if (ball == null || ball.isDead()) {
                    cancel();
                    return;
                }
                ball.setHeadPose(ball.getHeadPose().add(0, 0.2, 0));
                ball.teleport(getPlayer().getLocation().add(0, 3, 0));
                ball.setHelmet(new ItemStack(glasses.get(random.nextInt(glasses.size()))));
                Location loc = ball.getEyeLocation();
                loc.getWorld().spawnParticle(Particle.ENTITY_EFFECT, loc, 64, 0, 0, 0, 1, Color.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
                loc.getWorld().spawnParticle(Particle.INSTANT_EFFECT, loc, 64, 0, 0, 0, 1, new Particle.Spell(Color.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255)), 1.0f));
                loc.getWorld().spawnParticle(Particle.NOTE, loc.clone().add((random.nextDouble() - 0.5) * 4, (random.nextDouble() - 0.5) * 3, (random.nextDouble() - 0.5) * 4), 1);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, 0.7f, random.nextFloat());
                if (time % 5 == 0) {
                    ball.getWorld().playSound(ball.getLocation(), drum ? Sound.BLOCK_NOTE_BLOCK_BASEDRUM : Sound.BLOCK_NOTE_BLOCK_SNARE, 0.7f, 0.5f);
                    getPlayer().setSneaking(drum);
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
                    loc.getWorld().spawnParticle(Particle.DUST, loc.add(v), 128, random.nextFloat(), random.nextFloat(), random.nextFloat(), new Particle.DustOptions(Color.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255)), 1));
                    loc.subtract(v);
                }
                time--;
                if (time < 0)
                    time = 200;
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    @Override
    public void onStop() {
        getPlayer().removePotionEffect(PotionEffectType.SLOWNESS);
        if (task != null) {
            task.cancel();
            task = null;
        }
        getPlayer().setSneaking(false);
        if (ball != null) {
            ball.remove();
            ball = null;
        }
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


}

