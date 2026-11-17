package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;

public class Swim extends Prank {

    public Swim(Victim victim) {
        super(Pranks.SWIM, victim);
    }

    private BukkitTask task;

    @Override
    public void onStart() {
        task = Tasks.runRepeatingAsync(2, 10, () -> {
            if (getVictim() == null || getPlayer() == null) {
                return;
            }
            getPlayer().getWorld().getPlayers().forEach(p -> {
                if (p.equals(getPlayer())) return;
                PacketEvents.getAPI().getPlayerManager().sendPacket(getPlayer(), new WrapperPlayServerEntityMetadata(
                        p.getEntityId(),
                        Collections.singletonList(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, EntityPose.SWIMMING))
                ));
            });
        });
    }

    @Override
    public void onStop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

}
