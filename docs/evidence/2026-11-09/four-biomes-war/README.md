# Native and visible Four Biomes War acceptance

Minecraft26.3, Fabric Loader0.19.5, JDK25. New isolated native worlds and a new
full-pack rendered client; no prior society or external server world was opened.
All observations are bounded; no NPC positions, targets, health, combat damage
or clocks were set by the battle probe. Native probes use80TPS to advance actual
simulation ticks. The visible client uses ordinary20TPS.

## Native battle and restart

Retained `v1/` proves bilateral marches/hits and creature learning/persistence.
Its first death was friendly fire; it is not evidence of an enemy-caused kill.

The stronger `v2/war-battle-before-restart.json` passes at tick660. Eight soldiers
per faction actually leave home. Native events record13villager hits and9illager
hits, and a pillager is killed by a villager's native melee attack. The report
records learned attempts for pillagers, vindicators, Endermen and wandering
traders. A temporary test player pays exactly one generated merchant offer's
cost through native input/output slots; the merchant output and uses counter
confirm a real transaction. These finite player payment items are a declared
test fixture, not an income stream for the simulation.

`v2/war-battle-result.json` passes reopening at tick700. Hits/marches continue
monotonically, the dead founder is absent, casualty records remain and surviving
representatives keep their names and learned update counts. Another actual enemy
death occurs after reopening. `artifact-pins-*.json`, terminal receipts and logs
bind each native report to its exact mod JAR. These native artifacts precede the
small retreat-tag persistence refinement; the visible client below includes it.

## Full-pack playable world

The new runtime is `runs/four-biomes-play`. The playable save is
`runs/four-biomes-play/saves/FourBiomesWar`. The first rendered run ends at native
tick1004; the second reopens the same world and ends at1608. Full-pack launch and
normal shutdown succeed. No setup messages replay on reopening. The saved
campaign has real casualties from both sides, continued bilateral attack history
and eight neutral actors. No casualties were restored or populations reset.
The observer's camera/mode were adjusted for inspection; NPCs were not moved.

Rendered screenshots, final native campaign/marker files, client logs and
observations are retained in `visible/`. Screenshots show native settlement
infrastructure, villager identities, and soldiers' marching activity. Resourcepack
and optional integration warnings from the pinned pack are present in the logs;
they do not stop this client or the tested scenario.

`./tools/play-war.sh` resumes this saved campaign without a test time budget.
The native probe JARs are not present in the playable client. The separate
`WarMapTest` checks all4096chunk assignments and terrain geometry; the earlier
[world acceptance](../four-biomes-world/README.md) checks actual biomes, populated
settlements and bed pairs before combat begins.

This evidence covers the requested world/AI/war scenario. It does not establish
indefinite survival, complete vanilla spellcasting parity or faction balance.

Final checks:410Java tests and10Python tests pass. The requirement-by-requirement
audit and concrete evidence paths are in `completion-audit.json`; the final unit
counts are in `unit-test-summary.json`. The delivery JAR's248compiled classes
match the classes used by the inspected full-pack client (`visible/binding.json`).
