# Existing worker access experiments — 2026-11-11

**Current selection:** the 32-block combined supply/recipient ranking has been
retired after Rowan failed whole-camp coverage. Recruitment again prefers the
closest eligible existing neighbour in the original 16-block local search,
retaining real resource-path qualification, body snapshots and the two-worker
historical cap. A fresh main-derived frozen comparison is being run; no actual
adoption or primary gameplay has occurred in this thread.

**LATEST:** Temporary gate-wait and broader-route experiment code has been
removed from current source after testing. Neither is approved for the primary.
The 2000-tick combined copy fed both captives, but its longer continuation lost
one copied captive to a vindicator during a prolonged gate visit. Route-only did
not reliably rescue Dara. Main v213 is untouched by these experiments. The new
frozen startup mind-loading fix remains in the diagnostic probe.

**INVALID EVIDENCE ALERT:** `/tmp/villagers-native-crowd-walk-v253/villagers-0.4.1-dev.jar`
has AgentMind.SCHEMA_VERSION=9, while the saved source minds are schema 10.
`runs/worker-crowd-care-20261111` appeared to pass 2000 ticks, but rejected the
saved minds and used fallback minds (Dara's food incorrectly started at 16).
Its recovery/survival result is INVALID. `worker-crowd-sustain-20261111` likewise
cannot provide evidence. Never promote either copy. Both processes are terminal.
The native physical handoff receipts alone do not prove conserved recovery.
Check other v253-based runs for the same warning. The audit tool now checks the
actual JAR schema against saved mind files before launch and rejects restoration
warnings after launch. Do not downgrade source mind files to make an old JAR pass.

The goal remains active: a villager must beat Minecraft through conserved actions.
The authoritative world is still stopped at actual v213, gameTime 304508, with all
192 saved humanoids alive. No copied save has been promoted. Pip-4c3c has 4 HP and
no food; Dara-573a has 8 HP and no food. Armor, Nether, End and dragon victory
remain unproved.

Another user goal thread is concurrently developing native navigation in the same
workspace. This work uses port 25615, `runs/worker-*`, separate build output and
this separate handoff. Do not overwrite the other thread's artifacts, processes,
navigation changes or shared HANDOFF.md. Do not advance the primary concurrently.

## Production changes under test

`NativeIllagers.recruitWorker` keeps the two-additional-workers-ever limit and
adopts an existing eligible healthy local body. It evaluates candidates in a
32-block sphere and ranks actual supported native paths to supplies and recipient
contact stands before proximity. Container contents remain unknown until physical
inspection. Temporary navigation flags are restored. The receipt records candidate
path costs and identical physical body snapshots before/after adoption. No bodies,
items, health, movement or base attributes are supplied.

`NativeCampWork` uses `BlockContact.interactionPoint` for seeing and physically
opening actual camp stores. A visible reachable block face can be within three
blocks even when its center is farther away or obscured. The previous center-ray
check incorrectly rejected such legitimate contact. Inventory transfers retain
their existing conservation and capacity checks.

Build: source tools/java-env.sh, villagers_select_java, then
`./gradlew --project-cache-dir /tmp/villagers-worker-access-gradle-cache -I /tmp/villagers-worker-access-build.gradle build test`.
Output JAR: `/tmp/villagers-worker-access-build/libs/villagers-0.4.1-dev.jar`.
The latest complete build/test passed (`/tmp/villagers-worker-mobile-access-build.log`).

## Evidence and limits

- `runs/worker-access-frozen-20261111`: frozen audit passed; adopted existing
  d8b774c3-3628-43ba-8b81-7a721dc8601f in a copy with unchanged body and no supplies.
  All 192 preserved, primary unchanged. Supply-only ranking selected this helper.
- `runs/worker-access-care-20261111`: stopped safely after 306 ticks at Pip 1 HP;
  helper never collected food. A barrel face was reachable while the center test
  rejected it, motivating the contact fix.
- `runs/worker-face-care-20261111`: helper physically collected seven existing
  beetroots (source 7 to 0), but failed to deliver before the safe starvation stop
  after 307 ticks. This proves collection, not rescue.
- `runs/worker-mobile-frozen-20261111`: combined supply/recipient ranking selected
  existing 8a55e4b5-f10a-424f-b63b-e04072952198, with supply cost 2 and recipient cost
  24 native nodes. Frozen audit passed, identical body snapshots, all 192 alive,
  no new or missing IDs, primary unchanged. Pinned production JAR SHA256:
  `7ab3d3668c067719544296ffdfd1ae6a0438111d9b9227e120359c867b7aaeb9`.
- `runs/worker-mobile-care-20261111`: terminal, server exit 0 / tool exit 1.
  Requested 2000 normal-AI ticks; stopped after 582 at Dara 1 HP / 0 food,
  gameTime 305090. All 192 original humanoids remain alive, no new/missing IDs.
  Rowan-8a55 collected three actual apples at 304552 (source 3 to 0), then handed
  one to Pip at 304652, actual hand reach 2.2813. Pip ate it and was 3 HP / 4 food
  in the last progress snapshot. Rowan retained one apple after its own normal
  meal and walked toward Dara, but did not deliver. At tick 560 it was near
  (-624.53,64.54,-610.81); its latest gate route had 33 nodes ending at
  (-635,65,-612). These diagnostic coordinates must not be taught to agents.
  `/tmp/villagers-worker-mobile-care.log` and the runtime timeline preserve the
  full trace. This proves faster Pip feeding, not full recovery or a safe primary
  continuation. Primary is unchanged.

The primary's remaining worker slot has not been used by these copied experiments.
Planned paths are not evidence of physical travel, delivery, eating or survival.

## Schema-safe rerun and next action

`runs/worker-schema-safe-care-20261111` reran the current combined source with an
isolated, freshly built schema-10 JAR. Its schema receipt verifies 59 source minds
(45 schema 9, 14 schema 10); no incompatible-mind restoration warning appeared.
The run stopped at Dara 1 HP / 0 food after 582 ticks, gameTime 305090. Pip was
3 HP / 4 food at the last progress snapshot and received an actual apple at
304657. Rowan retained one apple and nine wheat but was finishing the gate stand
when the stop fired. Thus the schema-safe result still DOES NOT prove Dara rescue.
This replaces the apparent 2000-tick pass from the incompatible v253 JAR.

`tools/mind_schema_audit.py` now verifies the actual artifact constant via javap
against every saved `villagers/*/mind.json`. The copy audit preflights before
creating a runtime, rechecks the exact copied artifact/world, records a schema
receipt, and rejects unsupported-schema restoration warnings before any pass.
Two new Python tests plus three population tests pass; full current Java build
and tests passed in `/tmp/villagers-worker-schema-safe-build.log`. An actual
negative CLI check rejected v253 before creating its requested runtime.

The main-save ownership question is pending with the user because another active
goal thread shares the world. Continue isolated copies while that is unresolved.

## Gate visit and broader route comparison experiments

Two new behaviors are disabled by default and enabled only with explicit JVM
properties in copied experiments:

- `villagers.camp.gateWait=true`: `NativeCampGateWait` may physically pull a
  reachable own-camp lever when an actual iron door obstructs the ray to an
  already understood caller within eight blocks. It stops walking for at most
  80 ticks, gives only through the existing real contact/inventory checks, and
  physically restores the lever after the handoff or timeout. Maintenance runs
  each actor tick; it never remotely changes a lever outside actual reach.
- `villagers.camp.compareMealRoutes=true`: `NativeMealRoutes` compares at most
  24 observed candidate stands using complete native paths, retaining existing
  body, dry-floor, drop, border and camp constraints. This tests whether the
  normal small rotating candidate batch misses a much shorter real route.

`runs/worker-gate-wait-care-20261111` is terminal: stopped after 588 ticks at
Dara 1 HP / 0 food, all 192 alive. The helper had not reached a lever; its
gate-wait receipt remained empty. No claim that gate waiting helps is supported.
The trace showed another long detour, motivating the broader path comparison.
Build/test passed in `/tmp/villagers-worker-gate-wait-build.log`.

`runs/worker-meal-routes-care-20261111` tests both properties from the original
frozen Rowan-adoption copy, using pinned
`/tmp/villagers-worker-meal-routes-pinned.jar`, schema 11 (accepts the unchanged
schema-9/10 source minds). Build/test passed in
`/tmp/villagers-worker-meal-routes-build.log`. Process outcome must be inspected;
log `/tmp/villagers-worker-meal-routes-care.log`. Do not deploy either property
to the primary without actual rescue, survival and lever-restoration evidence.

Outcome: **schema-safe 2000-tick PASS**, gameTime 306508, all 192 originals alive,
no new/missing/dead IDs, source unchanged. Pinned JAR SHA256
`c75ded9a9f0335f97ad24f852cd7c9c14a9436d3b5982956ea069c343731393c`.
Initial tick-40 snapshot verifies Pip 4 HP/0 food and Dara 8 HP/0 food, so this
run did not reset their saved hunger. Rowan gave Pip an actual apple at 304656
and Dara an actual apple at 305076, hand reach 2.8095, source 1 to 0 / receiver
0 to 1. Dara's receipt confirms lever restored and gates closed. At 2000 ticks,
Pip is 3 HP/4 food and Dara is 1 HP/4 food: both actually ate and survived, neither
has healed. A bread handoff to Oak occurred at 305486. Gate waiting was first
observed after the first Dara apple, so that rescue supports the broader native
path search; it does not establish that the new gate-wait behavior was needed.

`runs/worker-meal-routes-sustain-20261111` continues that saved copied world for
6000 requested ticks with the same pinned JAR and both properties. Session 12802,
log `/tmp/villagers-worker-meal-routes-sustain.log`; inspect authoritative process
and terminal output before restarting or claiming long survival. The two small
initial meals alone are insufficient proof of a sustainable food system.

### Terminal continuation results; experimental source removed

- `worker-meal-routes-sustain-20261111`: terminal after one tick, false observer
  health stop because Dara's session was not loaded yet. Her saved metabolism
  was correctly 4 food. `NativeIllagerHabitatProbe` now restores all managed
  resident sessions while frozen and writes `native-resident-startup.json`
  before unfreezing. No health/food/clock setters are involved.
- `worker-ready-minds-sustain-20261111`: terminal after 214 ticks. Copied
  Dara-f13a (`f13adb74-0ccc-4105-af94-63bc300bc5c3`, a different resident from
  Dara-573a) was killed by a vindicator near the gate. Population preservation
  correctly FAILED: 191 alive, 1 newly dead, all UUIDs retained. This copy must
  never be promoted or used as successful survival evidence. The 80-tick open
  gate visit permits hostile contact and has been removed entirely from source.
- `worker-routes-only-care-20261111`: terminal, 581 ticks, Dara-573a 1 HP/0 food;
  Pip 3 HP/4 food, all 192 alive. Thus broader route comparison alone was not a
  reliable rescue. That optional utility and its hooks were also removed.

The surviving production edits from this line of investigation are worker
selection/contact fixes, the artifact/schema check, and frozen diagnostic mind
restoration. Do not re-enable gate-wait properties from historical command lines.
The other goal thread is actively changing native crowd walking and food supply
logic; inspect its current source before continuing navigation work. The current
cleanup build log is `/tmp/villagers-worker-gate-experiment-cleanup-build.log`.

Next work must resolve Dara's gate approach and prove actual eating before any
primary continuation. Check the other active goal's latest navigation changes
before editing those same files. All tests owned by this handoff are terminal;
there is no live worker-copy server on port 25615.
