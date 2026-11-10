# Villagers0.2.0 — experimental playable Fabric build

Minecraft26.3, Fabric Loader0.19.5+, matching Fabric API. Tested in the pinned
Vanilla Perfected modpack. This is a playable mod build, not a validated trained
model or a demonstrated self-sufficient population.

## Play now on this machine

Run `./tools/play.sh` from the project, or double-click `Villagers Play.sh` beside
this README. The prepared private play copy opens the generated village at normal
20TPS. Its saves/minds/configs are separate from the two training copies and the
existing human Modrinth profile. No launcher accounts or credentials are copied.
The observer is creative so you can take tools/food/seeds from the inventory and
try interactions. Villagers remain damageable.

- Sneak-right-click a managed villager to transfer one held sword, hoe, food or
  planting item. Your item is consumed; existing equipment is preserved.
- Crop work requires a real hoe, matching real planting material and a reachable
  ripe wheat/carrot/potato/beetroot crop. Harvesting immediately replants. General
  block breaking and placement stay disabled. Bread crafting is not implemented.
- `/villagers status` inspects the nearest resident.
- `/villagers release` restores vanilla AI while retaining its identity file.
- `/villagers trainingstatus` shows the current trial state.
- `/villagers play` stops acceleration in an adaptive training window and returns
  it to20TPS. A frozen-control process stays frozen; use the dedicated play copy
  for normal adaptive gameplay.

## Install in a Modrinth/Fabric instance

With that instance closed, add only `villagers-0.2.0.jar` to its mods folder.
Keep Fabric API installed. Back up worlds before adopting residents. The release
JAR does not start the development world or accelerate an ordinary launch.
Use `/villagers adopt` for one nearby villager or `/villagers adoptnearby` for
unmanaged residents within48blocks. These are operator commands; game cheats or
server permission are needed. Sneak interaction works after adoption.

## Limits and evidence

This version includes typed tools, doors, crop/replant permissions, physical route
stall recovery, grounded bed use, bounded contextual learning and persistent
world+UUID records. Golems retain vanilla AI. Full village economy, tool supply,
food physiology/crafting, genetic evolution and reliable long-run survival are
unfinished. Positive crop/combat exposure is not implied by faster simulation.
The failed historical trials are retained in docs/evidence/. Pair results and TPS
measurements live in runs/<role>/saves/VillageTrial/villagers/training-evidence/.
