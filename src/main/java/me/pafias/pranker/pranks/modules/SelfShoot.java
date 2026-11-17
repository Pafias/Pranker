package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public class SelfShoot extends Prank {

    public SelfShoot(Victim victim) {
        super(Pranks.SELF_SHOOT, victim);
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {
        ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player sourcePlayer)) return;
        if (!isTarget(sourcePlayer)) return;
        if (event.getHitEntity() instanceof LivingEntity le) {
            event.setCancelled(true);
            le.launchProjectile(event.getEntity().getClass(), sourcePlayer.getEyeLocation().getDirection().multiply(-1));
            event.getEntity().remove();
        }
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }

}
