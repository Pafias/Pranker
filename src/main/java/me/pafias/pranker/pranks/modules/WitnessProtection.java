package me.pafias.pranker.pranks.modules;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.utils.FakeNpc;
import me.pafias.putils.CC;
import me.pafias.putils.Tasks;
import me.pafias.putils.builders.PlayerProfileBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WitnessProtection extends Prank {

    public WitnessProtection() {
        super("witnessprotection", "Witness Protection", "Surrounds the player with FBI agents");

        updateTask = Tasks.runRepeatingAsync(20, 1, () -> {
            for (Map.Entry<Victim, List<FakeNpc>> entry : npcs.entrySet()) {
                Victim victim = entry.getKey();
                List<FakeNpc> agents = entry.getValue();

                if (victim.getPlayer() == null || !victim.getPlayer().isOnline())
                    continue;

                Location center = victim.getPlayer().getLocation().clone();
                double angleBase = angles.getOrDefault(victim, 0.0);
                double radius = 2.5;

                for (int i = 0; i < agents.size(); i++) {
                    double offset = (2 * Math.PI / agents.size()) * i;
                    double x = center.getX() + radius * Math.cos(angleBase + offset);
                    double z = center.getZ() + radius * Math.sin(angleBase + offset);
                    float yaw = (float) Math.toDegrees(angleBase + offset + Math.PI) + 90;
                    Location target = new Location(center.getWorld(), x, center.getY(), z, yaw, 0);
                    agents.get(i).teleport(target);
                }

                angles.put(victim, angleBase + Math.toRadians(5));
            }
        });
    }

    private final Map<Victim, List<FakeNpc>> npcs = new HashMap<>();
    private final Map<Victim, Double> angles = new HashMap<>();
    private final BukkitTask updateTask;

    @Override
    public void playerAdded(Victim player) {
        final int npcCount = 6;
        final int[] entityIds = new int[6];
        for (int i = 0; i < entityIds.length; i++) {
            entityIds[i] = newId();
        }
        new PlayerProfileBuilder()
                .randomUuid()
                .setName("FBI")
                .setSkinUrl("https://s.namemc.com/i/7c289a27810469ca.png")
                .buildAsync()
                .thenAccept(p -> {
                    List<FakeNpc> list = new ArrayList<>();

                    double radius = 2.5;
                    Location center = player.getPlayer().getLocation();

                    for (int i = 0; i < npcCount; i++) {
                        double angle = (2 * Math.PI / npcCount) * i;
                        double x = center.getX() + radius * Math.cos(angle);
                        double z = center.getZ() + radius * Math.sin(angle);
                        Location spawnLoc = new Location(center.getWorld(), x, center.getY(), z);

                        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "FBI");
                        profile.setProperties(p.getProperties());
                        FakeNpc agent = new FakeNpc(plugin.getPacketHandler(), entityIds[i], profile, CC.EMPTY, spawnLoc);
                        list.add(agent);
                        agent.spawnForAll();
                    }

                    npcs.put(player, list);
                    angles.put(player, 0.0);
                });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (Map.Entry<Victim, List<FakeNpc>> entry : npcs.entrySet()) {
            for (FakeNpc agent : entry.getValue()) {
                agent.spawn(event.getPlayer());
            }
        }
    }

    private static final double BOUNCE_RADIUS_SQUARED = Math.pow(3, 2);
    private static final double BOUNCE_FORCE = 1.0;

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        final Player mover = event.getPlayer();

        for (Victim victim : npcs.keySet()) {
            final Player protectedPlayer = victim.getPlayer();
            if (protectedPlayer == null || !protectedPlayer.isOnline() || protectedPlayer.equals(mover)) continue;

            if (mover.getLocation().distanceSquared(protectedPlayer.getLocation()) <= BOUNCE_RADIUS_SQUARED) {
                mover.setVelocity(mover.getLocation().toVector()
                        .subtract(protectedPlayer.getLocation().toVector())
                        .normalize()
                        .multiply(BOUNCE_FORCE)
                        .setY(0.3));
            }
        }
    }

    @EventHandler
    public void onToggleSwim(EntityToggleSwimEvent event) {
        if (event.getEntity() instanceof Player player) {
            final Victim victim = plugin.getPlayerManager().getPlayer(player);
            if (victim == null || !npcs.containsKey(victim)) return;
            final List<FakeNpc> list = npcs.get(plugin.getPlayerManager().getPlayer(player));
            for (FakeNpc agent : list) {
                agent.setSwimming(event.isSwimming());
            }
        }
    }

    @Override
    public void playerRemoved(Victim player) {
        List<FakeNpc> list = npcs.remove(player);
        angles.remove(player);

        if (list != null)
            Bukkit.getOnlinePlayers().forEach(p -> plugin.getPacketHandler().destroyNpc(p, list.toArray(new FakeNpc[0])));

        if (npcs.isEmpty())
            allRemoved();
    }

    public void allRemoved() {
        updateTask.cancel();
        npcs.values().forEach(list -> {
            if (list != null)
                Bukkit.getOnlinePlayers().forEach(p -> plugin.getPacketHandler().destroyNpc(p, list.toArray(new FakeNpc[0])));
        });
        npcs.clear();
        angles.clear();
    }

    public static int newId() {
        final int id = ThreadLocalRandom.current().nextInt();
        if (Bukkit.getWorlds().stream().anyMatch(w -> w.getEntities().stream().anyMatch(e -> e.getEntityId() == id)))
            return newId();
        return id;
    }

}
