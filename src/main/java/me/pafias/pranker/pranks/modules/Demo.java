package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;

public class Demo extends Prank {

    public Demo(Victim victim) {
        super(Pranks.DEMO, victim);
    }

    @Override
    public void onStart() {
        /*
        Wiki states:
        Demo event	0: Show welcome to demo screen.
                    101: Tell movement controls.
                    102: Tell jump control.
                    103: Tell inventory control.
                    104: Tell that the demo is over and print a message about how to take a screenshot.
         */
        // Only 0 is sent, which actually opens up a demo screen/gui. The other ones are just chat messages, not that exciting...
        PacketEvents.getAPI().getPlayerManager().sendPacket(getPlayer(), new WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.DEMO_EVENT,
                0
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
