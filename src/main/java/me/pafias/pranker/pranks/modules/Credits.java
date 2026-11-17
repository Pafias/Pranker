package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;

public class Credits extends Prank {

    public Credits(Victim victim) {
        super(Pranks.CREDITS, victim);
    }

    @Override
    public void onStart() {
        PacketEvents.getAPI().getPlayerManager().sendPacket(getPlayer(), new WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.WIN_GAME,
                1
        ));
        try {
            plugin.getPrankManager().removePlayer(getVictim(), getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
    }

}
