package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TNTWalk extends Prank {

    public TNTWalk(Victim victim) {
        super(Pranks.TNT_WALK, victim);
    }

    private final Set<Integer> entities = new HashSet<>();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo().getBlockX() != event.getFrom().getBlockX() || event.getTo().getBlockZ() != event.getFrom().getBlockZ()) {
            if (!isTarget(event.getPlayer())) return;
            handleTNT(event);
        }
    }

    final int explodeDelayTicks = 40;

    private void handleTNT(PlayerMoveEvent event) {
        final int id = getRandomID(event.getPlayer().getWorld());
        final UUID uuid = getRandomUUID();

        plugin.getPacketHandler().spawnTNT(event.getPlayer(), id, uuid, event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), explodeDelayTicks);

        entities.add(id);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);

        Tasks.runLaterAsync(explodeDelayTicks, () -> {
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

            plugin.getPacketHandler().explosion(event.getPlayer(), event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
        });
    }

    private int getRandomID(World world) {
        int id = ThreadLocalRandom.current().nextInt();
        if (world.getEntities().stream().anyMatch(e -> e.getEntityId() == id))
            return getRandomID(world);
        else
            return id;
    }

    private UUID getRandomUUID() {
        UUID uuid = UUID.randomUUID();
        if (plugin.getServer().getEntity(uuid) != null)
            return getRandomUUID();
        else
            return uuid;
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {
        if (entities.isEmpty()) {
            return;
        }
        final int[] ids = entities.stream().mapToInt(Integer::intValue).toArray();

        plugin.getPacketHandler().destroyEntities(getPlayer(), ids);

        entities.clear();
    }

}
