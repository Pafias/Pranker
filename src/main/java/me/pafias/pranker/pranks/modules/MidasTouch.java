package me.pafias.pranker.pranks.modules;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MidasTouch extends Prank {

    public MidasTouch() {
        super("midastouch", "Midas Touch", "Turns everything the player touches into gold. (Client-sided)");
    }

    private final Map<Player, List<MidasChange>> changes = new HashMap<>();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Block block = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (block.isEmpty()) return;
        if (block.isLiquid()) return;
        if (!getBukkitPlayers().contains(event.getPlayer())) return;
        BlockData oldData = block.getBlockData();
        BlockData newData = plugin.getServer().createBlockData(Material.GOLD_BLOCK);
        event.getPlayer().sendBlockChange(block.getLocation(), newData);
        List<MidasChange> list = changes.getOrDefault(event.getPlayer(), new ArrayList<>());
        list.add(new MidasChange(block.getLocation(), oldData, newData));
        changes.put(event.getPlayer(), list);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!getBukkitPlayers().contains(event.getPlayer())) return;
        Block target = event.getClickedBlock() != null ? event.getClickedBlock() : event.getPlayer().getTargetBlockExact(3);
        if (target == null || target.isLiquid()) return;
        BlockData oldData = target.getBlockData();
        BlockData newData = plugin.getServer().createBlockData(Material.GOLD_BLOCK);
        event.getPlayer().sendBlockChange(target.getLocation(), newData);
        List<MidasChange> list = changes.getOrDefault(event.getPlayer(), new ArrayList<>());
        list.add(new MidasChange(target.getLocation(), oldData, newData));
        changes.put(event.getPlayer(), list);
    }

    @Override
    public void playerAdded(Victim player) {

    }

    @Override
    public void playerRemoved(Victim player) {
        if (changes.containsKey(player.getPlayer())) {
            changes.get(player.getPlayer()).forEach(change -> {
                player.getPlayer().sendBlockChange(change.getTarget(), change.getOldData());
            });
            changes.remove(player.getPlayer());
        }
    }

    @Data
    @AllArgsConstructor
    private static class MidasChange {
        private Location target;
        private BlockData oldData;
        private BlockData newData;
    }

}
