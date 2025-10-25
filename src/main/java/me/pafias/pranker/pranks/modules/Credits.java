package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;

public class Credits extends Prank {

    public Credits() {
        super("credits", "Credits", "Roll credits");
    }

    @Override
    public void playerAdded(Victim player) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), new WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.WIN_GAME,
                1
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
