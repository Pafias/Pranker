package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gravity gun. Holding right-click on someone grabs them, they follow the crosshair, scrolling pushes/pulls them and
 * left-click throws them.
 * <p>
 * The server can't see a button being held, but while right-click is held the client resends the click every 4 ticks,
 * so the grab lasts as long as those keep coming. With an empty hand the client sends nothing when clicking air, only
 * when there's an entity in the crosshair, so the player's entity interaction range is raised to the force's range
 * so the first click on someone far away gets through.
 * <p>
 * While holding, the player's client gets a big fake Interaction entity (an invisible hitbox) around their head. The
 * client always hits the entity its eye is inside of first, so every click lands on it no matter where they look or
 * how fast they move: right-clicks keep the hold going, a left-click throws. It also stops clicks from reaching the
 * blocks/entities behind it.
 */
public class Force extends Prank {

    private static final double RANGE = 32;
    private static final double AIM_ANGLE = Math.toRadians(8);
    // Right-click repeats every 4 ticks; leave room for a missed one before letting go
    private static final int HOLD_GRACE_TICKS = 8;
    private static final double MIN_DISTANCE = 2;
    private static final double SCROLL_STEP = 1;
    // How hard they get pulled to the crosshair each tick, and the speed cap so they don't overshoot wildly
    private static final double PULL = 0.5;
    private static final double MAX_SPEED = 3;
    private static final double THROW_SPEED = 3;
    // Vanilla survival entity reach (plus some slack); the raised range only counts for the force itself
    private static final double NORMAL_REACH = 3.5;
    // Big enough that the player's eye stays inside even with the client lagging the box's position a few ticks
    private static final float BOX_SIZE = 12;

    public Force(Victim victim) {
        super(Pranks.FORCE, victim);
    }

    private LivingEntity grabbed;
    private double distance;
    // Written from netty threads by the box's interact packets
    private volatile int lastUseTick;
    // Id of the fake hitbox while right-click is being held, -1 otherwise. Outlives the grab after a throw so that
    // still holding right-click doesn't instantly grab them back
    private volatile int boxId = -1;
    private PacketListenerCommon listener;
    private boolean hadAllowFlight;
    // Thrown/dropped entities don't take fall damage on their landing. Value is the tick they were let go
    private final Map<UUID, Integer> falling = new HashMap<>();
    private BukkitTask task;
    private final NamespacedKey reachKey = new NamespacedKey(plugin, "force_reach");

