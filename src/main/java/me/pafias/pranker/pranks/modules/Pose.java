package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import lombok.AllArgsConstructor;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Forces the victim into a pose.
 * Other clients take a player's pose straight from the server (RemotePlayer#updatePlayerPose is a no-op),
 * plus a flag for the parts the renderer reads separately (spin, gliding, death).
 * The victim's own client recomputes its pose every tick from its own state, so for them we fake that state instead.
 */
public class Pose extends Prank {

    private static final int SHARED_FLAGS_INDEX = 0;
    private static final int POSE_INDEX = 6;
    private static final int LIVING_FLAGS_INDEX = 8;
    private static final int HEALTH_INDEX = 9;
    private static final int SLEEPING_POS_INDEX = 14;

    private static final byte SNEAKING_FLAG = 0x02;
    private static final byte GLIDING_FLAG = (byte) 0x80;
    private static final byte SPIN_ATTACK_FLAG = 0x04;

    private static final BlockData BARRIER = Material.BARRIER.createBlockData();

    @AllArgsConstructor
    private enum Mode {
        // The victim's own client reads sneaking from their keyboard
        CROUCHING(EntityPose.CROUCHING, false),
        // The victim crawls under a fake barrier ceiling
        SWIMMING(EntityPose.SWIMMING, true),
        // The victim gets the "Leave Bed" screen, leaving the bed ends the prank
        SLEEPING(EntityPose.SLEEPING, true),
        // The victim's own client would actually start gliding
        FALL_FLYING(EntityPose.FALL_FLYING, false),
        SPIN_ATTACK(EntityPose.SPIN_ATTACK, true),
        // The victim would see 0 hearts. Can only be undone by respawning the entity for each viewer
        DYING(EntityPose.DYING, false);

        private final EntityPose pose;
        private final boolean self;
    }

    public Pose(Victim victim, String... args) {
        super(Pranks.POSE, victim, args);
        if (!hasArgs())
            throw new IllegalArgumentException("Missing pose. Options: " + String.join(", ", poses("")));
        try {
            mode = Mode.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid pose: " + args[0]);
        }
    }

    private final Mode mode;
    private int entityId;
    private UUID uuid;
    private volatile Vector3i sleepingPos;
    private final Set<Location> barriers = new HashSet<>();
    private PacketListenerCommon listener;
    private BukkitTask task;
    private boolean leftBed;

