# Pranker
Prank your minecraft players with this plugin

# Installation
Should work on most non-legacy versions, but was designed around 1.17

Just put it in your plugins folder and you're done. There is no config file.

# Usage
**/prank \<player\> [prank/stop] [stop]**

***player*** will be the player to apply the prank on (obviously)

***prank*** will be the prank to apply, or ***stop*** to stop all pranks on the player

***stop*** will only stop the specified prank on the player

Multiple pranks can be applied to a player. Pranks will be stopped when the player logs out

# Overview of the pranks
*More prank ideas are welcome :)*

The **(C)** in the name means the prank is client-sided. How this works will vary per prank, but generally it means that other players will not see the prank happening, or it will not affect other players, or the world.

- Reach
  - Gives the player a 6 block reach as if they were cheating
- LagMove
  - Occasionally teleports the player a block back creating the illusion of lag
- NoDamage
  - Makes the player's hits deal 0 damage
- Cat
  - Turns the player into a cat.
  - This clears their inventory, applies invisibility to them, and spawns a cat which will mimic their movement and mouse inputs. The player and cat will be damageable, but will take 0 damage.
  - When stopping the prank all above is reversed and the player gets their inventory back.
- DropItem
  - Makes the player occasionally drop the item(s) they are holding in their main hand
- TNTWalk (C)
  - Spawns a primed tnt on the player's location whenever they move.
  - (C): the rest of the players will not see the TNT, and no blocks will be destroyed.
  - The explosion knockback may set off "player moved too quickly" warnings, and not let the player fly with the explosion. This will result in a weird set back effect which may be unpleasant. Try it yourself to see what it looks like.
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
  - May not work too well, like the arrow not having enough power to hit the player back if they're too far away from the target
- MidasTouch (C)
  - Turns everything the player touches into gold
  - More like: turns the block under the player into a gold block when they walk. Also turns the block the player clicks into gold
- Swim (C)
  - Set all other players in the swimming pose
- WitnessProtection (C)
  - Spawns a rotating circle of FBI agents around the player
  - (C): everyone will see them
  - If a player gets too close (within the circle), they will be pushed back
- Demo
  - Triggers the demo screen on the client
- Credits
  - Triggers the credits screen on the client