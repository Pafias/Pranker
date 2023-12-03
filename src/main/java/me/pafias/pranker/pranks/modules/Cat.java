package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cat extends Prank {

    public Cat() {
        super("cat", "Cat", "Turns the player into a cat");
        contents = new HashMap<>();
        cats = new HashMap<>();
        tasks = new HashMap<>();
    }

    Map<UUID, ItemStack[]> contents;

    Map<UUID, org.bukkit.entity.Cat> cats;
    Map<UUID, BukkitTask> tasks;

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (cats.containsKey(event.getPlayer().getUniqueId()))
            cats.get(event.getPlayer().getUniqueId()).setSitting(event.isSneaking());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if ((event.getEntity() instanceof org.bukkit.entity.Cat && cats.containsValue(((org.bukkit.entity.Cat) event.getEntity()))) || (event.getEntity() instanceof Player && getBukkitPlayers().contains(((Player) event.getEntity()))))
            event.setDamage(0);
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (getBukkitPlayers().contains(event.getPlayer()))
            event.setCancelled(true);
    }

    @Override
    public void playerAdded(Victim player) {
        player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 1, false, false, false));
        contents.put(player.getPlayer().getUniqueId(), player.getPlayer().getInventory().getContents());
        player.getPlayer().getInventory().clear();
        org.bukkit.entity.Cat kitty = (org.bukkit.entity.Cat) player.getPlayer().getWorld().spawnEntity(player.getPlayer().getLocation(), EntityType.CAT);
        cats.put(player.getPlayer().getUniqueId(), kitty);
        player.getPlayer().getCollidableExemptions().add(kitty.getUniqueId());
        tasks.put(player.getPlayer().getUniqueId(), new BukkitRunnable() {
            @Override
            public void run() {
                kitty.teleportAsync(player.getPlayer().getLocation());
                kitty.getLocation().setDirection(player.getPlayer().getLocation().getDirection());
            }
        }.runTaskTimerAsynchronously(plugin, 1, 1));
    }

    @Override
    public void playerRemoved(Victim player) {
        org.bukkit.entity.Cat kitty = cats.get(player.getPlayer().getUniqueId());
        player.getPlayer().getCollidableExemptions().remove(kitty.getUniqueId());
        tasks.get(player.getPlayer().getUniqueId()).cancel();
        tasks.remove(player.getPlayer().getUniqueId());
        kitty.remove();
        cats.remove(player.getPlayer().getUniqueId());
        player.getPlayer().getInventory().setContents(contents.get(player.getPlayer().getUniqueId()));
        contents.remove(player.getPlayer().getUniqueId());
        player.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
    }

}
