package me.pafias.pranker.pranks.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

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

    private void handleTNT(PlayerMoveEvent event) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        int id = getRandomID(event.getPlayer().getWorld());
        packet.getIntegers().write(0, id);
        UUID uuid = getRandomUUID();
        packet.getUUIDs().write(0, uuid);
        packet.getEntityTypeModifier().write(0, EntityType.PRIMED_TNT);
        packet.getDoubles().write(0, event.getTo().getX());
        packet.getDoubles().write(1, event.getTo().getY());
        packet.getDoubles().write(2, event.getTo().getZ());
        ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), packet);
        Set<Integer> set = entities.getOrDefault(event.getPlayer().getUniqueId(), new HashSet<>());
        set.add(id);
        entities.put(event.getPlayer().getUniqueId(), set);
        Entity tnt = event.getPlayer().getWorld().getEntity(uuid);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);
        PacketContainer packet2 = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet2.getIntegers().write(0, id);
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Integer.class);
        watcher.setEntity(tnt);
        int explodeDelayTicks = 40;
        watcher.setObject(8, serializer, explodeDelayTicks); // explode after 40 ticks (2 seconds)
        packet2.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
        ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), packet2);
        new BukkitRunnable() {
            @Override
            public void run() {
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                PacketContainer packet3 = new PacketContainer(PacketType.Play.Server.EXPLOSION);
                packet3.getFloat().write(0, (float) event.getTo().getX());
                packet3.getFloat().write(1, (float) event.getTo().getY());
                packet3.getFloat().write(2, (float) event.getTo().getZ());
                packet3.getFloat().write(3, 2f);
                ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), packet3);
            }
        }.runTaskLater(plugin, explodeDelayTicks);
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
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        int[] ids = set.stream().mapToInt(Integer::intValue).toArray();
        packet.getModifier().write(0, new IntArrayList(ids));
        ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), packet);
        entities.remove(player.getPlayer().getUniqueId());
    }

}
