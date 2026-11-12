# Earned camp entry and real rescue

The primary is saved normally at gameTime290175 with186living humanoids,
56managed villagers and no managed resident at zero food. Historical founder
Juniper-e9b7 remains dead from v108; no rollback, resurrection or copied-save
promotion was used. There is no verified birth, armor, Nether/End or dragon win.

The new NativeCampEntrance skill solves a disconnected fort perimeter using a
real heard voice, visible own-camp masonry, safe native routes before/after,
ordinary work time and hand reach. It may clear one two-column, three-high
volume (at most6blocks), retaining floor/lintel. Existing air is allowed; no
wood, furniture, cell gates or protected blocks are harvested. Body occupancy
pauses work. Drops/tool wear are native; wrong-tool stone yields no resources.
No NPC position, velocity, health, NoAI or invulnerability setter is used.

Frozen v124 exhausted approximately5,150dry physical nodes and14,998native
nodes without getting within23blocks of Dara. v132 showed that a small opening
would join a useful existing route; no block/body was changed by those tests.
Copied v133 then actually removed2stone blocks with200ticks effort apiece,
entered the courtyard, decoded requests and delivered7real meals over4k.
All186living identities remained; source hashes unchanged.

Actual v134 built the entrance and delivered one bread to Clover-d10c, who
healed9→20HP. It preserved186living identities over2k, but Dara was still
13HP/0food when the run ended. The actual state was retained while testing.
Exact-state copied v135 proved rescue: bread reached Dara after54ticks, with
the real native lever restored and cell gates closed afterward. All186survived.
Actual v136 then ran1k, delivered3real meals, and ended with Dara13HP/5food,
minimum managed health13 and all186living. No combat stop; source/authored-save
hashes unchanged; actual simulation distance16 verified. The code ran through
recorded development classes; the separately pinned JAR is for copied trials.

A rejected copied escape experiment v128 killed Ember-c73f after two Vindicator
hits (20→7→0). The feature remains disabled in the primary. It exposed an audit
bug: a dead UUID still saved in NBT made186bodies look preserved despite only
185living. The shared Python audit now checks living identities. The copied
first-hit combat guard in v130 stopped an arrow encounter at16HP and retained
all186living, rather than waiting for a second lethal hit. Guards retain normal
damage and do not prevent a lethal first hit. Actual v134/v136 used these guards.

538Java tests and3Python regression tests pass. Build/pinned JAR details are in
build-tests.json. The original authored FourBiomesWar save remains unchanged.
The previous floating/air fix remains; no successful trial entered water, so
these results do not prove a live underwater rescue.

Frozen v137 found2,398naturally hydrated soil sites in the bounded camp area,
but none currently visible within8of the provisioner. Native v138 rule/tag
checks establish that wheat can maintain dry farmland and grow at positive
native speed without granting water. Three natural soil sites are currently
visible locally. The provisioner owns no seeds, hoe or wood ingredients yet.
These are geometry/API fixtures, not completed farming. Next implement real
seed gathering, hoe preparation, planting and recurring native crop growth.
Operator survey coordinates were never taught to a creature.
