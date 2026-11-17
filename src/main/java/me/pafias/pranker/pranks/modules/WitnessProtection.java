package me.pafias.pranker.pranks.modules;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.pranker.utils.FakeNpc;
import me.pafias.putils.CC;
import me.pafias.putils.Tasks;
import me.pafias.putils.builders.PlayerProfileBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class WitnessProtection extends Prank {

    public WitnessProtection(Victim victim, String... args) {
        super(Pranks.WITNESS_PROTECTION, victim, args);
        agentRadius = doubleArg(0, 3.5, 1, 20, "radius");
        // Bounce a bit before the circle so the agents appear to stop people
        bounceRadius = agentRadius + 1.0;
    }

    private final List<FakeNpc> agents = new ArrayList<>();
    private double angle;
    private BukkitTask updateTask;

    private final double agentRadius;
    private final double bounceRadius;
    private static final double BOUNCE_FORCE = 1.0;

    @Override
    public void onStart() {
        updateTask = Tasks.runRepeatingAsync(20, 1, () -> {
            if (getPlayer() == null || agents.isEmpty()) {
                return;
            }

            Player target = getPlayer();
            if (target == null || !target.isOnline()) {
                return;
            }

            Location center = target.getLocation().clone();

            for (int i = 0; i < agents.size(); i++) {
                double offset = (2 * Math.PI / agents.size()) * i;
                double x = center.getX() + agentRadius * Math.cos(angle + offset);
                double z = center.getZ() + agentRadius * Math.sin(angle + offset);
                float yaw = (float) Math.toDegrees(angle + offset + Math.PI) + 90;
                Location targetLocation = new Location(center.getWorld(), x, center.getY(), z, yaw, 0);
                agents.get(i).teleport(targetLocation);
            }

            angle += Math.toRadians(5);
        });

        final int npcCount = 6;
        final int[] entityIds = new int[npcCount];
        for (int i = 0; i < entityIds.length; i++) {
            entityIds[i] = newId();
        }
        new PlayerProfileBuilder()
                .randomUuid()
                .setName("FBI")
                .setSkinUrl("https://s.namemc.com/i/7c289a27810469ca.png")
                .buildAsync()
                .thenAccept(profile -> {
                    List<FakeNpc> spawned = new ArrayList<>();

                    Location center = getPlayer().getLocation();

                    for (int i = 0; i < npcCount; i++) {
                        double npcAngle = (2 * Math.PI / npcCount) * i;
                        double x = center.getX() + agentRadius * Math.cos(npcAngle);
                        double z = center.getZ() + agentRadius * Math.sin(npcAngle);
                        Location spawnLoc = new Location(center.getWorld(), x, center.getY(), z);

                        PlayerProfile fakeProfile = Bukkit.createProfile(UUID.randomUUID(), "FBI");
                        fakeProfile.setProperties(profile.getProperties());
                        FakeNpc agent = new FakeNpc(plugin.getPacketHandler(), entityIds[i], fakeProfile, CC.EMPTY, spawnLoc);
                        spawned.add(agent);
                        agent.spawnForAll();
                    }

                    agents.clear();
                    agents.addAll(spawned);
                    angle = 0.0;
                });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (FakeNpc agent : agents) {
            agent.spawn(event.getPlayer());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        final Player mover = event.getPlayer();
        final Player protectedPlayer = getPlayer();
        if (protectedPlayer == null || !protectedPlayer.isOnline() || protectedPlayer.equals(mover)) return;
        if (mover.getGameMode() == GameMode.SPECTATOR) return;
        if (!mover.getWorld().equals(protectedPlayer.getWorld())) return;
        if (mover.getLocation().distanceSquared(protectedPlayer.getLocation()) > bounceRadius * bounceRadius) return;

        Vector direction = mover.getLocation().toVector().subtract(protectedPlayer.getLocation().toVector()).setY(0);
        if (direction.lengthSquared() < 1.0E-4)
            direction = mover.getLocation().getDirection().multiply(-1).setY(0);
        if (direction.lengthSquared() < 1.0E-4)
            direction = new Vector(1, 0, 0);
        mover.setVelocity(direction.normalize().multiply(BOUNCE_FORCE).setY(0.3));
    }

    @EventHandler
    public void onToggleSwim(EntityToggleSwimEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isTarget(player)) return;
        for (FakeNpc agent : agents) {
            agent.setSwimming(event.isSwimming());
        }
    }

    @Override
    public void onStop() {
        if (updateTask != null)
            updateTask.cancel();
        if (!agents.isEmpty()) {
            Bukkit.getOnlinePlayers().forEach(p -> plugin.getPacketHandler().destroyNpc(p, agents.toArray(new FakeNpc[0])));
            agents.clear();
        }
        angle = 0.0;
    }

    public static int newId() {
        final int id = ThreadLocalRandom.current().nextInt();
        if (Bukkit.getWorlds().stream().anyMatch(w -> w.getEntities().stream().anyMatch(e -> e.getEntityId() == id)))
            return newId();
        return id;
    }

}
