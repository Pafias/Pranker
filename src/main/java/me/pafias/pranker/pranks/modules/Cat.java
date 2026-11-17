package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class Cat extends Prank {

    public Cat(Victim victim) {
        super(Pranks.CAT, victim);
    }

    private ItemStack[] contents;
    private org.bukkit.entity.Cat kitty;
    private BukkitTask task;

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (kitty != null && isTarget(event.getPlayer())) {
            kitty.setSitting(event.isSneaking());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (kitty != null && event.getEntity() instanceof org.bukkit.entity.Cat cat && kitty.equals(cat)) {
            event.setDamage(0);
            return;
        }
        if (event.getEntity() instanceof Player damaged && isTarget(damaged)) {
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (isTarget(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onStart() {
        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 1, false, false, false));
        contents = getPlayer().getInventory().getContents();
        getPlayer().getInventory().clear();
        kitty = (org.bukkit.entity.Cat) getPlayer().getWorld().spawnEntity(getPlayer().getLocation(), EntityType.CAT);
        getPlayer().getCollidableExemptions().add(kitty.getUniqueId());
        task = Tasks.runRepeatingAsync(1, 1, () -> {
            kitty.teleportAsync(getPlayer().getLocation());
            kitty.getLocation().setDirection(getPlayer().getLocation().getDirection());
        });
    }

    @Override
    public void onStop() {
        if (kitty != null) {
            getPlayer().getCollidableExemptions().remove(kitty.getUniqueId());
            kitty.remove();
            kitty = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (contents != null) {
            getPlayer().getInventory().setContents(contents);
            contents = null;
        }
        getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
    }

}
