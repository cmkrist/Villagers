# Worker access and recovery — current handoff

**Latest progress:** a schema-compatible, main-settings copied control now passed
8000 ticks with all 192 originals alive. A further 2032 ticks brought Pip to
12.5 HP/16 food and Birch-67f3 to 14 HP/16 food before a different region's Iris
reached starvation at 1 HP. No main gameplay occurred. Work now focuses on Iris's
owned wheat and safe workbench access; keep the full game-completion goal active.

**New result:** the dry grain-route fix below moved Iris to farming and real
owned-potato meals. A new continuation lasted3132 ticks from the8000-tick source
and stopped on Linden-8ea7, not Iris. All192 originals alive. No live worker test
server remains. Linden is the next resident requiring investigation.

The full goal remains ACTIVE: get an actual villager to beat Minecraft through
conserved native actions while preserving current populations and saves. No
armor/Nether/End/dragon victory is proved. No subagents have been used.

## Authoritative main save

The other active goal thread completed actual recovery v269:
`runs/native-actual-frozen-care-v269/result.json` and budget-native.json verify
4000 observed ticks, gameTime308509, all192 originals alive, no new/missing/dead
UUIDs, old save unchanged, no combat stop, immutable source snapshot unchanged.
It adopted existing cb79 in the frozen startup at304508 with identical physical
body/stock before/after and no supplied goods. The actual worker ledger is now
FULL (two additional workers ever); do not recruit another body. This thread has
not independently opened the primary. Earlier v213 health values are historical.
Clover-edc5 still has an earned iron pickaxe; higher progression remains unproved.

Another active user goal thread edits navigation and tests copies in this same
workspace. Do not overwrite its changes, logs, processes or artifacts. Its runs
use port 25604. This line of work uses ports 25615/25616, `runs/worker-*`, pinned
`/tmp/villagers-worker-*` artifacts and isolated build output. The question of
which thread should own the main save was raised earlier; operational ownership
is now clear from the other thread's actual v269 run. Keep this thread on copied
fixes and leave primary progression to that owner. No messages to others or new
agents have been sent.

## Current source changes from this investigation

- Recruitment again uses the original local 16-block neighbourhood and chooses
  the closest eligible existing body with a real resource path. It keeps the
  historical two-additional-workers-ever cap, saved/dead identity history,
  unchanged physical body snapshots, no supplied resources and no spawning.
  The wider 32-block supply-plus-recipient ranking was retired after Rowan
  failed to cover another starving captive despite faster initial Pip feeding.
- `NativeCampWork` again scans stores using the earlier visible-center rule.
  Broader face-based vision changed the selected store and delayed the first
  delivery. Physical opening still uses a real reachable block-face contact.
  All transactional inventory and physical storage checks remain.
- `tools/mind_schema_audit.py` rejects a JAR whose AgentMind schema cannot load
  the saved minds. The copy audit checks before launch, rechecks exact copied
  inputs, records a receipt, and rejects restoration warnings before any pass.
- `NativeIllagerHabitatProbe` loads each managed resident's actual persisted
  mind while frozen and writes `native-resident-startup.json` before unfreezing.
  This prevents an absent async session from falsely stopping a fed 1-HP resident.
- `observe-native-play.py --last-health-recovery` now prepares an optional main
  observer mode restricted to `--min-health 1`, bounded play and starvation guard.
  It follows fed 1-HP villagers, or stops a starving 1-HP villager at timer 60
  before the next lethal interval. Missing/invalid metabolism, drowning, death,
  other thresholds and ordinary combat retain stops. It changes no food, health
  or damage. Disabled by default; not exercised on the main save. LabClient,
  build.gradle and ObservationHealthGuard carry the option and scope checks.

Prolonged gate-wait and broader meal-route comparison experiments were removed
from current source. Do not re-enable their historical JVM properties or promote
any experimental copied save.

## Latest copied test — terminal, no live worker test server

The volunteer experiment has been removed from source, including its hooks and
temporary supply-route flag. Current source builds/tests passed in
`/tmp/villagers-worker-volunteer-removal-build.log`.

## Iris audit — terminal

