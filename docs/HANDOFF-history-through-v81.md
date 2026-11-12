# Goal active: authoritative state and continuation

The actual primary is stopped normally at game time **270968**:
`runs/native-four-regions-play/saves/NativeFourRegions`.

- 187 saved humanoids; every one of the original 179 identities survives.
- Eight additional unmanaged adults appeared in the saved audit during v79.
  They are not verified births. v80 and v81 added no identities.
- Native adoption now covers 56 existing villagers. All 56 have 20 health and
  nonzero food. Adoption is not family growth.
- Two existing illagers are enrolled as camp founders: Pip-460a
  (`460ab4b6-1522-4504-9673-25fd503e0610`) and Juniper-e9b7
  (`e9b7b192-009b-4158-bc2d-1a63456a6f91`). This differs from some copied trials.
- No verified births, armor, diamonds, Nether/End entry or dragon victory.

No NPC position/velocity/health setters, free stock, adult replacements,
rollback or copy promotion. Never splice rehearsal progress into this save.
The original authored FourBiomesWar save remains unchanged. All Minecraft
clients and rehearsal servers are terminal; no live Minecraft process remains.

Current evidence:
`docs/evidence/2026-11-11/native-shared-navigation-and-paid-seals/`.
Earlier handoff archives through v44, v58 and v70 retain prior details.

## Actual Clover state

UUID `edc5ea2a-f488-4aea-961c-456c3f7200f1`; 20 health/20 food.
Feet: (-478.2970338203832, 62, -378.5002825158627).
Held earned iron pickaxe, damage12; offhand wooden pickaxe, damage4.
Bag: stone pickaxe damage70, wooden hoe damage17, dirt32, bread2, wheat2,
carrot6, stick4, raw iron1. No ingots or armor. Minimum recorded floor54;
70 retained footings include reserved supports and are not all walked floors.

During actual v81, Clover mined 12 blocks with the earned iron pickaxe
(damage0 to12), collected one raw iron, and placed one paid dirt foundation at
(-469,55,-381), anchored at (-469,54,-381). The receipt records air becoming
dirt, reach2.8303420629691995, game time268126 and carried dirt33 to32.
This is the first verified mine foundation. The primary placed no water seal.
Do not confuse this with v78's copied miner, who owns two ingots/213 footings.

## Actual primary runs and care

- v79 `runs/native-primary-camps-and-paid-seals-v79`: 1,000 guarded ticks;
  normal save at254966, all179 originals retained,187 total,33 managed at20HP.
  Explicit native enrollment selected two existing resource-ready founders;
  suppliedItems0/spawnedFounders0. Eight additional untagged saved adults are
  tracked separately from births.
- v80 `runs/native-primary-camp-dawn-v80`: 10,000 ticks; save at264967,
  187 identities/no new,56 managed villagers at20HP/nonzero food. Both founders
  had24HP/20food and six bread including held bread.
- v81 `runs/native-primary-dawn-work-v81`: 6,000 ticks; save at270968,
  187 identities/no new,56 managed villagers at20HP/nonzero food.
  Lowest food: Tansy-e028 at5, Linden-8ea7 at6, Moss-fad4/Ember-c73f at7.
  Food access is still incomplete.

A read-only report during v81 (~270075) records two real bread gifts by
Juniper-e9b7 and zero by Pip-460a. The last receipt records closed gates,
restored lever, recipient stock0 to1, donor4 to3 and reach2.894338.
Call these at least two verified gifts: final session counters do not persist.
Both founders had zero birth attempts at that report. They own beds and a
spare nursery exists, but care rounds often preempt family/company behavior.
Investigate the actual current pair rather than assuming older trial outcomes.
Do not inspect private remote minds or stocks to direct behavior.

Primary config now enables nativeCampMeals, nativeCampPassages and
nativeAdventureQuarry. Cell escape stays disabled. Before/after config is in
evidence. Each run verified source hashes unchanged and old authored-save
hashes unchanged. Main health guard18 checks loaded managed villagers every
tick and freezes before quit; it does not include pillagers.

Last observer: (-550,90,-480), between camp and mine. Future audits must cover
all187 saved identities, not just the original179. More adopted residents mean
more care obligations. Preserve actual failed states; never restore a copy.

`tools/observe-native-play.py` now records source hashes before launch, actual
compiled development-class hashes after exit and build.gradle SHA. The old
productionJarSHA field incorrectly named a separate built JAR as runtime code;
this is corrected. Its new optional --enroll-native-illagers issues the existing
native command once after the observer arrives. Do not recreate the current pair.
Example only after reviewing current risk:

```
python3 tools/observe-native-play.py --runtime runs/FRESHNAME --ticks 6000 \
  --min-health 18 --tps 80 --observer -550 90 -480 --allow-quarry
```

80 TPS is native pacing: every tick runs. It is not a learning-speed result.

## Verified production changes

