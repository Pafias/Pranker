package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import me.pafias.putils.builders.GameProfileBuilder;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class UpsideDown extends Prank {

    public UpsideDown(Victim victim) {
        super(Pranks.UPSIDE_DOWN, victim);
    }

    private Object originalProfile;

    @Override
    public void onStart() throws Exception {
        Class<?> p = getPlayer().getClass();
        Object op = p.getDeclaredMethod("getProfile").invoke(getPlayer());
        originalProfile = op;
        Object dinnerbone = GameProfileBuilder.fromHandle(op).setName("Dinnerbone").build();
        setProfile(getPlayer(), dinnerbone);
        hideAndShow(getPlayer());
        Tasks.runLaterSync(5, () -> setProfile(getPlayer(), op));
    }

    @Override
    public void onStop() {
        if (originalProfile != null) {
            setProfile(getPlayer(), originalProfile);
            hideAndShow(getPlayer());
            originalProfile = null;
        }
    }

    private void setProfile(Player player, Object profile) {
        try {
            Object el = player.getClass().getMethod("getHandle").invoke(player);
            Class<?> eh = el.getClass().getSuperclass();
            Field[] fields = eh.getDeclaredFields();
            Field field = null;
            for (Field f : fields)
                if (f.getType().getSimpleName().equals("GameProfile")) {
                    field = f;
                    break;
                }
            if (field == null)
                throw new NullPointerException("GameProfile field not found.");
            field.setAccessible(true);
            field.set(el, profile);
            field.setAccessible(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void hideAndShow(Player player) {
        Tasks.runSync(() -> {
            plugin.getServer().getOnlinePlayers().forEach(p -> p.hidePlayer(player));
        });
        Tasks.runLaterSync(2, () -> {
            plugin.getServer().getOnlinePlayers().forEach(p -> p.showPlayer(player));
        });
    }


}
