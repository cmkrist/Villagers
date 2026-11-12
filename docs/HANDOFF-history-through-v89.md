# Goal active: actual state at game time274970

The authoritative primary is stopped normally:
`runs/native-four-regions-play/saves/NativeFourRegions`.
All187 saved humanoids survive, including every original179. Eight additional
adults appeared during v79; these are not verified births. No new identities
in v80–v89. All56 managed villagers have20HP and nonzero food. No verified
native births, armor, diamonds, Nether/End entry or dragon victory.

All Minecraft clients/rehearsal servers are terminal. No NPC setters, supplied
stock, replacement adults, rollback or copy promotion. Never splice diagnostic
progress into the actual primary. Old authored FourBiomesWar hashes still match.
Dirty shared tree: never reset/clean unrelated work or overwrite a live JAR.

Latest evidence:
`docs/evidence/2026-11-11/native-fall-scopes-and-real-camp-floors/`.
Archives through v44/v58/v70/v81 retain detailed earlier work. Through-v81 has
shared body routing, path metadata, paid water seals and original deployment.

## Actual current people and resources

Camp founders are existing Pip-460a
`460ab4b6-1522-4504-9673-25fd503e0610` and Juniper-e9b7
`e9b7b192-009b-4158-bc2d-1a63456a6f91`. Both24HP; food17/20 at final snapshot.
Zero family attempts. Do not assume the older copied trials' different pair.
Pip was walking to visible company; Juniper seeking actual supplies.

Actual v89 recorded four real bread gifts: Pip one, Juniper three. Last donor
receipts identify Tansy/Linden, donor stock2→1, recipient0→1, actual contact,
closed cell gates and restored levers. Tansy now13food, Linden12, Moss16,
Ember11; all20HP. Remaining low-food residents, all20HP:

- Birch-764e `764e71da-6e8c-4016-b4cb-b9aee031c564`:6.
- Rowan-8889 `88893f4d-fbba-4245-b563-089e8c0a5b52`:6.
- Juniper-e3de `e3de735e-23c0-421b-a93d-34e386a06ff2`:5.
- Pip-4c3c `4c3c0db4-75ea-42f5-8f91-b1c4814944d7`:5.
- Birch-67f3 `67f32c45-c10a-4f05-a74d-d1d31fdeac0e`:5.
- Clover-d10c `d10cee59-b8c5-41fa-863a-1e455d73749b`:5.

Clover-edc5 adventurer `edc5ea2a-f488-4aea-961c-456c3f7200f1`:
20HP/19food, feet(-476.32643553145357,60,-378.5010903615746).
Held earned iron pickaxe damage14; offhand wooden pickaxe damage4.
Bag stonepick70, woodenhoe17, dirt30, bread2, wheat2, carrot6, stick4, rawiron1.
No ingots or armor.72 retained footings, minimum54; some are reserved supports.
Across v81/v88/v89, three actual dirt foundations at(-469,55,-381),
(-468,55,-381),(-467,55,-381), with dirt33→32→31→30.14 actual mined blocks
since the formerly undamaged iron pickaxe was equipped. No primary water seal.
Source v78's213 footings/two ingots belong only to that copied miner.

## New production changes, verified

NativePathLimits.mustReplan now covers fall-only scopes too. A route cached
without a fall bound cannot satisfy a later limited search. Existing scoped
PathNavigationMixin still preserves active path, target/range and timeout
metadata during inspection; moveTo commits only the chosen route. Frozen v83
and v86 prove fresh fall-limited paths and active-target preservation for both
founders. All ordinary unscoped caching remains native.

VillageSurvival bounds native refuge/recovery/home/golem/escape searches to two
blocks of fall. Other scenario worlds keep prior behavior. This changes native
planning, not movement or damage. It does not prove immunity to every cliff
encounter or knockback. Old v74 Fern-9bcb failure (husk spear7+5 then fall7,
food12) remains in archived evidence. Avoid overstating the new cap's scope.

Native camp founders now consider visible, willing family visits before another
optional patrol. Actual heard meal requests still take priority; consent,
readiness, paid bread, kinship, native nursery/path checks remain. Other
scenario pillagers retain old order. No birth verified.

