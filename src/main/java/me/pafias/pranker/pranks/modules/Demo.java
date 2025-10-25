package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;

public class Demo extends Prank {

    public Demo() {
        super("demo", "Demo", "Triggers the minecraft demo screen");
    }

    @Override
    public void playerAdded(Victim player) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), new WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.DEMO_EVENT,
                0
        ));
        try {
            removePlayer(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playerRemoved(Victim player) {
    }

}