`runs/worker-iris-production-paths-20261111`, session 10299 TERMINAL, port 25615,
log `/tmp/villagers-worker-iris-production-paths.log`. This is a frozen COPY of
the safely stopped sustain world below; no gameplay or body changes. The probe
now distinguishes normal64 and expanded128 native route safety, so dry/home
checks refer to the same route variant being inspected.

## New verified survival and recovery evidence

- `worker-early-volunteers-care-20261111`: experimental volunteers enabled,
  matching `--camp-meals --camp-passages`, same cb79-adopted starting save. The
  existing Juniper-40b3 actually moved into the courtyard, obtained real supplies
  and gave bread to Birch-67f3, but the run stopped at 5314 ticks when a different
  Birch-764e reached 1 HP/0 food. All 192 alive. Founder Pip-460a still could not
  find a route. This did not justify keeping the new volunteer code.
- `worker-no-volunteers-control-20261111`: SAME JAR and starting save/settings,
  volunteer property absent. **8000 ticks PASS**, all 192 originals alive, no new,
  missing or dead UUIDs, source unchanged. Main-settings `nativeCampPassages=true`
  matters: earlier tests omitted that flag, unintentionally disabling a setting
  already enabled on the main. Pinned JAR is
  `/tmp/villagers-worker-early-volunteers-pinned.jar`; its unused volunteer code
  is disabled by default, so continuation commands must NOT set the property.
- `worker-main-settings-sustain-20261111`: continued that copied save for another
  2032 ticks (requested12000), gameTime314540, then stopped safely at Iris-240e
  `240e2b57-7cf5-46a4-a9a4-bdd1357cfbc7` 1 HP/0 food near(-406.01,63.5,-840.79).
  All 192 originals alive, no new/missing/dead IDs. Pip-4c3c recovered to12.5 HP/16
  food, Birch-67f3 to14 HP/16 food; Dara-573a remains1 HP/12 food and Oak3 HP/11 food.
  This proves repeated conserved camp feeding and some actual healing, not
  whole-map sustainability. No armor/Nether/End/dragon progression is proved.

## Iris next action

At the 8000-tick source save Iris is18 HP/1 food with an empty bag. By the later
stop she has nine earned wheat but no meal, home or owned workbench. Frozen
`worker-iris-frozen-20261111` and `worker-iris-route-safety-20261111` passed with
all192 alive and no source changes. Existing public workbench(-379,63,-855) is
within her local table scan. A planner finds native routes to hand-contact stands,
but route safety matters: the normal query enters real water at(-395,62,-845)
above sand (NOT a waterlogged solid bridge). Do not relax the water guard.

The production-path audit verified that expanded128 paths can reach the table
but still cross real water, so the existing dry-path check rejects them. This
is not a waterlogged-solid false positive; do not weaken the physical water guard.

`BreadCrafting.mealPath` now excludes wet nodes during native search for an
owned-grain workbench trip. Existing post-search home/dry checks remain. Normal
native-region searches retain at most2-block drops; expanded retries retain the
existing1-block drop bound. Other worlds retain their prior behavior. New
`NativePathLimits.alsoAvoiding` composes with any outer blocked-node predicate;
two tests verify scope restoration, fall constraints and nested exclusions.
Full Java build/tests PASS: `/tmp/villagers-worker-dry-grain-route-build.log`.

`worker-iris-dry-route-care-20261111` used the healthy8000-tick source, with both
camp meals/passages enabled and no volunteer property. Pinned JAR:
`/tmp/villagers-worker-dry-grain-route-pinned.jar`, SHA256
`b45c369cbf354e9248b3b3623eb82abf17320db882988495124372bcad023d36`.
Terminal after3132 ticks (requested6000), gameTime315640, at Linden-8ea7
`8ea7e1a5-6ea5-43b7-a3dd-fd713a991f38`1 HP/0 food near(-680.5,64,-623.32).
All192 originals alive; no new/missing/dead IDs, source unchanged. Iris was5 HP/3
food near(-370,63,-836), with actual harvest/replant/eat-potato memories, bag one
potato/two wheat/one wheat seed. Do not claim bread crafting without a direct
receipt: those crafting counters are not in this probe's regular progress rows.

Next: inspect Linden's real body, possessions and food-access route in this
stopped copy, then rehearse from the earlier healthier8000-tick source. Keep
main camp settings enabled and all conservation/health guards. No operator
coordinates should be taught, no saved health/food reset, no copied save promotion.