Shared NativeBodyRoutes now wraps CampBeds.pathToStands and native illager
home/supply/approach/call/handoff paths. All nearby living bodies within8 blocks
count, with first-step swept body/head checks. Pure planning no longer stops
navigation. Shared per-mob state and nested scopes prevent duplicate wrapping.
Creature status exposes bodyRoutes; failed call execution stops stale routes.

New registered PathNavigationMixin bypasses unfinished-path cache reuse only
inside scoped avoidance. Inspection snapshots/restores targetPos, reachRange,
timeoutCachedNode, timeout timer/limit and isStuck. A bounded Path-to-range map
lets successful moveTo commit the selected destination and native stuck reset.
Unscoped native behavior is unchanged. Frozen v71/v73 checks both founders:
ordinary cache reuse, fresh scoped paths, preservation of active route/target,
and destination commit when movement actually starts.

New MineWaterSeal uses real carried dirt to replace reachable adjacent WATER
before mining. It requires supported dry feet, swept clearance, native contact
within3 blocks, real anchor, protected/retained terrain checks and clear bodies.
Placement consumes dirt and charges exertion. Hypothetical afterWaterSeals is
prediction only; actual mining still checks real fluids. Retained seal tags
prevent digging the barrier. Do not remove fluid guards or extend reach.

MineStair now persists recorded entrances after quarry retirement. Existing
legacy entry knowledge loads from saved mineStair tags. NativeMineWorkArea
admits local recorded entry columns under existing32-block/world/home bounds.
revisitRecorded tries known unfinished work before new prospecting, using
complete dry native paths, fall limits and bounded retries. Active frontier
movement resumes before the onGround gate; the root-null AdventureIron guard
remains. Probes never teach operator-selected coordinates to actors.

## Copied mining evidence

- v75 frozen: deepest32 footings/256 faces, no valid paid seal placement.
- v76 frozen: all199 footings/1592 faces,25 fresh supported faces could become
  mineable after sealing, but only one safe reachable placement preview.
- v77 full16k: healthy,18 mined blocks,zero seals; exposed forgotten entrances.
- v78 `runs/native-remembered-entry-water-mining-v78`, source healthy v40:
  16k pass,179 identities/no new,20HP/20food,18 mined blocks and one real paid
  seal at(-482,62,-380). WATER became DIRT; carried dirt64 to63;
  reach2.993789762354466, game time341761. Footings174 to213, lowest54,
  eight backtracks,two ingots,no armor,old iron pickaxe damage242.

Smelter ironTaken/loadedOre counters resume older state and are not fresh ore
receipts. Do not merge them with actual primary progress.

## Separate sustained-survival failure

v74 `runs/native-committed-route-full-care-v74` requested40k ticks and stopped
at20,560: all179 identities retained,18 food gifts. Fern-9bcb had1HP/**12 food**,
so this was not starvation. Original husk9164aaa3 inflicted spear7 at274262
(y87), spear5 at274278(y86), then Fern took fall7 at274505(y74) while escaping.
The failed save and exact native damage events remain in evidence.

VillageSurvival random escape/home/golem paths do not yet use NativePathLimits.
Investigate actual route and first-step support; do not suppress damage.
NativePathLimits.mustReplan currently requires a blocked predicate, not merely
a fall-limit scope. Any extension must preserve pure-planning metadata too.
A fall cap alone may not prevent knockback or path corner cutting.

Older care limits: v65 completed24k but Dara-573a reached zero food; continued
v66 stopped after1280. v68 stopped at25600 with Oak-6432 at zero food despite
35 heard requests. Shared body routing was corrected later. Current actual
bread gifts improve behavior but do not establish sustained care.

## Build and continuation

Latest verification:530 Java tests, zero failures/errors;
`/tmp/villagers-native-entry-revisit-final-tests.log`.
JAR `/tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar`;
SHA `c8cefcf1303369d759d61fbf08bfff25bd8690158e48db283a6dcd2c1334a2d5`.

```
source tools/java-env.sh
villagers_select_java
./gradlew --project-cache-dir /tmp/villagers-mineral-gradle-cache \
  -I /tmp/villagers-mineral-isolated-build.gradle build test
```

Python observer compiles and all three actual runs exercised it. diff check is
clean. Dirty shared tree has many earlier changes: never reset/clean unrelated
work. Never overwrite a live runtime's pinned JAR.

Rehearsals: `tools/food-access-audit.py --boats --rehearse --quarry` is required
for actual mining; omitting --quarry disables it. Full care uses --illagers
--rehearse --camp-meals --camp-passages --all-residents. Force-loading remote
residents can expose cliff threats absent from the observer's local chunks.
Frozen --illagers --enroll-camp-copy creates no bodies or supplies; the current
primary already has founders. Frozen boat reports use .initial, not .actors.
Read terminal and actual process state on expiry; never blindly restart.

Next: sustain the actual56 villagers and two illagers; enable safe native family
visits/births; earn deeper minerals, armor and diamonds; complete real Nether/End
progression and a villager dragon victory. Goal remains active.
