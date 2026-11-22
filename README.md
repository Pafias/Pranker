# Pranker

Prank your minecraft players with this plugin

# Installation

Should work on most non-legacy versions, but was designed around 1.21.11

Just put it in your plugins folder and you're done. There is no config file.

# Usage

**/prank \<player\> \<prank/stop\> [options/stop]**

***player*** will be the player to apply the prank on (obviously)

***prank*** will be the prank to apply, or ***stop*** to stop all pranks on the player

***options*** are extra settings some pranks take (listed below, tab completion helps), or ***stop*** to only stop the
specified prank on the player

Multiple pranks can be applied to a player. Pranks will be stopped when the player logs out

**/meow \<player\>**

Meows at another player, who hears it too. Requires the `meow.perm` permission, has a 30 second cooldown, and only
reaches players within 10 blocks. Players with `meow.bypass` ignore the cooldown and range limit.

**/purr [volume] [seconds]**

Toggles purring on and off. Requires the `meow.purr` permission. While purring, one purr is played every 30 seconds
by default and is only heard by players within 5 blocks. The volume defaults to `0.15` and can be set from `0` to `1`,
and the second argument sets the interval in seconds. Running `/purr [volume] [seconds]` while already purring changes
those settings; running plain `/purr` stops it.

# Overview of the pranks

*More prank ideas are welcome :)*

The **(C)** in the name means the prank is client-sided. How this works will vary per prank, but generally it means that
other players will not see the prank happening, or it will not affect other players, or the world.

- Reach `[distance]`
    - Gives the player extra reach as if they were cheating (6 blocks by default)
- LagMove
    - Occasionally teleports the player a block back creating the illusion of lag
- NoDamage
    - Makes the player's hits deal 0 damage
- Cat
    - Turns the player into a cat.
    - This clears their inventory, applies invisibility to them, and spawns a cat which will mimic their movement and
      mouse inputs. The player and cat will be damageable, but will take 0 damage.
    - When stopping the prank all above is reversed and the player gets their inventory back.
- DropItem
    - Makes the player occasionally drop the item(s) they are holding in their main hand
- TNTWalk (C)
    - Spawns a primed tnt on the player's location whenever they move.
    - (C): the rest of the players will not see the TNT, and no blocks will be destroyed.
    - The explosion knockback may set off "player moved too quickly" warnings, and not let the player fly with the
      explosion. This will result in a weird set back effect which may be unpleasant. Try it yourself to see what it
      looks like.
    - I don't know if this will trigger any anticheat, but be aware that it might.
- ChristmasHat
    - Gives the player a fancy christmas hat :D
    - This hat is made of particles, which may cause low fps on players with potato pc
- Undercover
    - Gives the player a bush disguise made of oak leaves
    - Basically fortnite bush
- Minigun
    - Makes the player shoot arrows like a minigun by just holding right-click
    - Does not need nor deplete arrows
- Dance
    - Forces the player to dance <img alt="catvibe" src="https://pafias.me/catvibe.gif" width="32" height="32"/>
    - This may also drop players' fps with the particles
    - Basically fortnite boogie bomb
- UpsideDown
    - Turns the player upside down
    - This will change their nametag to Dinnerbone because that's the only way to achieve it without client mods
- SelfShoot
    - Makes the player's arrow backfire when shooting an entity
    - Which means the entity will shoot an arrow back at the player
    - May not work too well, like the arrow not having enough power to hit the player back if they're too far away from
      the target
- MidasTouch (C)
    - Turns everything the player touches into gold
    - More like: turns the block under the player into a gold block when they walk. Also turns the block the player
      clicks into gold
- Swim (C)
    - Set all other players in the swimming pose
- WitnessProtection (C) `[radius]`
    - Spawns a rotating circle of FBI agents around the player (3.5 block radius by default)
    - (C): they're not real entities, but everyone will see them
    - If a player gets too close (within the circle), they will be pushed back
- Demo
    - Triggers the demo screen on the client
- Credits
    - Triggers the credits screen on the client
- Hole
    - Opens a hole down to the void under the player
    - The blocks are restored after 7 seconds
- Schizo (C)
    - Every few seconds, when the player turns their head, a random player briefly appears at the edge of their vision
      and vanishes
    - Every hit there will be a chance of a following "ghost hit"
- Spam (C)
    - Spams the player with fake ADs in chat, the action bar, and boss bars
- Pose `<pose>`
    - Forces the player into a pose: crouching, swimming, sleeping, fall_flying, spin_attack or dying
    - Leaving the bed ends the sleeping pose
- Shake
    - Makes the player shake as if they were freezing in powder snow, for everyone including themselves
    - They don't actually freeze or take damage
- Cage `[block]`
    - Builds a cage around the player (glass by default)
    - The blocks are removed when the prank is stopped
- Popular
    - Spawns villagers, "Fan of \<player\>", that follow the player around
- Jumpscare (C)
    - Plays the elder guardian jumpscare on the player's screen
- DiskSpaceWarning (C)
    - Shows the player a fake low disk space warning
    - Only works on 26.1 and newer
- Timelapse (C)
    - Makes the day/night cycle fly by for the player
- Forcefield `[radius] [exempt players...]`
    - Flings other players back when they get too close to the player (5 block radius by default)
    - Players listed after the radius are not affected
- WeakArmor
    - The player's armor breaks the moment they get hit by something
- Respect (C) `[radius]`
    - Makes nearby players bow to the player (10 block radius by default)
    - (C): only the player sees them bowing
- Morph `[entity] [options...]`
    - Disguises the player as another mob for everyone else (a cat by default)
    - Options customize the mob, like `black`, `baby` or `collarcolor=red`
- Force
    - Gravity gun: hold right-click on someone to grab them, scroll to push/pull them and left-click to throw them
