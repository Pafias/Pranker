package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Minigun extends Prank {

    public Minigun() {
        super("minigun", "Minigun", "Makes the player shoot arrows like a minigun");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!getBukkitPlayers().contains(event.getPlayer())) return;
        if (event.hasItem() && event.getItem().getType().equals(Material.BOW) && (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
            event.getPlayer().launchProjectile(Arrow.class, event.getPlayer().getEyeLocation().getDirection());
            new BukkitRunnable() {
                int i = 0;

                @Override
                public void run() {
                    i++;
                    event.getPlayer().launchProjectile(Arrow.class, event.getPlayer().getEyeLocation().getDirection());
                    if (i > 2)
                        cancel();
                }
            }.runTaskTimer(plugin, 1, 1);
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    @Override
    public void playerAdded(Victim player) {

    }

    @Override
    public void playerRemoved(Victim player) {

    }

}