CampBeds.pathToStands now targets the native node at actual supported floor
height, rounding fractional slab/stair tops upward. Previously an air candidate
could describe real supported feet one cell below its requested goal, causing
canReach=false even after reaching the physically correct floor. Extended camp
searches allow256 walking distance with the existing32 node multiplier: the
westward dry detour around a stream needs135–137 nodes despite nearby goals.
Native dry/body/drop/camp/physics checks remain. No bridge/window was added.

VillageFamilyProbe read-only report and guarded-exit snapshots now include
already-loaded managed creature states, preserving gift and family receipts
that disappear at shutdown. The loaded-state guard avoids creating new minds.

## This turn's runs

- v82 frozen current primary:187 IDs retained; four western residents had no
  complete handoff paths. No primary changes.
- v83 frozen fall/cache checks:PASS for both founders; no reachable one-block
  window stands. Do not implement a window based on this failed hypothesis.
- v84 full-resident care, before real-floor/detour fix:health_stop5120,
  Tansy4HP/0food,three gifts,all187 identities retained. Failed save preserved.
- v85 frozen larger search found130–137-node partial routes ending at real
  floors below the requested goals. This changed the implementation decision.
- v86 frozen corrected production routes:Juniper had complete135/136-node
  handoff paths to Ember/Moss; Pip's corresponding paths were still blocked.
- v87 live corrected care:eleven actual gifts,health_stop9600,Ember4HP/0food;
  all187 identities retained,zero births. Better care is not sustained survival.
  Pip had spent original wheat and lacked a surplus meal; Juniper had six wheat
  but struggled with supply routes. Pip collected a real fallen cyan bed after
  five paid masonry blocks. No copied state was promoted.
- v88 ACTUAL primary:2000 ticks,normal save272969,187 identities/no new,
  all56 managed20HP/nonzero food. One new mined block and paid foundation.
- v89 ACTUAL primary:2000 ticks,normal save274970,187 identities/no new,
  all56 managed20HP/nonzero food; four actual bread gifts, one more mined block
  and paid foundation. No births or armor. Receipts now in budget-native.json.

Source hashes stayed unchanged during both primary runs, old authored-save
hashes matched, observed native80TPS. Every tick runs; this is pacing, not a
learning-speed result. Current config enables nativeCampMeals,
nativeCampPassages,nativeAdventureQuarry; nativeCellEscape staysfalse.
Last observer(-550,90,-480). Health guard18 checks loaded managed villagers
at every tick and freezes before quit; it does not guard pillagers.

## Build and next action

Latest530 Java tests pass,zero failures/errors:
`/tmp/villagers-native-guarded-creature-receipts-tests.log`.
JAR `/tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar`;
SHA `ece655a88fdb206df25a29e64c928f9466c339b6eefd1fd82040f37d5d34e102`.
The earlier behavior-only JAR used for v86/v87/v88 had SHA229f59d959497300066b9e848a031b049fe77f39e059c678e01bb910da482dba.
The last change adds only read-only creature receipts. Primary runClient loads
recorded development sources/classes, not the separate JAR; its provenance is
in source-before.json and compiled-classes-after.json. Diff check clean.

```
source tools/java-env.sh
villagers_select_java
./gradlew --project-cache-dir /tmp/villagers-mineral-gradle-cache \
  -I /tmp/villagers-mineral-isolated-build.gradle build test
```

Next prioritize recurring provisions and actual deliveries for the six remaining
low-food residents, then safe family visits/paid housing/births. Investigate
source-bound supply routes: v87 Pip had only an owned bed plus one held bread,
Juniper had wheat6/bread2 total but failed nearby supply paths. Do not infer
stocks remotely in behavior. New normalized/long paths may be needed in
non-CampBeds callers too; prove native routes before broadening anything.
Mining also needs real continuation beyond one foundation/front per short
session. Armor/diamond and native Nether/End progression remain essential.

Use immutable copied rehearsals to explore failures. Full care:
`tools/food-access-audit.py --illagers --rehearse --camp-meals --camp-passages
--all-residents`, with source/runtime/server-home/verified production JAR.
Mining additionally needs --boats --rehearse --quarry; frozen boat data is
.initial, not .actors. For copies, all-resident tickets can expose remote
threats absent from the observer's local chunks. Read actual process and
terminal on observation expiry; never restart solely because a wait expired.

A further actual bounded run uses tools/observe-native-play.py with a fresh
runtime, --min-health 18, --allow-quarry and current
observer. Do not reenroll or recreate founders. Always audit all187 saved
identities and retain actual outcomes. Goal remains active.
