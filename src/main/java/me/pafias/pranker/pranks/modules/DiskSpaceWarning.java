package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerLowDiskSpaceWarning;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;

public class DiskSpaceWarning extends Prank {

    public DiskSpaceWarning(Victim victim) {
        super(Pranks.DISK_SPACE_WARNING, victim);
    }

    @Override
    public void onStart() throws Exception {
        if (!PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_26_1))
            throw new UnsupportedOperationException("This prank is not supported on this server version. 26.1 and higher only.");
        PacketEvents.getAPI().getPlayerManager().sendPacket(getPlayer(), new WrapperPlayServerLowDiskSpaceWarning());
        try {
            plugin.getPrankManager().removePlayer(getVictim(), getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() throws Exception {

    }

}
