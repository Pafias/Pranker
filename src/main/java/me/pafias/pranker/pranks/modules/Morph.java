package me.pafias.pranker.pranks.modules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import me.pafias.putils.Tasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Everyone else sees the player as another entity. The player's spawn packet gets its entity type swapped, and since
 * the entity id stays the same, all the movement/rotation/animation packets for the player drive the fake mob.
 * <p>
 * The client always renders itself as a player, so the player gets a second, packet-only copy of the mob that only
 * they can see, glued to their position every tick, while their own client is told they're invisible. Big mobs get
 * shrunk for them so they don't swallow the first person camera. Their clicks on that copy (it's right on top of them, so they hit it a lot) get redirected to whatever is actually behind it.
 * <p>
 * Extra args customize the mob (cat type, sheep color, baby...). They're applied through the Bukkit API to an
 * unspawned entity of that type, whose mob-specific data then gets sent along with the disguise, so this works for any
 * mob without hardcoding metadata indices per version. Each extra is either a bare value ({@code black},
 * {@code baby}, {@code 3}) or {@code option=value} for setters a bare value would be ambiguous for
 * ({@code collarcolor=red}).
 */
public class Morph extends Prank {

    // Entity (0-7) and LivingEntity (8-14) data is shared by every mob; anything after that is player-only
    // (skin parts, main hand, score...) and would break the client if applied to another entity
    private static final int LAST_SHARED_INDEX = 14;
    private static final int SHARED_FLAGS_INDEX = 0;
    private static final byte INVISIBLE_FLAG = 0x20;
    // Vanilla default, the attribute for it only exists on newer versions
    private static final double ATTACK_REACH = 3;
    // The client smooths every teleport of the copy over ~3 ticks, so it gets sent that far ahead to land on the player
    private static final double LEAD_TICKS = 3;
    // Anything faster than this per tick is a teleport, not movement worth leading
    private static final double MAX_LEAD_SPEED = 4;
    // Ticks without movement before the lead is taken back, packets don't line up exactly with ticks
    private static final int STOP_TICKS = 2;
    // Bigger copies get shrunk down to this, so they stay under the player's eyes (1.27 when sneaking) and out of view
    private static final double MAX_SELF_HEIGHT = 1.2;

    public Morph(Victim victim, String... args) {
        super(Pranks.MORPH, victim, args);
        final EntityType bukkitType = hasArgs() ? parseType(args[0]) : EntityType.CAT;
        type = EntityTypes.getByName(bukkitType.getKey().toString());
        if (type == null)
            throw new IllegalArgumentException("Entity not supported on this version: " + bukkitType.name().toLowerCase());
        extraData = buildData(bukkitType, args == null || args.length < 2 ? new String[0] : Arrays.copyOfRange(args, 1, args.length));
    }

    private final com.github.retrooper.packetevents.protocol.entity.type.EntityType type;
    // Mob-specific data (variant, color, baby...) sent right after the spawn
    private final List<EntityData<?>> extraData;
    // Scale for the copy the player sees, worked out from the mob's height
    private double selfScale = 1;
    private PacketListenerCommon listener;
    // The copy only the player sees
    private final int selfId = SpigotReflectionUtil.generateEntityId();
    private final UUID selfUuid = UUID.randomUUID();
    private final String selfTeam = "pranker_" + selfId;
    private BukkitTask selfTask;
    // Where the player really is, straight from their movement packets (the server only catches up next tick)
    private Vector3d selfPosition;
    private float selfYaw;
    private float selfPitch;
    private boolean selfOnGround;
    // Whether the copy was last sent ahead of the player
    private boolean selfLeading;
    private int selfIdleTicks;

    @Override
    public void onStart() throws Exception {
        final UUID uuid = getPlayer().getUniqueId();
        // Respawning keeps the same entity id
        final int entityId = getPlayer().getEntityId();

        listener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getUser() == null) return;
                if (uuid.equals(event.getUser().getUUID())) {
                    onSelfPacket(event, entityId);
                    return;
                }

