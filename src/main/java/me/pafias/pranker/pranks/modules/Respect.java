package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players near the victim bow (crouch, look down, face the victim), but only from the victim's perspective.
 * The server's own rotation/pose packets for those players are rewritten so they can't undo the bow.
 */
public class Respect extends Prank {

    private static final int POSE_INDEX = 6;
    private static final float BOW_PITCH = 90F;

    public Respect(Victim victim, String... args) {
        super(Pranks.RESPECT, victim, args);
        radius = intArg(0, 10, 1, 50, "radius");
    }

    private final int radius;
    // Entity id -> yaw facing the victim. Read from netty threads
    private final Map<Integer, Float> bowing = new ConcurrentHashMap<>();
    private PacketListenerCommon listener;
    private BukkitTask task;

    @Override
    public void onStart() throws Exception {
        final UUID uuid = getPlayer().getUniqueId();

        listener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getUser() == null || !uuid.equals(event.getUser().getUUID())) return;
                if (rewrite(event))
                    event.markForReEncode(true);
            }
        }, PacketListenerPriority.NORMAL);

        task = Tasks.runRepeatingSync(0, 2, () -> {
            final Player victim = getPlayer();
            if (victim == null || !victim.isOnline()) return;
            final Location center = victim.getLocation();
            final double radiusSquared = radius * radius;
            final Set<Integer> inRange = new HashSet<>();

            for (Player player : center.getWorld().getPlayers()) {
                if (player.equals(victim)) continue;
                if (player.getGameMode() == GameMode.SPECTATOR) continue;
                if (!victim.canSee(player)) continue;
                if (player.getLocation().distanceSquared(center) > radiusSquared) continue;

                final float yaw = yawTowards(player.getLocation(), center);
                final boolean started = bowing.put(player.getEntityId(), yaw) == null;
                inRange.add(player.getEntityId());
                if (started)
                    sendPose(player.getEntityId(), EntityPose.CROUCHING);
                // Resend every run so they keep facing the victim as either one moves
                sendRotation(player.getEntityId(), yaw, BOW_PITCH, player.isOnGround());
            }

            // Stand back up anyone who walked out of range (or left the world)
            for (Integer id : new ArrayList<>(bowing.keySet())) {
                if (inRange.contains(id)) continue;
                bowing.remove(id);
                center.getWorld().getPlayers().stream()
                        .filter(p -> p.getEntityId() == id)
                        .findFirst()
                        .ifPresent(this::restore);
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
        if (getPlayer() != null && getPlayer().isOnline())
            for (Player player : getPlayer().getWorld().getPlayers())
                if (bowing.containsKey(player.getEntityId()))
                    restore(player);
        bowing.clear();
    }

    private boolean rewrite(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_ROTATION) {
            final WrapperPlayServerEntityRotation packet = new WrapperPlayServerEntityRotation(event);
            final Float yaw = bowing.get(packet.getEntityId());
            if (yaw == null) return false;
            packet.setYaw(yaw);
            packet.setPitch(BOW_PITCH);
            return true;
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            final WrapperPlayServerEntityRelativeMoveAndRotation packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
            final Float yaw = bowing.get(packet.getEntityId());
            if (yaw == null) return false;
            packet.setYaw(yaw);
            packet.setPitch(BOW_PITCH);
            return true;
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_HEAD_LOOK) {
            final WrapperPlayServerEntityHeadLook packet = new WrapperPlayServerEntityHeadLook(event);
            final Float yaw = bowing.get(packet.getEntityId());
            if (yaw == null) return false;
            packet.setHeadYaw(yaw);
            return true;
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
            final WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(event);
            final Float yaw = bowing.get(packet.getEntityId());
            if (yaw == null) return false;
            packet.setYaw(yaw);
            packet.setPitch(BOW_PITCH);
            return true;
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
            final WrapperPlayServerEntityPositionSync packet = new WrapperPlayServerEntityPositionSync(event);
            final Float yaw = bowing.get(packet.getId());
            if (yaw == null) return false;
            packet.setYRot(yaw);
            packet.setXRot(BOW_PITCH);
            return true;
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            final WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
            if (!bowing.containsKey(packet.getEntityId())) return false;
            boolean changed = false;
            for (EntityData<?> data : packet.getEntityMetadata()) {
                if (data.getIndex() != POSE_INDEX) continue;
                ((EntityData<EntityPose>) data).setValue(EntityPose.CROUCHING);
                changed = true;
            }
            return changed;
        }
        return false;
    }

    private void restore(Player player) {
        final Location location = player.getLocation();
        sendPose(player.getEntityId(), poseOf(player));
        sendRotation(player.getEntityId(), location.getYaw(), location.getPitch(), player.isOnGround());
    }

    private void sendPose(int entityId, EntityPose pose) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(getPlayer(), new WrapperPlayServerEntityMetadata(
                entityId,
                Collections.singletonList(new EntityData<>(POSE_INDEX, EntityDataTypes.ENTITY_POSE, pose))
        ));
    }

    private void sendRotation(int entityId, float yaw, float pitch, boolean onGround) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(getPlayer(),
                new WrapperPlayServerEntityRotation(entityId, yaw, pitch, onGround));
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(getPlayer(),
                new WrapperPlayServerEntityHeadLook(entityId, yaw));
    }

    private static float yawTowards(Location from, Location to) {
        final double dx = to.getX() - from.getX();
        final double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static EntityPose poseOf(Player player) {
        if (player.isSleeping()) return EntityPose.SLEEPING;
        if (player.isGliding()) return EntityPose.FALL_FLYING;
        if (player.isSwimming()) return EntityPose.SWIMMING;
        if (player.isRiptiding()) return EntityPose.SPIN_ATTACK;
        if (player.isSneaking()) return EntityPose.CROUCHING;
        return EntityPose.STANDING;
    }

}
