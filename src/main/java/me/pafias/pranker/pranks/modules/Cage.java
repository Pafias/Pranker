package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

public class Cage extends Prank {

    public Cage(Victim victim, String... args) {
        super(Pranks.CAGE, victim, args);
        if (hasArgs()) {
            material = Material.matchMaterial(args[0]);
            if (material == null || !material.isBlock() || !material.isSolid())
                throw new IllegalArgumentException("Invalid block: " + args[0]);
        } else {
            material = Material.GLASS;
        }
    }

    private final Material material;

    private Map<Block, Material> map = new HashMap<>();

    @Override
    public void onStart() throws Exception {
        Location playerLocation = getPlayer().getLocation();
        Location center = playerLocation.toCenterLocation();
        getPlayer().teleport(center);
        for (int x = center.getBlockX() - 1; x <= center.getBlockX() + 1; x++) {
            for (int y = center.getBlockY() - 1; y <= center.getBlockY() + 2; y++) {
                for (int z = center.getBlockZ() - 1; z <= center.getBlockZ() + 1; z++) {
                    if (x == center.getBlockX() && z == center.getBlockZ()) continue;
                    Block block = playerLocation.getWorld().getBlockAt(x, y, z);
                    if (!block.getType().isAir()) continue;
                    map.put(block, block.getType());
                    block.setType(material, false);
                }
            }
        }
    }

    @Override
    public void onStop() throws Exception {
        for (Map.Entry<Block, Material> entry : map.entrySet()) {
            entry.getKey().setType(entry.getValue(), false);
        }
    }

}