                // 1.20.2+: players spawn through the regular spawn packet, so only the type needs changing
                if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                    final WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
                    if (packet.getEntityId() != entityId || packet.getEntityType() != EntityTypes.PLAYER) return;
                    packet.setEntityType(type);
                    event.markForReEncode(true);
                    if (!extraData.isEmpty())
                        event.getTasksAfterSend().add(() -> event.getUser().sendPacketSilently(new WrapperPlayServerEntityMetadata(entityId, extraData)));
                    return;
                }

                // Older versions have a dedicated player spawn packet with no type field, so swap it for a mob one
                if (event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
                    final WrapperPlayServerSpawnPlayer packet = new WrapperPlayServerSpawnPlayer(event);
                    if (packet.getEntityId() != entityId) return;
                    event.setCancelled(true);
                    event.getUser().sendPacketSilently(spawnPacket(entityId, packet.getUUID(), packet.getPosition(), packet.getYaw(), packet.getPitch()));
                    if (!extraData.isEmpty())
                        event.getUser().sendPacketSilently(new WrapperPlayServerEntityMetadata(entityId, extraData));
                    return;
                }

                if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                    final WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
                    if (packet.getEntityId() != entityId) return;
                    final List<EntityData<?>> shared = new ArrayList<>();
                    for (EntityData<?> data : packet.getEntityMetadata())
                        if (data.getIndex() <= LAST_SHARED_INDEX)
                            shared.add(data);
                    if (shared.isEmpty()) {
                        event.setCancelled(true);
                        return;
                    }
                    if (shared.size() == packet.getEntityMetadata().size()) return;
                    packet.setEntityMetadata(shared);
                    event.markForReEncode(true);
                }
            }

            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getUser() == null || !uuid.equals(event.getUser().getUUID())) return;
                // Moving the copy right as the client reports moving, instead of waiting for the next tick
                if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                    moveSelf(new WrapperPlayClientPlayerFlying(event));
                    return;
                }
                final boolean attack;
                if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                    final WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
                    if (packet.getEntityId() != selfId) return;
                    attack = packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
                } else if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
                    if (new WrapperPlayClientAttack(event).getEntityId() != selfId) return;
                    attack = true;
                } else return;
                event.setCancelled(true);
                if (attack) Tasks.runSync(Morph.this::redirectAttack);
            }
        }, PacketListenerPriority.NORMAL);

        respawn();
        showSelf();
        selfTask = Tasks.runRepeatingSync(1, 1, this::followSelf);
    }

    @Override
    public void onStop() throws Exception {
        if (selfTask != null) {
            selfTask.cancel();
            selfTask = null;
        }
        if (listener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            listener = null;
        }
        respawn();
        hideSelf();
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (isTarget(event.getPlayer())) reshowSelf();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (isTarget(event.getPlayer())) reshowSelf();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        // Same world teleports don't respawn anything, the copy just has to jump there
        if (isTarget(event.getPlayer()))
            Tasks.runLaterSync(1, this::resetSelfPosition);
    }

    /**
     * The player's own entity data: the invisible flag gets forced on, and the copy gets the real data so it still
     * sneaks, burns, etc. along with them
     */
    private void onSelfPacket(PacketSendEvent event, int entityId) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;
        final WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        if (packet.getEntityId() != entityId) return;
        final List<EntityData<?>> shared = new ArrayList<>();
        boolean changed = false;
        for (EntityData<?> data : packet.getEntityMetadata()) {
            if (data.getIndex() > LAST_SHARED_INDEX) continue;
            shared.add(copy(data));
            if (data.getIndex() == SHARED_FLAGS_INDEX) {
                ((EntityData<Byte>) data).setValue((byte) ((Byte) data.getValue() | INVISIBLE_FLAG));
                changed = true;
            }
        }
        if (changed) event.markForReEncode(true);
        if (!shared.isEmpty())
            event.getTasksAfterSend().add(() -> event.getUser().sendPacketSilently(new WrapperPlayServerEntityMetadata(selfId, shared)));
    }

    private void showSelf() {
        final Player victim = getPlayer();
        if (victim == null || !victim.isOnline()) return;
        final org.bukkit.Location location = victim.getLocation();
        // Without this the copy keeps shoving the player around, as it lags a bit behind them
        send(victim, new WrapperPlayServerTeams(selfTeam, WrapperPlayServerTeams.TeamMode.CREATE,
                new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                        Component.empty(), Component.empty(), Component.empty(),
                        WrapperPlayServerTeams.NameTagVisibility.NEVER, WrapperPlayServerTeams.CollisionRule.NEVER,
                        NamedTextColor.WHITE, WrapperPlayServerTeams.OptionData.NONE
                ), selfUuid.toString()));
        send(victim, spawnPacket(selfId, selfUuid, SpigotConversionUtil.fromBukkitLocation(location).getPosition(),
                location.getYaw(), location.getPitch()));
        final List<EntityData<?>> data = new ArrayList<>(sharedData(victim));
        data.addAll(extraData);
        if (!data.isEmpty())
            send(victim, new WrapperPlayServerEntityMetadata(selfId, data));
        // The scale attribute only exists since 1.20.5, older versions get the full size copy
        if (selfScale < 1 && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_5))
            send(victim, new WrapperPlayServerUpdateAttributes(selfId, Collections.singletonList(
                    new WrapperPlayServerUpdateAttributes.Property(Attributes.SCALE, selfScale, Collections.emptyList()))));
        send(victim, flagsPacket(victim, (byte) (sharedFlags(victim) | INVISIBLE_FLAG)));
        resetSelfPosition();
    }

    private void hideSelf() {
        final Player victim = getPlayer();
        if (victim == null || !victim.isOnline()) return;
        send(victim, new WrapperPlayServerDestroyEntities(selfId));
        send(victim, new WrapperPlayServerTeams(selfTeam, WrapperPlayServerTeams.TeamMode.REMOVE,
                Optional.empty()));
        send(victim, flagsPacket(victim, sharedFlags(victim)));
    }

    /**
     * The client drops every entity (and team members stay, but the copy is gone) on respawn/world change
     */
    private void reshowSelf() {
        Tasks.runLaterSync(2, () -> {
            if (listener == null) return;
            hideSelf();
            showSelf();
        });
    }

    /**
     * Called from the netty thread for each of the player's movement packets
     */
    private synchronized void moveSelf(WrapperPlayClientPlayerFlying packet) {
        if (selfPosition == null || !(packet.hasPositionChanged() || packet.hasRotationChanged())) return;
        Vector3d lead = Vector3d.zero();
        if (packet.hasPositionChanged()) {
            final Vector3d position = packet.getLocation().getPosition();
            final Vector3d step = new Vector3d(position.getX() - selfPosition.getX(), 0, position.getZ() - selfPosition.getZ());
            if (step.length() <= MAX_LEAD_SPEED)
                lead = step.multiply(LEAD_TICKS);
            selfPosition = position;
            selfIdleTicks = 0;
        }
        if (packet.hasRotationChanged()) {
            selfYaw = packet.getLocation().getYaw();
            selfPitch = packet.getLocation().getPitch();
        }
        selfOnGround = packet.isOnGround();
        selfLeading = !lead.equals(Vector3d.zero());
        sendSelfPosition(selfPosition.add(lead));
    }

    /**
     * Once the player stops, the copy is still sitting where it was sent ahead to, so bring it back
     */
    private synchronized void followSelf() {
        if (!selfLeading || ++selfIdleTicks < STOP_TICKS) return;
        selfLeading = false;
        sendSelfPosition(selfPosition);
    }

    private synchronized void resetSelfPosition() {
        final Player victim = getPlayer();
        if (victim == null || !victim.isOnline()) return;
        final org.bukkit.Location location = victim.getLocation();
        selfPosition = SpigotConversionUtil.fromBukkitLocation(location).getPosition();
        selfYaw = location.getYaw();
        selfPitch = location.getPitch();
        selfOnGround = victim.isOnGround();
        selfLeading = false;
        sendSelfPosition(selfPosition);
    }

    private void sendSelfPosition(Vector3d position) {
        final Player victim = getPlayer();
        if (victim == null || !victim.isOnline()) return;
        send(victim, new WrapperPlayServerEntityTeleport(selfId, position, selfYaw, selfPitch, selfOnGround));
        send(victim, new WrapperPlayServerEntityHeadLook(selfId, selfYaw));
    }

    /**
     * The copy surrounds the player's eyes for most mobs, so their client aims at it instead of what they're looking
     * at. Find what that was and hit it instead
     */
    private void redirectAttack() {
        final Player victim = getPlayer();
        if (victim == null || !victim.isOnline() || listener == null) return;
        final RayTraceResult result = victim.getWorld().rayTrace(victim.getEyeLocation(),
                victim.getEyeLocation().getDirection(), ATTACK_REACH, FluidCollisionMode.NEVER, true, 0,
                entity -> !entity.equals(victim));
        if (result != null && result.getHitEntity() != null)
            victim.attack(result.getHitEntity());
    }

    /**
     * Hiding and showing the player makes the server send everyone a fresh spawn packet, which goes through the
     * listener (or doesn't, once it's unregistered)
     */
    private void respawn() {
        final Player victim = getPlayer();
        if (victim == null || !victim.isOnline()) return;
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            if (viewer.equals(victim) || !viewer.canSee(victim)) continue;
            if (plugin.isEnabled()) {
                viewer.hidePlayer(plugin, victim);
                viewer.showPlayer(plugin, victim);
            } else {
                // Pranks get stopped in onDisable, when the plugin variant refuses to run. The legacy one skips that
                // check, and is just as safe here since the hide is undone right away
                viewer.hidePlayer(victim);
                viewer.showPlayer(victim);
            }
        }
    }

    private PacketWrapper<?> spawnPacket(int entityId, UUID uuid, Vector3d position, float yaw, float pitch) {
        // Living entities got merged into the regular spawn packet in 1.19
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_19))
            return new WrapperPlayServerSpawnLivingEntity(
                    entityId, uuid, type, new Location(position, yaw, pitch),
                    yaw, Vector3d.zero(), Collections.emptyList()
            );
        return new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(uuid), type, position,
                pitch, yaw, yaw, 0, Optional.of(Vector3d.zero())
        );
    }

    private static void send(Player player, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, packet);
    }

    private static List<EntityData<?>> sharedData(Player player) {
        final List<EntityData<?>> data = new ArrayList<>();
        for (EntityData<?> value : SpigotConversionUtil.getEntityMetadata(player))
            if (value.getIndex() <= LAST_SHARED_INDEX)
                data.add(value);
        return data;
    }

    private static byte sharedFlags(Player player) {
        for (EntityData<?> value : SpigotConversionUtil.getEntityMetadata(player))
            if (value.getIndex() == SHARED_FLAGS_INDEX)
                return (Byte) value.getValue();
        return 0;
    }

    private WrapperPlayServerEntityMetadata flagsPacket(Player player, byte flags) {
        return new WrapperPlayServerEntityMetadata(player.getEntityId(),
                Collections.singletonList(new EntityData<>(SHARED_FLAGS_INDEX, EntityDataTypes.BYTE, flags)));
    }

    private static <T> EntityData<T> copy(EntityData<T> data) {
        return new EntityData<>(data.getIndex(), data.getType(), data.getValue());
    }

    private static EntityType parseType(String input) {
        final String name = input.toLowerCase(Locale.ROOT).replace("minecraft:", "");
        for (EntityType type : EntityType.values()) {
            if (type == EntityType.UNKNOWN || type == EntityType.PLAYER || !type.isAlive()) continue;
            if (type.name().equalsIgnoreCase(name) || type.getKey().getKey().equals(name))
                return type;
        }
        throw new IllegalArgumentException("Invalid entity: " + input);
    }

    /**
     * Creates (without spawning) an entity of the type, applies the extras to it and grabs its mob-specific data.
     * Creating it also rolls the natural random variant, like a real spawn would
     */
    private List<EntityData<?>> buildData(EntityType bukkitType, String[] extras) {
        final Entity dummy;
        try {
            dummy = getPlayer().getWorld().createEntity(getPlayer().getLocation(), bukkitType.getEntityClass());
        } catch (Exception ex) {
            if (extras.length > 0)
                throw new IllegalArgumentException("Can't customize " + bukkitType.name().toLowerCase());
            return Collections.emptyList();
        }
        for (String extra : extras)
            applyExtra(dummy, bukkitType, extra);
        if (dummy.getHeight() > MAX_SELF_HEIGHT)
            selfScale = MAX_SELF_HEIGHT / dummy.getHeight();
        final List<EntityData<?>> data = new ArrayList<>();
        for (EntityData<?> value : SpigotConversionUtil.getEntityMetadata(dummy))
            if (value.getIndex() > LAST_SHARED_INDEX)
                data.add(value);
        return data;
    }

    private static void applyExtra(Entity entity, EntityType bukkitType, String extra) {
        final int split = extra.indexOf('=');
        final String option = split < 0 ? null : extra.substring(0, split);
        final String value = split < 0 ? extra : extra.substring(split + 1);
        for (Method setter : setters(bukkitType)) {
            if (option != null && !setter.getName().equalsIgnoreCase("set" + option)) continue;
            final Object argument = convert(setter, value, option != null);
            if (argument == null) continue;
            try {
                setter.invoke(entity, argument);
                return;
            } catch (Exception ex) {
                throw new IllegalArgumentException("Failed to apply " + extra + ": " + ex.getMessage());
            }
        }
        throw new IllegalArgumentException("Invalid option for " + bukkitType.name().toLowerCase() + ": " + extra);
    }

    /**
     * Turns the value into the setter's argument, or null if it doesn't fit
     *
     * @param explicit whether the option was named, otherwise booleans only match their own name ({@code baby})
     */
    private static Object convert(Method setter, String value, boolean explicit) {
        final Class<?> param = setter.getParameterTypes()[0];
        if (param == boolean.class) {
            if (explicit)
                return value.equalsIgnoreCase("true") ? Boolean.TRUE : value.equalsIgnoreCase("false") ? Boolean.FALSE : null;
            return setter.getName().equalsIgnoreCase("set" + value) ? Boolean.TRUE : null;
        }
        if (param == int.class) {
            // Bare numbers only go to setSize (slimes, phantoms); anything else has to be named
            if (!explicit && !setter.getName().equals("setSize")) return null;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        final String name = value.toUpperCase(Locale.ROOT).replace('-', '_');
        for (Map.Entry<String, Object> constant : constants(param).entrySet())
            if (constant.getKey().equals(name))
                return constant.getValue();
        return null;
    }

    /**
     * Single-arg setters specific to the mob (not the ones every mob has like setAI), with variant/type setters first
     * so a bare value like "black" picks the cat type over the collar color
     */
    private static List<Method> setters(EntityType bukkitType) {
        final List<Method> setters = new ArrayList<>();
        for (Method method : bukkitType.getEntityClass().getMethods()) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1) continue;
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (method.getDeclaringClass().isAssignableFrom(Mob.class)) continue;
            final Class<?> param = method.getParameterTypes()[0];
            if (param != boolean.class && param != int.class && constants(param).isEmpty()) continue;
            setters.add(method);
        }
        setters.sort(Comparator.comparing((Method m) -> !(m.getName().endsWith("Type") || m.getName().endsWith("Variant")))
                .thenComparing(Method::getName));
        return setters;
    }

    /**
     * Enum constants, or the static constants of registry-backed types like Cat.Type on newer versions
     */
    private static Map<String, Object> constants(Class<?> type) {
        final Map<String, Object> constants = new LinkedHashMap<>();
        if (type.isEnum()) {
            for (Object constant : type.getEnumConstants())
                constants.put(((Enum<?>) constant).name(), constant);
            return constants;
        }
        if (type.isPrimitive() || type.getName().startsWith("java.")) return constants;
        for (Field field : type.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != type) continue;
            try {
                final Object constant = field.get(null);
                if (constant != null)
                    constants.put(field.getName(), constant);
            } catch (Exception ignored) {
            }
        }
        return constants;
    }

    /**
     * Tab completion for the extras: bare values and option names, or that option's values once there's an "="
     */
    public static List<String> options(String entity, String typed) {
        final EntityType bukkitType;
        try {
            bukkitType = parseType(entity);
        } catch (IllegalArgumentException ex) {
            return Collections.emptyList();
        }
        final boolean named = typed.contains("=");
        final Set<String> options = new LinkedHashSet<>();
        for (Method setter : setters(bukkitType)) {
            final String option = setter.getName().substring(3).toLowerCase(Locale.ROOT);
            final Class<?> param = setter.getParameterTypes()[0];
            if (param == boolean.class) {
                options.add(named ? option + "=true" : option);
            } else if (param == int.class) {
                if (!named) options.add(option + "=");
            } else {
                for (String constant : constants(param).keySet())
                    options.add(named ? option + "=" + constant.toLowerCase(Locale.ROOT) : constant.toLowerCase(Locale.ROOT));
                if (!named) options.add(option + "=");
            }
        }
        return options.stream().filter(o -> o.startsWith(typed.toLowerCase(Locale.ROOT))).toList();
    }

}