`runs/worker-store-vision-care-20261111`, session 84278 TERMINAL, requested 8000 ticks,
port 25615. Source is the frozen fresh main-derived cb79 adoption copy below.
Pinned JAR `/tmp/villagers-worker-store-vision-pinned.jar`.
Log `/tmp/villagers-worker-store-vision-care.log`.
Stopped after 1299 ticks at Oak-6432 1 HP/0 food, gameTime 305807. All 192 current
originals remain alive, no new/missing/dead IDs. Pip and Dara both received and
ate actual food, ending 1 HP/2 food each in the last progress snapshot. The
restored scan improved the first-delivery sequence but did not prove whole-camp
survival. No live process from this line of testing remains; do not promote it.

`runs/worker-local-helper-frozen-20261111` is a completed frozen audit PASS from
actual main. It adopted existing cb79c23c-9623-45ac-9776-90c0f0a7813e in a COPY,
with identical physical body/stock before and after. All 192 preserved; primary
unchanged at the time of that frozen test. Actual v269 subsequently used the
remaining main worker slot; both recorded additional slots are now occupied.

## Latest verified runtime evidence

- `worker-current-brief-gate-care-20261111`: schema-safe 4000-tick PASS with Rowan
  (8a55...), all 192 alive, source unchanged. Startup confirms actual saved
  Pip 4 HP/0 food and Dara 8 HP/0 food. At the end Pip is 3 HP/4 food and Dara
  1 HP/13 food after actual repeated deliveries; no healing. Brief gate receipts
  confirm lever restoration and closed doors. Pinned JAR SHA256:
  e91abf8861a1265b8f4e266273bd6ae00910e93c9463aa60567fb69773d8f039.
- `worker-current-brief-gate-sustain-20261111`: failed safely after another 436
  ticks when Birch-67f3 reached 1 HP/0 food. All 192 still alive. Rowan did not
  reach that different captive in time. Whole-camp sustained recovery unproved.
- `worker-local-helper-care-20261111`: old local cb79 helper with broad face
  vision failed at 304 ticks / Pip 1 HP/0 food, all 192 alive. It collected seven
  real beetroots from (-616,64,-617) at 304660. The other goal's successful v258
  used earlier vision and collected four potatoes from (-617,64,-603) at 304569.
  This concrete store-choice difference motivated restoring the earlier scan.

The older volunteer hypothesis was tested and removed as documented above.
Retain the worker cap and all native movement/supply/health rules; inspect the
other goal thread's latest code before editing shared navigation/meal files.

## Invalid or unsafe evidence

- `worker-crowd-care-20261111` appeared to pass 2000 ticks but used a schema-9
  JAR against schema-10 minds. Fallback minds reset food. That result and its
  continuation are INVALID. evidence-validity.json records the rejection.
- `worker-meal-routes-care-20261111` genuinely fed both captives for 2000 ticks,
  but the longer `worker-ready-minds-sustain-20261111` exposed a gate-wait hazard:
  copied Dara-f13a was killed by a vindicator after 214 further ticks. That copy
  has 191 living originals and must NEVER be promoted. The gate-wait code was
  removed entirely. Main and source saves remained unchanged.
- `worker-routes-only-care-20261111` failed at 581 ticks / Dara-573a 1 HP/0 food;
  all 192 preserved. The broader search alone was not a reliable rescue.

## Verification and build

Full Java build/tests passed after observer and local-selection changes and
again after restoring store vision. Five Python schema/population tests pass.
The observer CLI rejects recovery without min-health1 before touching a save.
Logs: `/tmp/villagers-worker-final-health-observer-build.log`,
`/tmp/villagers-worker-local-helper-build.log`,
`/tmp/villagers-worker-store-vision-build.log`.

Build with Java selected by tools/java-env.sh and:
`./gradlew --project-cache-dir /tmp/villagers-worker-access-gradle-cache -I /tmp/villagers-worker-access-build.gradle build test`.
Output `/tmp/villagers-worker-access-build/libs/villagers-0.4.1-dev.jar`.
Pin each test JAR separately; other threads mutate shared source/build output.
Earlier detailed experiments are archived in WORKER-SUPPLY-ACCESS-history-20261111.md.
