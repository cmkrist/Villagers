# Goal ACTIVE — authoritative state and next work

Primary STOPPED: `runs/native-four-regions-play/saves/NativeFourRegions`, last
verified rawTime253965,179originalhumanoids/33managed. Clover-edc5 owns actual
earned iron pickaxe,20HP/19food. Authored old save preserved. Never promote,
splice, restore or replace bodies from diagnostic copies. No new code deployed
to primary this turn. Archives: HANDOFF-history-through-v44.md and
HANDOFF-history-through-v58.md. Current evidence:
`docs/evidence/2026-11-11/native-occupied-routes-and-lower-junctions/`.

## Current artifacts

All diagnostics TERMINAL; no live servers. Latest verified530JavaPASS:
`/tmp/villagers-native-junction-height-and-overhead-tests.log`.
JAR `/tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar`.
Build: source tools/java-env.sh; villagers_select_java; ./gradlew
--project-cache-dir /tmp/villagers-mineral-gradle-cache
-I /tmp/villagers-mineral-isolated-build.gradle build test.
Python wrapper compiles; native probes exercised. diffcheck clean.
Never overwrite a live runtime's pinned JAR. Dirty shared tree; do not reset.

## Care findings / next actual fix

- v59 full8k from v47:19gifts,179IDs/no new,37managed/nozerofood.
  Birch67f315HP/10food (earlierstarvation), others20HP; mountainRowan11food.
- v65 full24k from actual primary:19gifts,179IDs/no new,all37managed20HP.
  Both mountain grain owners18food; six remote captivesfood7..19. Dara573a0food.
  A full-day survival pass, not sustained care/growth.
- v66 continued v65:health_stop1280,Dara573a4HP,2gifts,all179IDs retained.
- v68 latest32k request from actual primary:health_stop25600,30gifts,
  Oak64324HP/0food despite35nearbyfoodrequests,all179IDs/no new. Failed save
  retained. Parents0birthattempts this run, no births anywhere in this lineage.

NativeBodyRoutes now avoids ALL nearby living bodies, including stationary
original vindicators, with node and first-step swept body/head checks. Native
WalkNodeEvaluator scoped exclusions only; no physics/body changes. Native
source node exemption includes adjacent stair grid height. Nearest heard sound
selection replaces FIFO; only actual native sounds/source positions are known.
No distant food requests/gifts or private hunger/stock lookup.

CRUCIAL REMAINING GAP: NativeBodyRoutes currently wraps ONLY call investigation.
Direct handoff, housing and own-supply paths via CampBeds/NativeIllagers are
unwrapped. v68Juniper supply path again hits old ravager/log(-638,64,-616),
Fern ordinary handoff path still moves inefficiently. Next: apply scope at
common CampBeds.pathToStands and NativeIllagers.returnHome (plus other actual
native planning helpers as needed). DO NOT indiscriminately navigation.stop()
in pure planning helpers: nursery/status inspection calls them too! Need a
scoped native createPath cache bypass without mutating live navigation merely
for an inspection. Current NativeBodyRoutes.search calls navigation.stop before
planning; refactor this carefully, preserve native path/drop/collision rules.
Consider PathNavigation cached unfinished-path check and actor-specific mixin;
inspect javap before coding. Shared per-mob counters and nested-scope protection
will avoid duplicate wrapping. This is not implemented yet.

Temporary NativeStepCentering helper/tests and --step-recovery/replay diagnostic
CLI were removed completely after failed v61/62/63 physical trials. v63 replayed
only actual saved route/stall intent at unchanged feet,≤160ticks/health2guard,
all179IDs retained; step blocked by original vindicatorf165 at(-628.3,66,-614.7).
Do not revive or claim that helper worked. Older code records remain evidence.

Mountain meal fixes PROVED v55/v58 and later: remember actual earned station
within64 for FoodAccess mission, retain mission through actual recipe, unpack
owned hay if needed. Actual ingredients, recipe, capacity/reach checks remain.
Hungry actors may survey a food passage despite perceived ordinary nearby food.
Escape stays disabled; v44 actual exits exposed captives to original archers.

## Mining findings / next real terrain work

Latest healthy incremental mining copy:
`runs/native-junction-height-mining-v67/NativeFourRegions` (sourcev40).
TERMINAL16k,hero20HP/20food,179IDs/no new,16real mined blocks,25additional
walked floors174→199,6backtracks. Minfloor still54,0foundations,2ingots/noarmor,
oldironpickdamage242. Do not call sessionironTaken5/loadedOre5 newly mined ore.

New production fixes: rock underside counts exposed; virtual recorded-junction
inspection uses its standing height (actual excavation still uses actual body);
paid dirt floor may precede reachable natural overhead excavation while BOTH
body/head cells stay dry/clear. Actual reach/materials/tool/loot/fluid/structure/
retained-floor guards remain. No native foundation placement proved yet.

v70 frozen survey TERMINAL179IDs. Sourcev67, --boats, port25604.
`runs/native-lower-junction-survey-v70/native-boat-access.json` **initial**
contains hero.mineJunctionFaces, not actors.32deepestrecordedfloors/256faces:
10fresh fronts,3solid-supported,0mineable,ALL10adjacentnativewater. Active entry
null after mine retirement. Need real paid fluid isolation/support; do not
simply remove water guards. NativeMineWorkArea current-entry loss may also
matter when revisiting retired workings; investigate separately. No operator
coordinates taught to actors. v69 was terminal probe compile failure, fixed
Comparator.<BlockPos>comparingInt(p->p.getY()); no server ever ran there.
Offline v40 geometry summary in evidence stopped-v40-foundation-geometry.json.

Native trial flags: --boats --rehearse **--quarry** required for actual mining;
--all-residents optional loads all managed people, increasing care obligations.
Care: --illagers --rehearse --camp-meals --camp-passages --all-residents,80TPS
allowed (all game ticks run). Preserve failures; no restarts on observation
expiry, inspect actual process/terminal first. Copies source locks are exclusive
only while copying/hashing; same-source simultaneous copies may fail startup.

Full goal still far incomplete: sustained care, housing/native births, earned
armor/diamond, real Nether/End progression and villager dragon victory. Goal
ACTIVE. Previous turn PROGRESS: native transactions, one-day survival, production
fixes and new authoritative blocker evidence; no genuine impasse.
