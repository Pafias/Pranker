package me.pafias.pranker.pranks.modules;

import com.mojang.authlib.GameProfile;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpsideDown extends Prank {

    public UpsideDown() {
        super("upsidedown", "Upside Down", "Turns the player upside down (Unstable, may not work)");
    }

    private final Map<UUID, GameProfile> originalProfiles = new HashMap<>();

    @Override
    public void playerAdded(Victim player) {
        try {
            Class p = player.getPlayer().getClass();
            GameProfile op = (GameProfile) p.getDeclaredMethod("getProfile").invoke(player.getPlayer());
            originalProfiles.put(player.getPlayer().getUniqueId(), op);
            GameProfile dinnerbone = new GameProfile(op.getId(), "Dinnerbone");
            setProfile(player.getPlayer(), dinnerbone);
            // TODO nametag
            hideAndShow(player.getPlayer());
            new BukkitRunnable() {
                @Override
                public void run() {
                    setProfile(player.getPlayer(), op);
                }
            }.runTaskLater(plugin, 5);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void playerRemoved(Victim player) {
        setProfile(player.getPlayer(), originalProfiles.get(player.getPlayer().getUniqueId()));
        hideAndShow(player.getPlayer());
    }

    private void setProfile(Player player, GameProfile profile) {
        try {
            Object el = player.getClass().getMethod("getHandle").invoke(player);
            Class eh = el.getClass().getSuperclass();
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
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getOnlinePlayers().forEach(p -> p.hidePlayer(player));
            }
        }.runTask(plugin);
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getOnlinePlayers().forEach(p -> p.showPlayer(player));
            }
        }.runTaskLater(plugin, 2);
    }


}
