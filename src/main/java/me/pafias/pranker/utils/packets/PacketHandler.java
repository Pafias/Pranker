package me.pafias.pranker.utils.packets;

import com.github.retrooper.packetevents.protocol.player.Equipment;
import me.pafias.pranker.utils.FakeNpc;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface PacketHandler {

    // Essential

    void addToTab(Player player, FakeNpc npc);

    void spawnNpc(Player player, FakeNpc npc);

    void removeFromTab(Player player, FakeNpc npc);

    void destroyNpc(Player player, FakeNpc... npc);

    // Extras

    void rotate(Player player, FakeNpc npc, float yaw, float pitch);

    void changeEquipment(Player player, FakeNpc npc, Equipment... equipment);

    void teleport(Player player, FakeNpc npc, Location location);

    void setSwimming(Player player, FakeNpc fakeNpc, boolean swimming);

}
