package me.pafias.pranker.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import lombok.Getter;
import me.pafias.pranker.utils.packets.PacketHandler;
import me.pafias.putils.Tasks;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FakeNpc {

    private final PacketHandler packetHandler;

    @Getter
    private final PlayerProfile profile;
    @Getter
    private final Component nametag;
    @Getter
    private final Location location;

    @Getter
    private final int entityId;

    @Getter
    private final Set<Player> viewers = ConcurrentHashMap.newKeySet();

    public FakeNpc(PacketHandler packetHandler, int entityId, PlayerProfile profile, Component nametag, Location location) {
        this.entityId = entityId;
        this.packetHandler = packetHandler;
        this.profile = profile;
        this.nametag = nametag;
        this.location = location;
    }

    public void spawnForAll() {
        Bukkit.getOnlinePlayers().forEach(this::spawn);
    }

    public void spawn(Player player) {
        if (viewers.contains(player))
            return;
        viewers.add(player);

        packetHandler.addToTab(player, this);
        packetHandler.spawnNpc(player, this);
        Tasks.runLaterSync(10, () -> {
            packetHandler.removeFromTab(player, this);
        });
    }

    public void setSwimming(boolean swimming) {
        viewers.forEach(player -> packetHandler.setSwimming(player, this, swimming));
    }

    public void teleport(Location location) {
        viewers.forEach(player -> packetHandler.teleport(player, this, location));
    }

    public void destroy(Player player) {
        if (!viewers.contains(player)) return;
        viewers.remove(player);
        packetHandler.destroyNpc(player, this);
    }

}