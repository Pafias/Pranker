package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.putils.Tasks;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Swim extends Prank {

    public Swim() {
        super("swim", "Swim", "Makes everyone else appear to be swimming");
    }

    private final Map<Victim, BukkitTask> tasks = new HashMap<>();

    @Override
    public void playerAdded(Victim player) {
        tasks.put(player, Tasks.runRepeatingAsync(2, 10, () -> {
            player.getPlayer().getWorld().getPlayers().forEach(p -> {
                if (p == player.getPlayer()) return;
                PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), new WrapperPlayServerEntityMetadata(
                        p.getEntityId(),
                        Collections.singletonList(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, EntityPose.SWIMMING))
                ));
            });
        }));
    }

    @Override
    public void playerRemoved(Victim player) {
        BukkitTask task = tasks.remove(player);
        if (task != null)
            task.cancel();
    }

}
