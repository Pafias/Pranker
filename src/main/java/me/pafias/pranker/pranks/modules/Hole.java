package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.ArrayList;
import java.util.List;

public class Hole extends Prank {

    private static final int RADIUS = 3;
    private static final int MIN_Y = -50;

    public Hole(Victim victim) {
        super(Pranks.HOLE, victim);
    }

    // Stored bottom-up so restoring in order always gives each block its support first
    private final List<BlockState> changedBlocks = new ArrayList<>();
    private World world;
    private int minX, maxX, minZ, maxZ;

    @Override
    public void onStart() {
        final Location loc = getPlayer().getLocation();
        world = loc.getWorld();
        minX = loc.getBlockX() - RADIUS;
        maxX = loc.getBlockX() + RADIUS;
        minZ = loc.getBlockZ() - RADIUS;
        maxZ = loc.getBlockZ() + RADIUS;
        final int bottom = Math.max(MIN_Y, world.getMinHeight());

        for (int y = bottom; y <= loc.getBlockY(); y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Block block = world.getBlockAt(x, y, z);
                    if (block.getType().isAir()) continue;
                    // BlockState keeps full block data (orientation, waterlogging, ...) and tile entity contents
                    changedBlocks.add(block.getState());
                }
            }
        }
        // Clear top-down so nothing is left hanging over an already-cleared block
        for (int i = changedBlocks.size() - 1; i >= 0; i--)
            changedBlocks.get(i).getBlock().setType(Material.AIR, false);

        Tasks.runLaterSync(7 * 20, () -> {
            try {
                plugin.getPrankManager().removePlayer(getVictim(), getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /*
     * Gravity blocks (sand, gravel, concrete powder, ...) schedule a fall tick when placed or
     * updated, regardless of applyPhysics. Any that fall into the hole get lost when it's restored,
     * so prevent them from turning into falling blocks anywhere in/around the hole while it's open.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFall(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        final Block block = event.getBlock();
        if (!block.getWorld().equals(world)) return;
        if (block.getX() < minX - 1 || block.getX() > maxX + 1) return;
        if (block.getZ() < minZ - 1 || block.getZ() > maxZ + 1) return;
        // Only block the "start falling" transition (block -> air), not landings elsewhere
        if (event.getTo().isAir() && !block.getType().isAir())
            event.setCancelled(true);
    }

    @Override
    public void onStop() {
        // Restore bottom-up, without physics, so gravity/attached blocks always have their support in place
        for (BlockState state : changedBlocks)
            state.update(true, false);
        changedBlocks.clear();
    }

}