    @Override
    public void onStart() throws Exception {
        entityId = getPlayer().getEntityId();
        uuid = getPlayer().getUniqueId();
        sleepingPos = blockPos(getPlayer().getLocation());

        listener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getUser() == null || event.getUser().getUUID() == null) return;
                final UUID viewer = event.getUser().getUUID();
                final boolean self = viewer.equals(uuid);
                if (self && !mode.self) return;
                if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                    final WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
                    if (packet.getEntityId() == entityId && rewrite(packet.getEntityMetadata(), self))
                        event.markForReEncode(true);
                } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                    // Spawn metadata leaves out default values, so there may be nothing to rewrite
                    if (new WrapperPlayServerSpawnEntity(event).getEntityId() != entityId) return;
                    Tasks.runLaterSync(1, () -> {
                        final Player player = plugin.getServer().getPlayer(viewer);
                        if (player != null && listener != null) apply(player);
                    });
                }
            }

            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (mode != Mode.SLEEPING || event.getPacketType() != PacketType.Play.Client.ENTITY_ACTION) return;
                if (event.getUser() == null || !uuid.equals(event.getUser().getUUID())) return;
                if (new WrapperPlayClientEntityAction(event).getAction() != WrapperPlayClientEntityAction.Action.LEAVE_BED)
                    return;
                Tasks.runSync(() -> {
                    if (leftBed || listener == null) return;
                    leftBed = true;
                    try {
                        plugin.getPrankManager().removePlayer(getVictim(), Pranks.POSE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }, PacketListenerPriority.NORMAL);

        for (Player player : getPlayer().getWorld().getPlayers())
            if (shows(player)) apply(player);

        if (mode == Mode.SWIMMING)
            task = Tasks.runRepeatingSync(1, 1, new Runnable() {
                int ticks;

                @Override
                public void run() {
                    updateBarriers(++ticks % 10 == 0);
                }
            });
    }

    @Override
    public void onStop() throws Exception {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (listener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            listener = null;
        }
        final Player victim = getPlayer();
        if (victim == null || !victim.isOnline()) return;
        for (Player player : victim.getWorld().getPlayers())
            if (shows(player)) restore(player);
        barriers.clear();
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (isTarget(event.getPlayer())) reapplySelf();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (isTarget(event.getPlayer())) reapplySelf();
    }

    private void reapplySelf() {
        // The victim gets a fresh client-side player, other viewers are handled by the spawn packet
        barriers.clear();
        if (mode.self)
            Tasks.runLaterSync(2, () -> {
                if (listener != null && getPlayer() != null) apply(getPlayer());
            });
    }

    private boolean shows(Player viewer) {
        return mode.self || !viewer.equals(getPlayer());
    }

    private void apply(Player viewer) {
        final Player victim = getPlayer();
        if (victim == null || !victim.isOnline() || !viewer.getWorld().equals(victim.getWorld())) return;
        if (viewer.equals(victim)) {
            switch (mode) {
                case SPIN_ATTACK -> sendData(viewer, LIVING_FLAGS_INDEX, EntityDataTypes.BYTE, (byte) (livingFlags(victim) | SPIN_ATTACK_FLAG));
                case SLEEPING -> {
                    sleepingPos = blockPos(victim.getLocation());
                    sendData(viewer, SLEEPING_POS_INDEX, EntityDataTypes.OPTIONAL_BLOCK_POSITION, Optional.of(sleepingPos));
                }
                case SWIMMING -> barriers.forEach(b -> viewer.sendBlockChange(b, BARRIER));
            }
            return;
        }
        sendData(viewer, POSE_INDEX, EntityDataTypes.ENTITY_POSE, mode.pose);
        switch (mode) {
            case CROUCHING -> sendData(viewer, SHARED_FLAGS_INDEX, EntityDataTypes.BYTE, (byte) (sharedFlags(victim) | SNEAKING_FLAG));
            case FALL_FLYING -> sendData(viewer, SHARED_FLAGS_INDEX, EntityDataTypes.BYTE, (byte) (sharedFlags(victim) | GLIDING_FLAG));
            case SPIN_ATTACK -> sendData(viewer, LIVING_FLAGS_INDEX, EntityDataTypes.BYTE, (byte) (livingFlags(victim) | SPIN_ATTACK_FLAG));
            case DYING -> sendData(viewer, HEALTH_INDEX, EntityDataTypes.FLOAT, 0F);
        }
    }

    private void restore(Player viewer) {
        final Player victim = getPlayer();
        if (viewer.equals(victim)) {
            switch (mode) {
                case SPIN_ATTACK -> sendData(viewer, LIVING_FLAGS_INDEX, EntityDataTypes.BYTE, livingFlags(victim));
                case SLEEPING -> sendData(viewer, SLEEPING_POS_INDEX, EntityDataTypes.OPTIONAL_BLOCK_POSITION, Optional.<Vector3i>empty());
                case SWIMMING -> barriers.forEach(b -> viewer.sendBlockChange(b, b.getBlock().getBlockData()));
            }
            return;
        }
        if (mode == Mode.DYING) {
            // Clients never reset the death animation, so respawn the victim for them
            if (!viewer.canSee(victim)) return;
            viewer.hidePlayer(plugin, victim);
            Tasks.runLaterSync(2, () -> {
                if (viewer.isOnline() && victim.isOnline()) viewer.showPlayer(plugin, victim);
            });
            return;
        }
        sendData(viewer, POSE_INDEX, EntityDataTypes.ENTITY_POSE, poseOf(victim));
        switch (mode) {
            case CROUCHING, FALL_FLYING -> sendData(viewer, SHARED_FLAGS_INDEX, EntityDataTypes.BYTE, sharedFlags(victim));
            case SPIN_ATTACK -> sendData(viewer, LIVING_FLAGS_INDEX, EntityDataTypes.BYTE, livingFlags(victim));
        }
    }

    private boolean rewrite(List<EntityData<?>> metadata, boolean self) {
        boolean changed = false;
        for (EntityData<?> data : metadata) {
            final int index = data.getIndex();
            if (index == LIVING_FLAGS_INDEX && mode == Mode.SPIN_ATTACK) {
                ((EntityData<Byte>) data).setValue((byte) ((Byte) data.getValue() | SPIN_ATTACK_FLAG));
                changed = true;
            } else if (self) {
                if (index == SLEEPING_POS_INDEX && mode == Mode.SLEEPING) {
                    ((EntityData<Optional<Vector3i>>) data).setValue(Optional.of(sleepingPos));
                    changed = true;
                }
            } else if (index == POSE_INDEX) {
                ((EntityData<EntityPose>) data).setValue(mode.pose);
                changed = true;
            } else if (index == SHARED_FLAGS_INDEX && (mode == Mode.CROUCHING || mode == Mode.FALL_FLYING)) {
                final byte flag = mode == Mode.CROUCHING ? SNEAKING_FLAG : GLIDING_FLAG;
                ((EntityData<Byte>) data).setValue((byte) ((Byte) data.getValue() | flag));
                changed = true;
            } else if (index == HEALTH_INDEX && mode == Mode.DYING) {
                ((EntityData<Float>) data).setValue(0F);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Puts a fake 3x3 barrier ceiling right above the victim's crawling hitbox (only for the victim).
     * Their client then can't fit them standing or crouching, so it falls back to the swimming pose,
     * same as crawling under a trapdoor. 3x3 so they can't outrun it before the server catches up.
     */
    private void updateBarriers(boolean resend) {
        final Player victim = getPlayer();
        if (victim == null || !victim.isOnline()) return;
        final Location location = victim.getLocation();
        final int y = (int) Math.ceil(location.getY() + 0.6);
        final Set<Location> next = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                final Location block = new Location(location.getWorld(), location.getBlockX() + dx, y, location.getBlockZ() + dz);
                // Something solid is already there, it works as a ceiling anyway
                if (block.getBlock().getType().isAir()) next.add(block);
            }

        for (Location old : barriers)
            if (!next.contains(old) && old.getWorld().equals(victim.getWorld()))
                victim.sendBlockChange(old, old.getBlock().getBlockData());
        for (Location block : next)
            if (resend || !barriers.contains(block))
                victim.sendBlockChange(block, BARRIER);
        barriers.clear();
        barriers.addAll(next);
    }

    private <T> void sendData(Player viewer, int index, EntityDataType<T> type, T value) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(viewer, new WrapperPlayServerEntityMetadata(
                entityId,
                Collections.singletonList(new EntityData<>(index, type, value))
        ));
    }

    private static Vector3i blockPos(Location location) {
        return new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static EntityPose poseOf(Player player) {
        if (player.getPose() == org.bukkit.entity.Pose.SNEAKING) return EntityPose.CROUCHING;
        try {
            return EntityPose.valueOf(player.getPose().name());
        } catch (IllegalArgumentException ex) {
            return EntityPose.STANDING;
        }
    }

    private static byte sharedFlags(Player player) {
        byte flags = 0;
        if (player.getFireTicks() > 0) flags |= 0x01;
        if (player.isSneaking()) flags |= SNEAKING_FLAG;
        if (player.isSprinting()) flags |= 0x08;
        if (player.isSwimming()) flags |= 0x10;
        if (player.isInvisible()) flags |= 0x20;
        if (player.isGlowing()) flags |= 0x40;
        if (player.isGliding()) flags |= GLIDING_FLAG;
        return flags;
    }

    private static byte livingFlags(Player player) {
        byte flags = 0;
        if (player.isHandRaised()) {
            flags |= 0x01;
            if (player.getHandRaised() == EquipmentSlot.OFF_HAND) flags |= 0x02;
        }
        if (player.isRiptiding()) flags |= SPIN_ATTACK_FLAG;
        return flags;
    }

    public static List<String> poses(String prefix) {
        return Arrays.stream(Mode.values())
                .map(m -> m.name().toLowerCase())
                .filter(m -> m.startsWith(prefix.toLowerCase()))
                .toList();
    }

}
