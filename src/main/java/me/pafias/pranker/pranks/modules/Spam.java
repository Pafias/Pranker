package me.pafias.pranker.pranks.modules;

import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.putils.CC;
import me.pafias.putils.Tasks;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Spam extends Prank {

    public Spam(Victim victim) {
        super(Pranks.SPAM, victim);
        random = new Random();
    }

    private final Random random;
    private final Map<String, BukkitTask> tasks = new HashMap<>();
    private final Map<Integer, ActiveBossBar> bossBars = new HashMap<>();
    private String actionbarText;
    private int actionbarFrame;
    private long nextActionbarSwapAt;
    private long nextBossbarAttemptAt;
    private long nextChatAt;

    private static final List<String> POSSIBLE_TEXTS = new ArrayList<>(List.of(
            "CLAIM YOUR FREE NETHERITE ARMOR NOW!",
            "5 HOT SINGLE JENNYS IN YOUR AREA!",
            "LOCAL VILLAGER DOCTOR HATES THIS ONE TRICK!",
            "YOU WON 9999 DIAMONDS! CLICK NOW!",
            "THIS CREEPER PROBABLY WON'T EXPLODE... PROBABLY.",
            "SUBSCRIBE TO REMOVE ADS (NOT REALLY)",
            "YOUR INVENTORY HAS FEELINGS TOO",
            "TOP 10 WAYS TO PUNCH AIR FASTER",
            "BREAKING: SHEEP LEARNS REDSTONE, SHOCKS SCIENTISTS",
            "ENCHANT YOUR PICKAXE WITH TAX EVASION IV",
            "DOWNLOAD MORE RAM FOR YOUR SHULKER BOX",
            "SKELETONS HATE HIM: ONE WEIRD SHIELD TRICK",
            "CLICK HERE TO UNCAP YOUR CROUCH SPEED",
            "LIMITED TIME OFFER: BUY 1 DIRT, GET 64 MORE",
            "VILLAGER CRYPTO IS TOTALLY SAFE TRUST ME",
            "NEW PATCH: WATER NOW 30% WETTER",
            "YOU ARE THE 1,000,000TH PLAYER TO OPEN CHAT",
            "SERVER ADMINS FEAR THIS SIMPLE COMMAND",
            "GET RICH FAST WITH THIS 100% LEGIT FISH FARM",
            "MINECOIN DOUBLING SERVICE ONLINE NOW",
            "FREE XP IF YOU SPIN IN CIRCLES FOR 8 HOURS",
            "YOUR ARMOR EXPIRED. RENEW PREMIUM TODAY",
            "DISCOVER THE SECRET 11TH HEART",
            "SPEEDRUNNER REVEALS HOW TO WALK FASTER: WALK",
            "THIS AD IS SPONSORED BY BIG COBBLESTONE",
            "BREAKING NEWS: ZOMBIE APPLIES FOR OFFICE JOB",
            "PLACE 3 TORCHES TO UNLOCK NIGHT VISION PRO",
            "YOUR PET WOLF RATED THIS SERVER 5 BARKS",
            "ONE WEIRD TRICK TO MAKE SAND LESS SANDY",
            "ALCHEMISTS HATE THIS INFINITE POTION GLITCH",
            "EARN PASSIVE INCOME BY WATCHING FURNACES",
            "CONGRATS! YOU UNLOCKED INVISIBLE ACHIEVEMENT",
            "CLICK TO INSTALL HD CLOUDS IN REAL LIFE",
            "MOJANG DOESN'T WANT YOU TO KNOW ABOUT THIS",
            "TOP 3 REASONS YOUR CHEST IS JUDGING YOU",
            "NEW STUDY: CROUCHING IMPROVES WIFI SIGNAL",
            "BECOME A PRO BY RENAMING YOUR SWORD STEVE",
            "VILLAGER MBA COURSE STARTS THIS THURSDAY",
            "TURN 1 FEATHER INTO 64 EMERALDS TODAY",
            "THIS AXOLOTL PREDICTED YOUR NEXT MOVE",
            "WARNING: YOUR HOTBAR MAY CONTAIN HOT ITEMS",
            "SKELETONS NOW OFFER FREE DENTAL CHECKUPS",
            "POTION OF PRODUCTIVITY: JUST ADD COFFEE",
            "UPGRADE TO DIAMOND INTERNET SPEED",
            "PIGLIN BANK APPROVES LOANS IN 3 OINKS",
            "GET VERIFIED BY A CERTIFIED CHICKEN",
            "NEW META: STARE AT THE SUN FOR BETTER FPS",
            "ENDERMAN TEACHES TELEPORTATION MASTERCLASS",
            "OFFICIAL GUIDE: HOW TO NOT DIG STRAIGHT DOWN",
            "FREE TRIAL OF CREATIVE MODE (EMOTIONAL ONLY)",
            "EXPERT TIP: KEEP YOUR FEET AWAY FROM LAVA",
            "TNT CAN'T HURT YOU IF YOU BELIEVE HARD ENOUGH",
            "STREAMERS USE THIS SECRET TO LOOK BUSY",
            "GET 200% MORE LUCK BY WEARING GOLD BOOTS",
            "NEW CHALLENGE: TOUCH GRASS BLOCKS OUTSIDE",
            "FISH REVEALS 7 SECRETS TO BETTER SWIMMING",
            "GRANDMA'S RECIPE FOR ENCHANTED BAKED POTATO",
            "MYSTERY BOX CONTAINS EXACTLY MORE MYSTERY",
            "PRO TIP: SHIFT KEY REDUCES FALL DAMAGE FEAR",
            "WORLD BORDER CALLED. IT MISSES YOU",
            "YOU HAVE BEEN CHOSEN BY THE COUNCIL OF COWS"
    ));

    @Override
    public void onStart() {
        actionbarText = randomText();
        actionbarFrame = random.nextInt(1000);
        final long now = System.currentTimeMillis();
        nextActionbarSwapAt = now + random.nextLong(2500, 5000);
        nextBossbarAttemptAt = now + random.nextLong(1500, 3200);
        nextChatAt = now + random.nextLong(1000, 2200);

        // Spread attempts over time so the prank feels noisy but not bursty.
        tasks.put("bossbar_create", Tasks.runRepeatingSync(5, 5, () -> {
            if (!hasPlayer() || bossBars.size() >= 5)
                return;

            final long current = System.currentTimeMillis();
            if (current < nextBossbarAttemptAt)
                return;

            nextBossbarAttemptAt = current + random.nextLong(1700, 3400);
            final double spawnChance = Math.min(0.8D, 0.35D + (5 - bossBars.size()) * 0.08D);
            if (random.nextDouble() > spawnChance)
                return;

            createBossBar(current);
        }));

        tasks.put("actionbar", Tasks.runRepeatingSync(2, 2, () -> {
            if (!hasPlayer())
                return;

            final long current = System.currentTimeMillis();
            if (current >= nextActionbarSwapAt) {
                actionbarText = randomText(actionbarText);
                nextActionbarSwapAt = current + random.nextLong(2500, 5000);
            }

            getPlayer().sendActionBar(CC.a(animatedLine("&8[&dALERT&8] ", actionbarText, actionbarFrame++)));
        }));

        tasks.put("chat", Tasks.runRepeatingSync(5, 5, () -> {
            if (!hasPlayer())
                return;

            final long current = System.currentTimeMillis();
            if (current < nextChatAt)
                return;

            nextChatAt = current + random.nextLong(1200, 2800);
            getPlayer().sendMessage(CC.a("&8[&6SPONSORED&8] " + randomColor() + randomText()));
        }));
    }

    @Override
    public void onStop() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        if (hasPlayer()) {
            for (ActiveBossBar bar : bossBars.values()) {
                getPlayer().hideBossBar(bar.bossBar);
            }
            getPlayer().sendActionBar(CC.EMPTY);
        }
        tasks.clear();
        bossBars.clear();
    }

    private void createBossBar(long now) {
        final int id = random.nextInt(10000);
        final String text = randomText();
        final BossBar.Color randomColor = BossBar.Color.values()[random.nextInt(BossBar.Color.values().length)];
        final long durationMillis = random.nextLong(6000, 22000);
        final long expiresAt = now + durationMillis;
        final BossBar bossBar = BossBar.bossBar(
                CC.a(animatedLine("&8[&cAD&8] ", text, random.nextInt(1000))),
                1f, randomColor, BossBar.Overlay.PROGRESS
        );

        final ActiveBossBar active = new ActiveBossBar(bossBar, text, durationMillis, expiresAt, random.nextInt(1000));
        bossBars.put(id, active);
        getPlayer().showBossBar(bossBar);

        final long durationTicks = Math.max(1L, durationMillis / 50);
        final String deleteKey = "bossbar_delete_" + id;
        final String updateKey = "bossbar_update_" + id;

        tasks.put(deleteKey, Tasks.runLaterSync(durationTicks, () -> {
            if (hasPlayer())
                getPlayer().hideBossBar(active.bossBar);
            bossBars.remove(id);
            final BukkitTask updateTask = tasks.remove(updateKey);
            if (updateTask != null)
                updateTask.cancel();
            tasks.remove(deleteKey);
        }));

        tasks.put(updateKey, Tasks.runRepeatingSync(2, 2, () -> {
            final float remaining = (active.expiresAt - System.currentTimeMillis()) / (float) active.durationMillis;
            active.bossBar.progress(Math.max(0f, remaining));
            active.bossBar.name(CC.a(animatedLine("&8[&cAD&8] ", active.text, active.frame++)));
        }));
    }

    private String randomText() {
        return POSSIBLE_TEXTS.get(random.nextInt(POSSIBLE_TEXTS.size()));
    }

    private String randomText(String except) {
        if (POSSIBLE_TEXTS.size() <= 1)
            return randomText();

        String next;
        do {
            next = randomText();
        } while (next.equals(except));
        return next;
    }

    final String[] palette = {"&a", "&b", "&c", "&d", "&e", "&f", "&3", "&6", "&7", "&9"};

    private String animatedLine(String prefix, String text, int frame) {
        final String base = palette[Math.floorMod(frame / 2, palette.length)];
        final String accent = palette[Math.floorMod((frame / 2) + 2, palette.length)];
        return prefix + base + text.replace(" ", accent + " ");
    }

    private String randomColor() {
        return palette[random.nextInt(palette.length)];
    }

    private boolean hasPlayer() {
        return getPlayer() != null && getPlayer().isOnline();
    }

    private static final class ActiveBossBar {
        private final BossBar bossBar;
        private final String text;
        private final long durationMillis;
        private final long expiresAt;
        private int frame;

        private ActiveBossBar(BossBar bossBar, String text, long durationMillis, long expiresAt, int frame) {
            this.bossBar = bossBar;
            this.text = text;
            this.durationMillis = durationMillis;
            this.expiresAt = expiresAt;
            this.frame = frame;
        }
    }

}
