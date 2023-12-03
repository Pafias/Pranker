package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public class SelfShoot extends Prank {

    public SelfShoot() {
        super("selfshoot", "Self-shoot", "Makes the player's arrow backfire when shooting an entity. (Entity shoots arrow back at the player)");
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {
        ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player player)) return;
        if (!getBukkitPlayers().contains(player)) return;
        if (event.getHitEntity() instanceof LivingEntity le) {
            event.setCancelled(true);
            le.launchProjectile(event.getEntity().getClass(), player.getEyeLocation().getDirection().multiply(-1));
            event.getEntity().remove();
        }
    }

    @Override
    public void playerAdded(Victim player) {

    }

    @Override
    public void playerRemoved(Victim player) {

    }

}
