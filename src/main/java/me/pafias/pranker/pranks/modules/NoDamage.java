package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class NoDamage extends Prank {

    public NoDamage() {
        super("nodamage", "No damage", "Makes the player's hits deal 0 damage");
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!getBukkitPlayers().contains(((Player) event.getDamager()))) return;
        event.setDamage(0);
    }

    @Override
    public void playerAdded(Victim player) {

    }

    @Override
    public void playerRemoved(Victim player) {

    }
}
