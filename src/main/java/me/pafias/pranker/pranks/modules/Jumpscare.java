package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;

public class Jumpscare extends Prank {

    public Jumpscare(Victim victim) {
        super(Pranks.JUMPSCARE, victim);
    }

    @Override
    public void onStart() throws Exception {
        // 0 = visual effect only
        // 1 = visual effect + sound effect
        PacketEvents.getAPI().getPlayerManager().sendPacket(getPlayer(), new WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.PLAY_ELDER_GUARDIAN_MOB_APPEARANCE,
                1
        ));
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
