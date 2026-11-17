package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class WeakArmor extends Prank {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public WeakArmor(Victim victim) {
        super(Pranks.WEAK_ARMOR, victim);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isTarget(player)) return;
        final PlayerInventory inventory = player.getInventory();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            final ItemStack item = inventory.getItem(slot);
            if (item.isEmpty()) continue;
            inventory.setItem(slot, null);
            player.broadcastSlotBreak(slot);
        }
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }
}
