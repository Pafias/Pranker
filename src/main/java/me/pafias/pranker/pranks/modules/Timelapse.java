package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.clock.ClockNetworkState;
import com.github.retrooper.packetevents.protocol.world.clock.WorldClocks;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTimeUpdate;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

/**
 * Wiki:
 * Update Time
 * <p>
 * Time is based on ticks, where 20 ticks happen every second. There are 24000 ticks in a day, making Minecraft days exactly 20 minutes long.
 * <p>
 * The time of day is based on the timestamp modulo 24000. 0 is sunrise, 6000 is noon, 12000 is sunset, and 18000 is midnight.
 * <p>
 * The default SMP server increments the time by 20 every second.
 * Clientbound set_time, Play ID: 115 (0x73)
 * Field Name 	Field Type 	Notes
 * World Age 	Long 	In ticks; not changed by server commands.
 * Clocks 	Clock ID 	Prefixed Array 	VarInt 	ID in the minecraft:world_clock registry.
 * Time 	VarLong 	Current time of the clock, in ticks.
 * Fractional time 	Float 	Fractional part of the time in ticks, normally a nonnegative number less than 1.
 * Rate 	Float 	Rate at which the client should automatically advance the time of the clock, in clock ticks per client tick.
 */
public class Timelapse extends Prank {

    // Clock ticks per client tick: a full day takes 24000 / 100 = 240 ticks (12 seconds)
    private static final float RATE = 100F;

    public Timelapse(Victim victim) {
        super(Pranks.TIMELAPSE, victim);
    }

    private PacketListenerCommon listener;
    private BukkitTask task;
    private long startTime;
    private long startMillis;

    @Override
    public void onStart() throws Exception {
        final UUID uuid = getPlayer().getUniqueId();
        startTime = getPlayer().getWorld().getFullTime();
        startMillis = System.currentTimeMillis();

        // Block the server's own time syncs, otherwise they reset the rate every second
        listener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.TIME_UPDATE
                        && event.getUser() != null && uuid.equals(event.getUser().getUUID()))
                    event.setCancelled(true);
            }
        }, PacketListenerPriority.NORMAL);

        // 26.1+ clients advance the clock by themselves at the given rate, so one packet is enough
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_26_1)) {
            sendTime(startTime, RATE);
            return;
        }

        // Older versions have no rate field, so keep pushing the time forward manually
        task = Tasks.runRepeatingAsync(0, 2, () -> {
            if (getPlayer() == null || !getPlayer().isOnline()) return;
            final long elapsedTicks = (System.currentTimeMillis() - startMillis) / 50;
            sendTime(startTime + (long) (elapsedTicks * RATE), RATE);
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
        // Snap back to the real time; the server's next sync corrects the rate if the daylight cycle is off
        if (getPlayer() != null && getPlayer().isOnline())
            sendTime(getPlayer().getWorld().getFullTime(), 1F);
    }

    private void sendTime(long time, float rate) {
        final World world = getPlayer().getWorld();
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(getPlayer(), new WrapperPlayServerTimeUpdate(
                world.getGameTime(),
                Map.of(WorldClocks.OVERWORLD, new ClockNetworkState(time, 0F, rate))
        ));
    }

}
