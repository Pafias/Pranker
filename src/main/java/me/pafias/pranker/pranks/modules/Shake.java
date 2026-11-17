package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;

/**
 * Makes the victim shake (like when fully frozen in powder snow), for everyone including themselves.
 * There is no shaking pose, the client shakes any entity whose frozen ticks reach the freeze threshold,
 * so we fake that value with packets. The server never actually freezes the victim, so there's no damage.
 */
public class Shake extends Prank {

    private static final int FROZEN_TICKS_INDEX = 7;
    // Entity#getTicksRequiredToFreeze
    private static final int FULLY_FROZEN_TICKS = 140;

    public Shake(Victim victim) {
        super(Pranks.SHAKE, victim);
    }

    private PacketListenerCommon listener;
    private BukkitTask task;

    @Override
    public void onStart() throws Exception {
        final int entityId = getPlayer().getEntityId();

        // Keep the server's own metadata updates (e.g. walking through powder snow, re-tracking) from undoing it
        listener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;
                if (event.getUser() == null) return;
                final WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
                if (packet.getEntityId() != entityId) return;
                boolean changed = false;
                for (EntityData<?> data : packet.getEntityMetadata()) {
                    if (data.getIndex() != FROZEN_TICKS_INDEX) continue;
                    ((EntityData<Integer>) data).setValue(FULLY_FROZEN_TICKS);
                    changed = true;
                }
                if (changed)
                    event.markForReEncode(true);
            }
        }, PacketListenerPriority.NORMAL);

        // Resend periodically so players who start tracking the victim later see it too
        task = Tasks.runRepeatingSync(0, 20, () -> {
            final Player victim = getPlayer();
            if (victim == null || !victim.isOnline()) return;
            for (Player player : victim.getWorld().getPlayers())
                sendFrozenTicks(player, FULLY_FROZEN_TICKS);
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
        if (victim != null && victim.isOnline())
            for (Player player : victim.getWorld().getPlayers())
                sendFrozenTicks(player, victim.getFreezeTicks());
    }

    private void sendFrozenTicks(Player receiver, int ticks) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(receiver, new WrapperPlayServerEntityMetadata(
                getPlayer().getEntityId(),
                Collections.singletonList(new EntityData<>(FROZEN_TICKS_INDEX, EntityDataTypes.INT, ticks))
        ));
    }

}
