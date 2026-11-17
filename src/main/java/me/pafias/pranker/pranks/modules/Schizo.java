package me.pafias.pranker.pranks.modules;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.pranker.utils.FakeNpc;
import me.pafias.putils.CC;
import me.pafias.putils.Tasks;
import me.pafias.putils.builders.PlayerProfileBuilder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class Schizo extends Prank {

    private final Random random = new Random();

    public Schizo(Victim victim) {
        super(Pranks.SCHIZO, victim);
        Tasks.runAsync(() -> {
            plugin.getLogger().info("Asynchronously loading Schizo profiles...");
            for (String uuid : Arrays.asList( // Random people from namemc trending
                    "eb0f2383-7f55-4b92-850c-a288d6a9f41a",
                    "7601ed6f-f96d-4d1c-aa8b-f08fdab2a1d0",
                    "5d28cc91-dfda-46d3-8802-24147e2b70c4",
                    "3ecec3fa-7a02-4a4c-9531-7448cff0a6cc",
                    "495a0818-d6e2-42aa-b846-375c25e5ddf1",
                    "77e954fd-04dc-4952-94e6-88fdd5655ece",
                    "49cc444d-aff6-491e-8a86-91653b9f0b57")
            ) {
                PlayerProfile profile = new PlayerProfileBuilder()
                        .setUuid(UUID.fromString(uuid))
                        .setName(uuid.split("-")[0]) // fallback name
                        .setFetchProperties(true)
                        .build();
                profiles.add(profile);
            }
            plugin.getLogger().info("Finished loading " + profiles.size() + " Schizo profiles.");
        });
    }

    private final List<PlayerProfile> profiles = new ArrayList<>();
    private FakeNpc npc;
    private long lastSpawnTime;

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        final Player target = event.getPlayer();
        if (!isTarget(target)) return;

        final long now = System.currentTimeMillis();
        if (now - lastSpawnTime < random.nextLong(4000, 8000))
            return;
        lastSpawnTime = now;

        final float fromYaw = event.getFrom().getYaw();
        final float toYaw = event.getTo().getYaw();

        // detect actual rotation
        final float diff = Math.abs(toYaw - fromYaw);
        if (diff < 8.0F) return; // ignore small head movements

        final boolean lookingRight = toYaw > fromYaw;

        // If one already exists, clean it up first
        if (npc != null) {
            npc.destroy(target);
            npc = null;
        }

        // Pick random skin
        Collections.shuffle(profiles);
        final PlayerProfile profile = profiles.get(0);

        // Compute spawn location at the edge of player's vision
        final Location loc = target.getLocation().clone();

        // Offset slightly to the left or right (~60 deg from view)
        final float angleOffset = lookingRight ? 60F : -60F;
        final double radians = Math.toRadians(loc.getYaw() + angleOffset);

        final RayTraceResult trace = target.rayTraceBlocks(32);
        final double distance = trace == null ? 5 + random.nextDouble() * 1.5 : trace.getHitPosition().distance(target.getLocation().toVector()) - 1;
        final double dx = -Math.sin(radians) * distance;
        final double dz = Math.cos(radians) * distance;
        loc.add(dx, random.nextDouble() * 0.5, dz);

        double x = loc.getX() + dx;
        double z = loc.getZ() + dz;
        World world = loc.getWorld();

        // get highest solid block Y
        int highestY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        double y = highestY + 0.5; // spawn slightly above ground so NPC doesn't clip
        loc.set(x, y, z);

        Vector dir = target.getEyeLocation().toVector().subtract(loc.toVector());
        double dx2 = dir.getX(), dy2 = dir.getY(), dz2 = dir.getZ();

        float yaw = (float) Math.toDegrees(Math.atan2(-dx2, dz2));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy2, Math.sqrt(dx2 * dx2 + dz2 * dz2)));

        loc.setYaw(yaw);
        loc.setPitch(pitch);

        npc = new FakeNpc(
                plugin.getPacketHandler(),
                random.nextInt(100000),
                profile,
                CC.a("&k" + profile.getName()),
                loc
        );

        npc.spawn(target);

        // Despawn after 10 ticks
        Tasks.runLaterSync(10, () -> {
            if (npc != null) {
                npc.destroy(target);
                npc = null;
            }
        });
    }

    @EventHandler
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target) || !isTarget(target)) return;
        if (random.nextDouble() < 0.3) // 30% chance of "ghost hit"
            Tasks.runLaterSync(random.nextLong(10, 40), () -> sendGhostHit(target));
    }

    private void sendGhostHit(Player player) {
        Entity source = player.getWorld().spawn(player.getLocation(), Bat.class, e -> {
            e.setInvisible(true);
            e.setSilent(true);
        });
        player.damage(1, source);
        source.remove();
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {
        if (npc != null) {
            npc.destroy(getPlayer());
            npc = null;
        }
    }

}
