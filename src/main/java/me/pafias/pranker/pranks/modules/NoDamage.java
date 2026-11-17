package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class NoDamage extends Prank {

    public NoDamage(Victim victim) {
        super(Pranks.NO_DAMAGE, victim);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!isTarget(damager)) return;
        event.setDamage(0);
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }
}
