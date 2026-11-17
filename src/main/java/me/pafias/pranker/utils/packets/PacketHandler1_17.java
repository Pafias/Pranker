package me.pafias.pranker.utils.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import me.pafias.pranker.utils.FakeNpc;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PacketHandler1_17 implements PacketHandler {

    @Override
    public void addToTab(Player player, FakeNpc npc) {
        WrapperPlayServerPlayerInfo.PlayerData playerData = new WrapperPlayServerPlayerInfo.PlayerData(
                npc.getNametag(),
                new UserProfile(npc.getProfile().getId(),
                        npc.getProfile().getName(),
                        npc.getProfile().getProperties()
                                .stream()
                                .map(property -> new TextureProperty(property.getName(), property.getValue(), property.getSignature()))
                                .toList()),
                GameMode.CREATIVE,
                null,
                1
        );
        WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
                Collections.singletonList(playerData)
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
    public void spawnNpc(Player player, FakeNpc npc) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSpawnPlayer(
                npc.getEntityId(),
                npc.getProfile().getId(),
                new Vector3d(
                        npc.getLocation().getX(),
                        npc.getLocation().getY(),
                        npc.getLocation().getZ()
                ),
                npc.getLocation().getYaw(),
                npc.getLocation().getPitch(),
                Collections.emptyList()
        ));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityHeadLook(
                npc.getEntityId(),
                npc.getLocation().getYaw()
        ));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityMetadata(
                npc.getEntityId(),
                Collections.singletonList(new EntityData<>(
                        17,
                        EntityDataTypes.BYTE,
                        (byte) 0x7F
                ))
        ));
    }

    @Override
    public void removeFromTab(Player player, FakeNpc npc) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER,
                new WrapperPlayServerPlayerInfo.PlayerData(null,
                        new UserProfile(npc.getProfile().getId(), null),
                        null,
                        -1
                )
        ));
    }

    @Override
    public void destroyNpc(Player player, FakeNpc... npc) {
        int[] ids = Arrays.stream(npc).mapToInt(FakeNpc::getEntityId).toArray();
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerDestroyEntities(ids));
    }

    @Override
    public void destroyEntities(Player player, int... ids) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerDestroyEntities(ids));
    }

    @Override
    public void rotate(Player player, FakeNpc npc, float yaw, float pitch) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityHeadLook(
                npc.getEntityId(), yaw
        ));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityRotation(
                npc.getEntityId(), yaw, pitch, true
        ));
    }

    @Override
    public void changeEquipment(Player player, FakeNpc npc, Equipment... equipment) {
        if (equipment == null || equipment.length == 0) return;
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityEquipment(
                npc.getEntityId(), Arrays.asList(equipment)
        ));
    }

    @Override
    public void teleport(Player player, FakeNpc npc, Location location) {
        teleportEntity(player, npc.getEntityId(), location);
    }

    @Override
    public void teleportEntity(Player player, int id, Location location) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityTeleport(
                id,
                new Vector3d(
                        location.getX(),
                        location.getY(),
                        location.getZ()
                ),
                location.getYaw(),
                location.getPitch(),
                true
        ));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityHeadLook(
                id,
                location.getYaw()
        ));
    }

    @Override
    public void setSwimming(Player player, FakeNpc fakeNpc, boolean swimming) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityMetadata(
                fakeNpc.getEntityId(),
                Collections.singletonList(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, swimming ? EntityPose.SWIMMING : EntityPose.STANDING))
        ));
    }

    @Override
    public void spawnTNT(Player player, int id, UUID uuid, double x, double y, double z, int explodeDelayTicks) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSpawnLivingEntity(
                id,
                uuid,
                EntityTypes.TNT,
                new com.github.retrooper.packetevents.protocol.world.Location(x, y, z, 0, 0),
                0,
                Vector3d.zero(),
                Collections.singletonList(new EntityData<>(8, EntityDataTypes.INT, explodeDelayTicks))
        ));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityMetadata(
                id,
                Collections.singletonList(new EntityData<>(8, EntityDataTypes.INT, explodeDelayTicks))
        ));
    }

    @Override
    public void explosion(Player player, double x, double y, double z) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerExplosion(
                new Vector3d(
                        x,
                        y,
                        z
                ),
                null
        ));
    }

    @Override
    public void spawnGuardianBeam(Player player, int id, UUID uuid, Location location, int targetId) {
        final com.github.retrooper.packetevents.protocol.world.Location position = new com.github.retrooper.packetevents.protocol.world.Location(
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()
        );
        final List<EntityData<?>> metadata = Arrays.asList(
                new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20), // Invisible
                new EntityData<>(5, EntityDataTypes.BOOLEAN, true), // No gravity
                new EntityData<>(17, EntityDataTypes.INT, targetId) // Beam target
        );
        // Living entities got merged into the regular spawn packet in 1.19
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_19))
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSpawnLivingEntity(
                    id, uuid, EntityTypes.GUARDIAN, position, location.getYaw(), Vector3d.zero(), metadata
            ));
        else
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSpawnEntity(
                    id, Optional.of(uuid), EntityTypes.GUARDIAN, position.getPosition(),
                    location.getPitch(), location.getYaw(), location.getYaw(), 0, Optional.of(Vector3d.zero())
            ));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityMetadata(id, metadata));
    }

}
