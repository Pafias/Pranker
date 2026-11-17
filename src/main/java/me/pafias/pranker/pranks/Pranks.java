package me.pafias.pranker.pranks;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Pranks {

    CAT("cat", "Cat", "Turns you into a cat"),
    CHRISTMAS_HAT("christmashat", "Christmas Hat", "Puts a christmas hat on the head"),
    CREDITS("credits", "Credits", "Triggers the vanilla credits sequence"),
    DANCE("dance", "Dance", "Forces the player to dance"),
    DEMO("demo", "Demo", "Triggers the vanilla demo screen"),
    DROP_ITEM("dropitem", "Drop Item", "Makes the player occasionally drop their held item"),
    HOLE("hole", "Hole", "Spawns a hole to the void under the player"),
    LAG_MOVE("lagmove", "Lag Move", "Occasionally sets the player back"),
    MIDAS_TOUCH("midastouch", "Midas Touch", "Turns everything the player touches into gold"),
    MINIGUN("minigun", "Minigun", "Lets the player use a bow like a minigun"),
    NO_DAMAGE("nodamage", "No Damage", "The player takes 0 damage"),
    REACH("reach", "Reach", "The player gets reach", "[distance]"),
    SCHIZO("schizo", "Schizo", "Makes the player schizophrenic"),
    SELF_SHOOT("selfshoot", "Self-Shoot", "Arrows the player shoots will backfire"),
    SPAM("spam", "Spam", "Spams the player with ADs"),
    SWIM("swim", "Swim", "Turns everyone into the swimming pose from the player's perspective"),
    POSE("pose", "Pose", "Forces the player into a pose (crouching, swimming, sleeping, fall_flying, spin_attack, dying)", "<pose>"),
    SHAKE("shake", "Shake", "Makes the player shake like they're freezing"),
    TNT_WALK("tntwalk", "TNT-Walk", "Spawns TNTs as the player walks"),
    UNDERCOVER("undercover", "Undercover", "Hides the player in a bush outfit"),
    UPSIDE_DOWN("upsidedown", "Upside Down", "Turns the player into Dinnerbone"),
    WITNESS_PROTECTION("witnessprotection", "Witness Protection", "Forms an FBI force-field around the player", "[radius]"),
    CAGE("cage", "Cage", "Cages the player", "[block]"),
    POPULAR("popular", "Popular", "Sends villagers to the player"),
    JUMPSCARE("jumpscare", "Jumpscare", "Jumpscare the player with an elder guardian"),
    DISK_SPACE_WARNING("diskspacewarning", "Disk Space Warning", "Warns the player about low disk space (26.1+)"),
    TIMELAPSE("timelapse", "Timelapse", "Quickly cycles the world time"),
    FORCEFIELD("forcefield", "Forcefield", "Flings players back when they get too close to the player", "[radius] [exempt players...]"),
    WEAK_ARMOR("weakarmor", "Weak Armor", "The player's armor breaks instantly when they get hit"),
    RESPECT("respect", "Respect", "Makes nearby players bow to the player (only the player sees it)", "[radius]"),
    MORPH("morph", "Morph", "Disguises the player as another entity", "[entity] [options...]"),
    FORCE("force", "Force", "Hold right-click on someone to grab them, scroll to push/pull, left-click to throw");

    private final String id;
    private final String name;
    private final String description;
    private final String argsUsage;

    Pranks(String id, String name, String description) {
        this(id, name, description, null);
    }

    public static Pranks parse(String input) {
        for (Pranks prank : values()) {
            if (prank.getId().equalsIgnoreCase(input)
                    || prank.getName().equalsIgnoreCase(input))
                return prank;
        }
        return null;
    }

}
