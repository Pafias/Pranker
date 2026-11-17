package me.pafias.pranker.pranks.modules;

import lombok.AllArgsConstructor;
import me.pafias.pranker.pranks.Pranks;
import lombok.Data;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.List;

public class MidasTouch extends Prank {

    public MidasTouch(Victim victim) {
        super(Pranks.MIDAS_TOUCH, victim);
    }

    private final List<MidasChange> changes = new ArrayList<>();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!isTarget(event.getPlayer())) return;
        Block block = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (block.isEmpty()) return;
        if (block.isLiquid()) return;
        BlockData oldData = block.getBlockData();
        BlockData newData = plugin.getServer().createBlockData(Material.GOLD_BLOCK);
        event.getPlayer().sendBlockChange(block.getLocation(), newData);
        changes.add(new MidasChange(block.getLocation(), oldData, newData));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isTarget(event.getPlayer())) return;
        Block target = event.getClickedBlock() != null ? event.getClickedBlock() : event.getPlayer().getTargetBlockExact(3);
        if (target == null || target.isLiquid()) return;
        BlockData oldData = target.getBlockData();
        BlockData newData = plugin.getServer().createBlockData(Material.GOLD_BLOCK);
        event.getPlayer().sendBlockChange(target.getLocation(), newData);
        changes.add(new MidasChange(target.getLocation(), oldData, newData));
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {
        for (MidasChange change : changes) {
            getPlayer().sendBlockChange(change.getTarget(), change.getOldData());
        }
        changes.clear();
    }

    @Data
    @AllArgsConstructor
    private static class MidasChange {
        private Location target;
        private BlockData oldData;
        private BlockData newData;
    }

}
