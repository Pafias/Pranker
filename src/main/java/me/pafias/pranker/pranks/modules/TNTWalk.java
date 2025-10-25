package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.putils.Tasks;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TNTWalk extends Prank {

    public TNTWalk() {
        super("tntwalk", "TNT Walk", "Spawns a TNT at the player's location every time they walk. (Client-sided)");
        entities = new HashMap<>();
    }

    private final Map<UUID, Set<Integer>> entities;

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo().getBlockX() != event.getFrom().getBlockX() || event.getTo().getBlockZ() != event.getFrom().getBlockZ()) {
            if (!getBukkitPlayers().contains(event.getPlayer())) return;
            handleTNT(event);
        }
    }

    final int explodeDelayTicks = 40;

    private void handleTNT(PlayerMoveEvent event) {
        final int id = getRandomID(event.getPlayer().getWorld());
        final UUID uuid = getRandomUUID();

        WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(
                id,
                uuid,
                EntityTypes.TNT,
                new Location(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), 0, 0),
                0,
                Vector3d.zero(),
                Collections.singletonList(new EntityData<>(8, EntityDataTypes.INT, explodeDelayTicks))
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getPlayer(), packet);

        Set<Integer> set = entities.getOrDefault(event.getPlayer().getUniqueId(), new HashSet<>());
        set.add(id);
        entities.put(event.getPlayer().getUniqueId(), set);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);

        WrapperPlayServerEntityMetadata packet2 = new WrapperPlayServerEntityMetadata(
                id,
                Collections.singletonList(new EntityData<>(8, EntityDataTypes.INT, explodeDelayTicks))
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getPlayer(), packet2);

        Tasks.runLaterAsync(explodeDelayTicks, () -> {
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

            WrapperPlayServerExplosion packet3 = new WrapperPlayServerExplosion(
                    new Vector3d(
                            event.getTo().getX(),
                            event.getTo().getY(),
                            event.getTo().getZ()
                    ),
                    null
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getPlayer(), packet3);
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
    public void playerAdded(Victim player) {

    }

    @Override
    public void playerRemoved(Victim player) {
        Set<Integer> set = entities.get(player.getPlayer().getUniqueId());
        final int[] ids = set.stream().mapToInt(Integer::intValue).toArray();

        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(ids);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), packet);

        entities.remove(player.getPlayer().getUniqueId());
    }

}