    @Override
    public void onStart() throws Exception {
        final AttributeInstance reach = getPlayer().getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (reach != null) {
            reach.removeModifier(reachKey);
            // Transient so it never gets saved to the player data
            reach.addTransientModifier(new AttributeModifier(reachKey, RANGE - reach.getBaseValue(), AttributeModifier.Operation.ADD_NUMBER));
        }
        final UUID uuid = getPlayer().getUniqueId();
        listener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
                if (event.getUser() == null || !uuid.equals(event.getUser().getUUID())) return;
                final WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
                if (boxId == -1 || packet.getEntityId() != boxId) return;
                // The server doesn't know this entity, keep it from ever seeing the packet
                event.setCancelled(true);
                if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK)
                    Tasks.runSync(Force.this::launch);
                else
                    lastUseTick = Bukkit.getCurrentTick();
            }
        }, PacketListenerPriority.NORMAL);
        task = Tasks.runRepeatingSync(1, 1, this::tick);
    }

    @Override
    public void onStop() throws Exception {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (listener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            listener = null;
        }
        release(null);
        removeBox();
        falling.clear();
        final AttributeInstance reach = getPlayer().getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (reach != null)
            reach.removeModifier(reachKey);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (!isTarget(event.getPlayer())) return;
        final Action action = event.getAction();
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && boxId == -1) {
            lastUseTick = Bukkit.getCurrentTick();
            grab(findTarget(event.getPlayer()));
            // The click that grabbed someone shouldn't also place a block or open something
            if (grabbed != null)
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!isTarget(event.getPlayer())) return;
        if (boxId == -1 && event.getRightClicked() instanceof LivingEntity entity && canGrab(event.getPlayer(), entity)) {
            lastUseTick = Bukkit.getCurrentTick();
            grab(entity);
        }
        // No trading/mounting/etc. with whatever is in the crosshair while holding, or with stuff out of normal reach
        if (grabbed != null || outOfReach(event.getPlayer(), event.getRightClicked()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !isTarget(player)) return;
        if (grabbed != null && event.getEntity().equals(grabbed)) {
            event.setCancelled(true);
            launch();
            return;
        }
        // The raised interaction range would otherwise let them punch people from across the map
        if (outOfReach(player, event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onScroll(PlayerItemHeldEvent event) {
        if (grabbed == null || !isTarget(event.getPlayer())) return;
        event.setCancelled(true);
        int delta = event.getNewSlot() - event.getPreviousSlot();
        // Wrapping around the hotbar (8 -> 0) is still a single scroll step
        if (delta > 4) delta -= 9;
        if (delta < -4) delta += 9;
        // Scrolling up lowers the slot, and pushes them away
        distance = Math.clamp(distance - delta * SCROLL_STEP, MIN_DISTANCE, RANGE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (event.getEntity().equals(grabbed) || falling.remove(event.getEntity().getUniqueId()) != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        // Flight is only allowed so the server doesn't kick them for floating; they don't get to actually fly
        if (event.getPlayer().equals(grabbed) && event.isFlying())
            event.setCancelled(true);
    }

    private void tick() {
        final int now = Bukkit.getCurrentTick();
        falling.entrySet().removeIf(entry -> {
            final Entity entity = Bukkit.getEntity(entry.getKey());
            return entity == null || (entity.isOnGround() && now - entry.getValue() > 5);
        });

        if (boxId == -1) return;
        final Player holder = getPlayer();
        if (holder == null || !holder.isOnline() || now - lastUseTick > HOLD_GRACE_TICKS) {
            release(null);
            removeBox();
            return;
        }
        plugin.getPacketHandler().teleportEntity(holder, boxId, boxLocation(holder));

        if (grabbed == null) return;
        if (!grabbed.isValid() || grabbed.isDead() || !grabbed.getWorld().equals(holder.getWorld())) {
            release(null);
            return;
        }

        final Location eye = holder.getEyeLocation();
        final Vector target = eye.toVector().add(eye.getDirection().multiply(distance));
        final Vector velocity = target.subtract(center(grabbed).toVector()).multiply(PULL);
        if (velocity.length() > MAX_SPEED)
            velocity.normalize().multiply(MAX_SPEED);
        grabbed.setVelocity(velocity);
        grabbed.setFallDistance(0);
        grabbed.getWorld().spawnParticle(Particle.PORTAL, center(grabbed), 6, 0.3, 0.5, 0.3, 0.5);
    }

    private void grab(LivingEntity entity) {
        if (entity == null) return;
        final Player holder = getPlayer();
        grabbed = entity;
        distance = Math.clamp(center(entity).distance(holder.getEyeLocation()), MIN_DISTANCE, RANGE);
        if (entity instanceof Player player) {
            hadAllowFlight = player.getAllowFlight();
            // Otherwise the server kicks them for flying after a few seconds of hovering
            player.setAllowFlight(true);
        }
        spawnBox(holder);
        holder.getWorld().playSound(holder.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.8F);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1F, 1F);
    }

    /**
     * Lets go, keeping their current momentum (or the given one) so they get tossed instead of just dropping
     */
    private void release(Vector velocity) {
        if (grabbed == null) return;
        final LivingEntity entity = grabbed;
        grabbed = null;
        if (entity instanceof Player player && player.isOnline() && !hadAllowFlight) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        if (velocity != null)
            entity.setVelocity(velocity);
        if (entity.isValid())
            falling.put(entity.getUniqueId(), Bukkit.getCurrentTick());
    }

    private void launch() {
        final Player holder = getPlayer();
        final LivingEntity entity = grabbed;
        if (entity == null || holder == null) return;
        release(holder.getEyeLocation().getDirection().multiply(THROW_SPEED).add(new Vector(0, 0.3, 0)));
        holder.getWorld().playSound(holder.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1F, 0.8F);
        entity.getWorld().spawnParticle(Particle.CLOUD, center(entity), 20, 0.3, 0.5, 0.3, 0.1);
    }

    private void spawnBox(Player holder) {
        if (boxId != -1) return;
        final int id = WitnessProtection.newId();
        final Location location = boxLocation(holder);
        PacketEvents.getAPI().getPlayerManager().sendPacket(holder, new WrapperPlayServerSpawnEntity(
                id, Optional.of(UUID.randomUUID()), EntityTypes.INTERACTION,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                0, 0, 0, 0, Optional.of(Vector3d.zero())
        ));
        PacketEvents.getAPI().getPlayerManager().sendPacket(holder, new WrapperPlayServerEntityMetadata(id, List.of(
                new EntityData<>(8, EntityDataTypes.FLOAT, BOX_SIZE), // Width
                new EntityData<>(9, EntityDataTypes.FLOAT, BOX_SIZE), // Height
                new EntityData<>(10, EntityDataTypes.BOOLEAN, false) // No arm swing on click
        )));
        boxId = id;
    }

    private void removeBox() {
        final int id = boxId;
        if (id == -1) return;
        boxId = -1;
        final Player holder = getPlayer();
        if (holder != null && holder.isOnline())
            PacketEvents.getAPI().getPlayerManager().sendPacket(holder, new WrapperPlayServerDestroyEntities(id));
    }

    /**
     * Interaction hitboxes grow up from their position, so start it half a box below the eye
     */
    private static Location boxLocation(Player holder) {
        return holder.getEyeLocation().subtract(0, BOX_SIZE / 2, 0);
    }

    /**
     * Whatever living thing is closest to the crosshair, so far away targets don't need pixel-perfect aim
     */
    private LivingEntity findTarget(Player holder) {
        final Location eye = holder.getEyeLocation();
        final Vector look = eye.getDirection();
        LivingEntity best = null;
        double bestAngle = AIM_ANGLE;
        for (LivingEntity entity : holder.getWorld().getNearbyLivingEntities(holder.getLocation(), RANGE)) {
            if (!canGrab(holder, entity)) continue;
            final Vector toTarget = center(entity).toVector().subtract(eye.toVector());
            if (toTarget.lengthSquared() > RANGE * RANGE) continue;
            final double angle = look.angle(toTarget);
            if (angle >= bestAngle) continue;
            if (!holder.hasLineOfSight(entity)) continue;
            best = entity;
            bestAngle = angle;
        }
        return best;
    }

    private static boolean canGrab(Player holder, LivingEntity entity) {
        if (entity.equals(holder)) return false;
        if (entity instanceof Player player && (player.getGameMode() == GameMode.SPECTATOR || !holder.canSee(player)))
            return false;
        return !entity.isDead();
    }

    private static boolean outOfReach(Player player, Entity entity) {
        return player.getEyeLocation().distanceSquared(center(entity)) > NORMAL_REACH * NORMAL_REACH
                && !entity.getBoundingBox().expand(NORMAL_REACH).contains(player.getEyeLocation().toVector());
    }

    private static Location center(Entity entity) {
        return entity.getLocation().add(0, entity.getHeight() / 2, 0);
    }

}
