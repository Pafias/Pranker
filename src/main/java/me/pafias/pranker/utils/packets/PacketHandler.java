package me.pafias.pranker.utils.packets;

import com.github.retrooper.packetevents.protocol.player.Equipment;
import me.pafias.pranker.utils.FakeNpc;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface PacketHandler {

    void addToTab(Player player, FakeNpc npc);

    void spawnNpc(Player player, FakeNpc npc);

    void removeFromTab(Player player, FakeNpc npc);

    void destroyNpc(Player player, FakeNpc... npc);

    void destroyEntities(Player player, int... ids);

    void rotate(Player player, FakeNpc npc, float yaw, float pitch);

    void changeEquipment(Player player, FakeNpc npc, Equipment... equipment);

    void teleport(Player player, FakeNpc npc, Location location);

    void teleportEntity(Player player, int id, Location location);

    void setSwimming(Player player, FakeNpc fakeNpc, boolean swimming);

    void spawnTNT(Player player, int id, UUID uuid, double x, double y, double z, int explodeDelayTicks);

    void explosion(Player player, double x, double y, double z);

    void spawnGuardianBeam(Player player, int id, UUID uuid, Location location, int targetId);

}
