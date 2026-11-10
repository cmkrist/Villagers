# Visible Minecraft testing

The project, isolated game directory, villager files, logs, and evidence belong at
`/home/niko/.internal/source/Villagers` on the internal drive. The existing Modrinth
client and its **I Will Survive** world are user data; use a separate development
client and laboratory world.

## Pinned pack

The installed pack is [Vanilla Perfected 1.0.0+26.3, Wilderness Bound Update](https://modrinth.com/modpack/vanilla-perfected/version/IFYAmjXO),
Modrinth project `1ocGzRHv`, version `IFYAmjXO`. Its official `modrinth.index.json`
specifies Minecraft **26.3** and Fabric Loader **0.19.5**. Fabric API is
**0.162.0+26.3**. `tools/pack-lock.json` records the official archive hash and every
pack mod's SHA-512, extracted from that published archive on November 6, 2026.

The installed profile has 80 top-level mod jars: 78 official pack jars plus
`polymer-bundled` and `waylandcraft`, which were added separately. Preparation
copies **77 verified official mod jars**. It excludes the two additions and
`fabric-api`, which Gradle already supplies. Nested dependency jars make Fabric's
reported loaded mod count higher than the top-level file count.

## Prepare and launch

```bash
cd /home/niko/.internal/source/Villagers
python3 tools/prepare-client.py --dry-run
python3 tools/prepare-client.py
./tools/test.sh
./tools/run-client.sh
```

The preparation command reads the installed profile at
`~/.local/share/ModrinthApp/profiles/Vanilla Perfected`, validates official mod
hashes, and copies mods, configuration, resourcepacks, and defaultconfigs into
`run/`. It does not copy saves, launcher accounts or sessions, options.txt,
servers.dat, screenshots, shaders, or launcher state. Config files with potentially
private names and generated logs are excluded. The runtime records copied file
hashes and all exclusions in `run/pack-provenance.json`. Installed profile files
are never changed. Source assets must be regular files; destination symlinks are
refused.

Preparation is idempotent when files match. If Minecraft changes a managed
configuration, another preparation refuses to replace it. Continue launching
without preparing again to retain those settings. To deliberately reset managed
files from the source, use `python3 tools/prepare-client.py --refresh`; previous
contents are preserved under `run/.pack-backups/`. Even `--refresh` refuses to
replace untracked conflicts. `--source /path/to/profile` selects another installed
copy of the same pinned pack.

Both shell scripts select JDK 25 through `tools/java-env.sh`: `VILLAGERS_JAVA_HOME`
if set, otherwise the first of `JAVA_HOME`, the `java` on `PATH`, and
`/home/niko/.internal/minecraft-tools/jdk-25.0.5+8` that really is Java 25. The
newer system Java is skipped automatically.
`run-client.sh` requires a visible desktop session and launches Gradle's Fabric
development client. Unit tests are supplemental: they cannot establish in-game
survival, pathfinding, or pack compatibility.

## First visible acceptance session

1. Confirm the separate window opens, the title/menu renders, and the log contains
   Minecraft 26.3, Fabric Loader 0.19.5, and the Villagers mod. Check for Mixin or
   dependency errors before interpreting any AI result.
2. The development launcher enables `-Dvillagers.lab=true`. Its client harness
   creates or resumes `saves/VillageTrial` inside the selected runtime: a normal
   world with seed `846206`, structures enabled, and normal difficulty. It locates
   a generated village and moves only the creative observer. It does not construct
   houses, spawn residents/golems, or supply food/tools.
3. Observe movement, access to shelter, food use, and claimed homes. Record
   screenshots from the actual rendered game, plus seed and cohort size. Confirm
   the window title identifies **VillageTrial** and the intended runtime role.
4. Observe a complete day/night cycle at normal tick rate. Introduce one
   controlled threat within a contained arena. Record detection, escape choices,
   damage, survival, and whether villagers return to ordinary activity afterward.
5. Repeat a harmless choice with different outcomes. Inspect the learner's saved
   action values before and after; distinguish updated values from a mere scripted
   reaction. Restart the world and verify the same UUIDs retain their own history.
6. Compare learner-enabled and fixed-policy cohorts under the same seed and
   resources. Record survival, needs, action diversity, and observed navigation
   failures. Do not call survival improved until repeated trials support it.
7. Increase populations through 16, 64, 128, and 256 **loaded** villagers only as
   smaller runs succeed. Record client FPS, server tick time, decision count, and
   AI time. Distinguish measured results from target budgets and unloaded agents.

Save dated evidence summaries and exact conditions under `docs/evidence/`; keep
generated worlds, screenshots, and runtime logs under the ignored `run/`. A
title-screen launch passes startup compatibility only. A screenshot cannot
establish learning, survival improvement, or scaling by itself.

## Lab commands and evidence

The development client consumes a small file queue inside its selected runtime. Use the
helper to publish complete batches atomically; it refuses an existing pending
batch and never replaces it. Each argument is one complete quoted command, or
pipe newline-separated commands on stdin. Commands execute asynchronously only
after the disposable laboratory world is ready.

```bash
python3 tools/lab-command.py --run-dir "$PWD/runs/play" 'villagers villageaudit'
python3 tools/lab-command.py --run-dir "$PWD/runs/play" 'screenshot baseline'
python3 tools/lab-command.py --run-dir "$PWD/runs/play" 'tick query'
```

Run each batch after the previous one has been consumed. Submit screenshots after
the preceding change has rendered. Keep interventions in the separate play copy.
Legacy `villagers danger` and `villagers calm` commands belong to the flat fixture;
they are not natural-village or untouched comparison tests.
`villagers status`, `villagers adopt`, and `villagers release` target the nearest
villager within 24 blocks. Adoption enables the prototype; release restores its
vanilla AI while retaining saved learning. `villagers lab` resumes an existing
cohort, and does not intentionally reset its identities. Time, weather,
creative/spectator observer mode, camera teleport, screenshots, and graceful quit
are also accepted by the client harness. Other command families are rejected.

Evidence paths are relative to each selected runtime:

- `evidence/<role-session>/*.png`: actual rendered game captures; matching named JSON files
  record camera and client-tracked villagers at capture time.
- `evidence/<role-session>/latest.json` and `observations.jsonl`: periodic observations,
  including elapsed time, server tick, client-tracked villagers, health, and
  position. Client tracking counts are not the total server population.
- `evidence/report-<timestamp>.json`: requested general reports with decisions,
  completed skills, safety interrupts, think time, and learned-agent snapshots.
- `evidence/<role-session>/commands.jsonl`: native command results/rejections. Dispatch
  alone does not prove that a command produced its intended in-world result.
- `saves/VillageTrial/villagers/<UUID>/mind.json`: world-scoped persistent
  identities, drives, learned action values, and home records.
- `saves/VillageTrial/villagers/village-evidence/`: original-cohort baseline,
  authoritative server census, lifecycle/death records, and village audits.
- `saves/VillageTrial/villagers/training-evidence/<run-id>/status.json` and
  `ticks.jsonl`: training state, achieved throughput, native tick costs,
  configured maximum, cohort outcomes, and bounded-stop reason.
- `logs/latest.log`: pack loading, command results, errors, and shutdown logs.

The harness automatically captures after roughly 15 seconds and 60 seconds, then
once per minute, and records observations about every five seconds while the world
is active. An explicit `screenshot label` uses a filename-safe label and can be
skipped while another capture is pending or a GUI overlay is open. Check that its
PNG exists before claiming evidence. Use a new label for each capture to preserve
the earlier file.

For persistence testing, request a report and screenshot, wait for those files,
then queue a graceful client shutdown:

```bash
python3 tools/lab-command.py --run-dir "$PWD/runs/play" 'quit'
```

After the process exits and world saving completes, preserve the report and mind
files, restart the same selected runtime, and compare the same UUIDs and saved
learning. No pending batch is overwritten; do not leave a `quit` command queued
for a future session accidentally. The command channel is a local development
fixture, enabled by the launcher, rather than an interface for a user's world.

## Desktop tooling

The current desktop is COSMIC Wayland. The existing Minecraft client uses native
Wayland, so X11 automation cannot reliably drive it. `grim` can capture the desktop;
Minecraft's own screenshot function is preferable for game-only evidence. The
desktop has `/dev/uinput` access for input tooling, and Screenshot/RemoteDesktop
portals are available. Restrict automation to the separate development client.

## Movement regression checks

`villagers repairlab` clears legacy cabin aisles/headroom only in the disposable
lab, preserving beds, identities, resources and villager positions. It also runs
when an existing lab resumes. New cabins have their floor at y=-61, level with
the courtyard, with feet-level space at y=-60. The old y=-60 cabin floor raised
villagers into the doorway lintel and trapped them at z≈-6.48.

`villagers openlab` opens a five-block gate in the courtyard boundary. This changes
the fixture so it no longer isolates villagers from the surrounding flat world.
It does not teleport villagers. `villagers report` now includes positions,
actions, sleeping state, navigation completion, target and path reachability.

After a repair, observe actual crossings from inside the cabin (z≤-6) into the
courtyard (z>-5), plus consumption of existing food. Labels alone cannot pass this
check. Test a healthy hungry villager with no known food separately: daytime
exploration must remain available and receive food-search urgency. Physical
accessibility and the action mask caused separate observed failures.

## Generated village is the default trial

`./tools/run-client.sh` now creates/resumes **VillageTrial** in a normal world,
seed846206, with structures enabled. `VILLAGERS_TRIAL=lab ./tools/run-client.sh`
selects the preserved flat **VillagerLab**. Only the development observer moves
when a village is located; residents, golems, homes, inventories and terrain are
not spawned, repaired or relocated by the village setup.

`villagers villageaudit` writes ground-truth census and lifecycle evidence under
`run/saves/VillageTrial/villagers/village-evidence/`. The durable marker is
`run/saves/VillageTrial/villagers/village-trial.json`. Baseline admission freezes
original UUIDs; restarting cannot refill dead/missing residents. Existing natural
golems keep vanilla AI. The observer uses creative flight so ordinary hostile
spawning remains eligible (the normal player-distance exclusion still applies).
Only the observer has creative abilities; villagers keep ordinary damage/death.

Sneak-right-click a managed villager with a real sword or hoe to transfer one
item; the donor's item is consumed. Food and planting items can also be handed
over one at a time. Swords require actual hostile aggression, native melee reach,
sight, cooldown and durability. Harvesting requires a permitted managed harvester,
a mature wheat/carrot/potato/beetroot crop on farmland, actual reach/sight and a
matching real seed/root paid from stock or native drops. Empty hands are valid;
an owned hoe is equipped and worn when available. The action replants age0 and
deposits the remaining native loot. General breaking/placement stay disabled.
One last carrot/potato is retained for planting. Wheat can become bread using
the native recipe at a reachable real workbench. `--hand-harvest` on the native
server fixture checks an empty bag with no supplied tree or tool; it must return
surplus produce and a native receipt showing paid replanting and no tool damage.

`villagers testharvest` is an **explicit post-baseline intervention**: only if an
existing resident is within eight blocks of a ripe crop, the development observer
gets one iron hoe and one matching planting item and transfers them through the
same real-item handoff code. Only the observer moves. A single interruptible
forage request can ask the actor to walk into tool reach; survival overrides still
apply. Native harvest/state/material/wear evidence is saved, and the intervention
is marked in the trial marker/lifecycle log. If no eligible crop/resident exists,
no world or inventory changes occur. Do not report such a refusal as a successful
harvest. Keep tool-intervention results separate from the frozen natural-supplies
baseline. A complete saved-world backup before tool testing lives in run/archives/.

## Independent visible training and play

The paired clients use `runs/adaptive` and `runs/frozen`, cloned from the same
explicitly stopped saved-world snapshot. Worlds, minds, configuration, evidence,
queues and build caches are independent; verified immutable pack assets are
shared read-only. `runs/play` is a third independent copy at 20 TPS with adaptive
learning, not an automatic import of the latest trained state.

`tools/prepare-parallel.py --source-snapshot /absolute/path/to/stopped/archive`
requires an archived world directory outside active `saves/`. Inspect its
`--dry-run` first. Preparation refuses live locks and conflicting existing
experiments; repeated preparation preserves ongoing learned state. Do not copy a
live world or reset a comparator to undo deaths.

```bash
VILLAGERS_RUN_DIR="$PWD/runs/adaptive" ./tools/run-client.sh
VILLAGERS_RUN_DIR="$PWD/runs/frozen" ./tools/run-client.sh
./tools/play.sh
```

Launch the comparison clients separately if simultaneous rendered execution is
desired. The launcher infers each prepared role/TPS from its manifest and locks
the game directory. Training accepts bounded 20/40/80 TPS, measures actual wall
throughput and native tick costs, and returns to 20 TPS after its 48,000-additional
game-tick budget or an explicit/overload/failure stop. `villagers play` ends that
session's acceleration; it does not turn a frozen-control process into adaptive
learning. Native `frozen` means simulation freeze; `learning_mode` identifies the
policy control. More simulated ticks and more updates do not establish improvement.

Read-only checkpoint at 2026-11-07 06:01 UTC: both comparison clients had completed
their bounded windows and returned to 20 TPS. Each recorded nine living originals,
two confirmed losses, no missing originals, and one currently loaded natural golem.
Both clients remained open. This is one small same-seed pair, with no observed
survival advantage for the learner. The unlaunched play copy retained its earlier
11-of-11 starting census. Preserve the [paired evidence](evidence/2026-11-07/accelerated-pair/README.md)
and confirm current runtime status before a later intervention.

## Player-impression smoke test

The social update starts unfamiliar players at high awe **0.85**, trust **0.10**,
fear **0.15**, and low confidence **0.05**, then revises those separate estimates.
Use the dedicated play world at 20 TPS for this check;
leave completed comparison worlds and their evidence unchanged. Build/integration
tests and the rendered social test must be reported separately. The complete
contract is [player-impressions.md](design/player-impressions.md).

1. Preserve the play save and schema-1 mind files before first migration. Launch
   the social build, identify the actual player and a healthy managed recipient,
   and inspect `/villagers impression`. Record UUIDs, prior values and confidence.
   File-queue inspection must resolve the actual observer and its position; a
   server-console UUID or spawn-relative target does not test the player's view.
2. Give one existing useful bread/item through successful native handoff. For
   bread, use a recipient with hunger at least 0.2 and no existing allowed food;
   redundant gifts and gifts within the evidence cooldown need not change trust.
   Record player
   inventory before/after, recipient receipt, a successful handoff counter/event,
   and that UUID pair's revised trust/confidence. An attempted gift or rewritten
   belief value is insufficient. Count one successful physical transfer once.
3. In the separate play cohort, use one controlled nonlethal player-attributed
   hit. Record the target, native accepted damage, health loss, player UUID and
   impression before/after. Check that a non-victim receives no harm evidence:
   witnessed harm is not implemented. An attempted swing or cancelled damage
   event must not teach harm.
4. Capture the rendered reaction and matching native event/state evidence.
   Distinguish actual behavioral changes from labels, a brief hurt animation or
   numerical updates alone. Repeat inspection after saving and reopening the
   same player/villager UUIDs.

An explicit development social probe may supply one real bread to the observer,
transfer it through the normal handoff, and cause a bounded native player-source
hit in the dedicated play copy. It is a labelled fixture intervention, not an
untouched survival trial or proof that a person clicked those actions. Save its
actual transfer/damage callbacks and recipient health/inventory deltas; a direct
impression setter cannot pass. Keep victim and control roles explicit, and leave
the training pair untouched. The separate witnessed-protection hook requires an
actual player kill of a monster targeting or recently hurting the observing
villager, within 12 blocks of both actors with sight of both. A gift/direct-hit
probe does not validate protection or imply witnessed-harm learning.

Schema 2 must preserve schema-1 homes, identities, learned values/counters and
history while adding bounded player records. Verify those fields before/after
upgrade and impressions after restart. The older 0.2.0 JAR cannot interpret
schema-2 records: retain the pre-upgrade backup for downgrade instead of silently
discarding social history. A second real player is a later discrimination test;
invented IDs cover serialization/update rules only.

## Evidence backlog and failure behavior (current source)

The village evidence writer accepts at most 128 queued tasks; the rendered
client writer accepts at most 16. Both serialize their IO on a single worker.
Queue saturation, a write failure or an uncaught task exception latches failure
and logs it once. Further submissions are rejected rather than building a larger
backlog or doing disk work on the game/render thread. Already accepted tasks
are drained at shutdown. Rejected screenshot images are closed immediately.

A failed server evidence channel marks the village trial failed, clears its
readiness, releases only its owned nonpersistent loading tickets, and stops
training at 20 TPS. A failed rendered evidence channel changes the window title
to `EVIDENCE FAILED`, stops automatic observation/captures and asks the server to
stop training. The explicit command channel remains available for inspection and
normal shutdown. These outcomes mean evidence is incomplete; a missing event is
not proof that it never happened. The final village marker is committed after
older queued snapshots drain, when disk IO remains possible.

Deterministic unit checks exercise blocked workers, queue bounds, rejection
without caller-thread IO, draining accepted work, sticky disk failure, and
ordered commit barriers. Live failure injection and chunk-ticket cleanup still
need a visible disposable-world integration check; no production/profile world
should be used to manufacture those failures.


## Observer command routing and crafting check

Current LabClient resolves the visible observer UUID to its server player before
executing queued native commands. Position, entity identity and permissions come
from that player, with no server-console fallback. A disconnected observer yields
a recorded rejection. Both helper/client grammars now allow exact `glossary` and
`teach <concept> <2–12 lowercase letters>`; arbitrary execute/give/data roots stay
disallowed. Java and Python grammar regressions cover these boundaries.

In the isolated play role only, `/villagers craftprobe start` records an explicit
three-wheat operator handoff and, if necessary, one crafting-table fixture in air.
It rejects out-of-reach observers before provisioning. `/villagers craftprobe status`
records the same selected resident's actual inventory, food, craft count, station
and nearby crop states. These are operator interventions and cannot establish
natural-supplies survival. The ordinary controller performs the craft; the probe
does not force its action or mutate its hunger. See the positive
[bread-crafting evidence](evidence/2026-11-07/bread-crafting/README.md).

## Resourcepack files and preparation exclusion

Minecraft rejects linked resourcepack paths. Newly prepared parallel runtimes
use verified read-only regular pack files (hardlinks to immutable cached bytes
where possible, copies otherwise); mod JAR links remain supported. Old stopped
runtimes can be inspected/repaired with:

```bash
python3 -B tools/repair-resourcepacks.py --run-dir /absolute/project/runs/play --dry-run
python3 -B tools/repair-resourcepacks.py --run-dir /absolute/project/runs/play
```

Use the actual canonical project path. Repair does not change saves or mods;
checksums/permissions are validated before replacement and the manifest is backed
up. Active default/parallel/repair preparation holds `.villagers-client.lock`,
shared with the launcher, and rejects detected manual clients. Existing running
adaptive/frozen instances must be left alone until their owner naturally closes
them; do not repair live directories. Next launch validation rejects remaining
legacy resourcepack links rather than silently claiming pack parity.

Python-only verification, which starts no Java job:

```bash
python3 -B -m unittest discover -s tools/tests -v
```

The standard test wrapper also runs these checks during later authorized builds.
See the [closed-play repair evidence](evidence/2026-11-07/resourcepack-fix/README.md).


## World-free shutdown diagnostic

`VILLAGERS_TITLE_SHUTDOWN=1 ./tools/play.sh` runs the normal pinned visible client
at its title screen, captures a menu/executor observation, then requests ordinary
quit after100 title ticks. It bypasses automatic world opening and all society
commands. The flag is rejected for adaptive/frozen roles. It does not submit work
to or stop the inspected executor; normal lifecycle compatibility handles its
known shutdown defect separately. Do not confuse a title-only pass with world
save/reopen or gameplay evidence. [Reproduction and fix](evidence/2026-11-07/shutdown-pool/README.md).


## Bounded play observation

Use `VILLAGERS_PLAY_TICKS=1200 ./tools/play.sh` for a bounded60simulated-second
acceptance instead of leaving simulation running while inspecting evidence.
The optional play-only budget is0(default, interactive) or100..12000loaded-world
ticks. It starts on the first server tick in the named disposable world, including
loading before the player joins. At the boundary it freezes simulation and
requests normal quit once, so shutdown latency cannot silently advance a critical
body. The next ordinary launch resumes normal simulation. No actor changes,
external watcher, recurring job or automatic reopen.
The existing writer records `budget-native.json` and `budget-end.json` before
normal drain. Use>=400ticks when the normal15second screenshot is needed.
An optional `VILLAGERS_PLAY_MIN_HEALTH=1` with a positive tick budget stops at
that managed-villager health threshold on the server thread using the same
freeze-and-quit boundary. Default0 disables this guard; valid thresholds are1..20.
Capture exact process tuples early after launch; a short budget may finish before
later inspection. Never infer a missing game PID or reuse an old exception.
[Native1200tick check](evidence/2026-11-07/remembered-farm/README.md).

If a bounded play-copy observation is actually at `PauseScreen`, the explicit
`./tools/lab-command.py --run-dir <canonical-play-dir> resume` command closes that
menu once. It requires play role, a positive play-tick budget, the disposable named
world and no overlay. It rejects unpaused clients, comparison roles and ordinary
interactive play. It never automatically dismisses a user's menu or changes other
clients. Inspect the client's own `latest.json` before using it. The current tool
production reopen completed without needing this command; its positive paused-menu
path remains unverified natively.

## Civilization stability acceptance

These are isolated native server tests. They require a prepared Fabric server
with its owner's already accepted EULA. They never open the primary society or
user saves. Use fresh canonical paths under `runs/` for every invocation.

```bash
source tools/java-env.sh
villagers_select_java
./tools/test.sh --no-daemon --max-workers=2
./gradlew -I tools/probe-classpath.gradle writeProbeClasspath --no-daemon --max-workers=2
python3 tools/freeze-server-artifact.py --server-home /path/to/prepared/server --artifact-runtime runs/stability-artifact-new
python3 tools/tool-renewal-server-test.py --artifact-runtime "$(pwd -P)/runs/stability-artifact-new" --runtime "$(pwd -P)/runs/stability-market-new" --classpath-file build/probe-classpath.txt --input-market --maker-rations 3
python3 tools/tool-renewal-server-test.py --artifact-runtime "$(pwd -P)/runs/stability-artifact-new" --runtime "$(pwd -P)/runs/stability-village-new" --classpath-file build/probe-classpath.txt --civilization
```

The artifact freezer verifies every detached main class against the exact JAR
used by the native server. The runner records artifact hashes, the probe source,
PID/start identity and terminal status for each phase. A loopback-only
`--server-port` permits independently owned fixtures to use different ports.

The market test requires two paid input sales, two manufactured customer tools,
native farming, income-funded meals before emergency hunger, conserved marked
currency/materials and clean restart. `--maker-rations 3` reduces initial food
from the historical four-ration fixture; it does not supply food during play.

`--civilization` runs four founders, two equipped farmers, two dependents, six
beds, forty-eight initially immature mixed crops, common initial language and
four marked bread each. Normal hunger, social behavior, sharing and breeding
remain enabled. It observes at least48,000ticks and at most96,000total world ticks at80TPS,
with no later supplies or resident setters. Both dependents must consume produced
food, a crop must regrow and be harvested again, and every founder/ordinary child
must remain present, alive, unhurt and above zero food. Every saved resident's
item totals/components, nutrition and tool wear are checked at entity load on restart. Native slot compaction is allowed; missing items or changed components are not.

A missing native result or crash is a failure even if Minecraft exits0. These
prepared peaceful checks establish no hostile-village, indefinite tool renewal,
autonomous construction or population-capacity claim. See
[acceptance gates](design/civilization-stability.md) and
[retained results](evidence/2026-11-07/civilization-stability/README.md).

A stopped civilization fixture that exhausted only its observation budget can be
copied with `--civilization-continue-from /canonical/runs/prior-fixture` together
with `--civilization`. The source is locked during copying and preserved. The
runner accepts only the exact observation-exhaustion result, not deaths or other
failures; no residents, resources, terrain or clock are reset. History and minimum
food carry forward, and the96,000total-world-tick ceiling cannot restart. The
continuation and restart have separate reports and process receipts.


For a copied native-island food window, use `tools/food-access-audit.py` with
`--sustain --rehearse`. Default14000game ticks continue past the first harvest;
reports distinguish receipts, repeated cells and newly managed bodies. The optional
`--sustain-ticks 1000..48000` and `--tps 20|40|80` change the observation window
and wall-time simulation rate. All game rules, hunger, growth and movement still
run on every game tick. The20TPS/14000tick and80TPS/48000tick windows have passed. The latter
kept all8 original island residents alive with11harvest receipts, but no repeated
cell yield or birth. A `stop-observation` file in a fresh sustain runtime
requests a normal server stop at its next40tick reporting boundary. Only marked
copies use this probe; never install it in the playable primary.

The frozen `--boats --expand-copy-border` audit and
`--sustain --rehearse --expand-copy-border` windows exercise the production
`NativeExploration.expand` operation in marked copies. They require the saved,
loaded adventurer and an actually owned pickaxe. The frozen audit calls it twice
to check idempotency. The ordinary `villagers native-exploration` dev command is
restricted to the named native world and records the existing→8192 border change;
it never moves actors, supplies items or changes the four-region generator.

Critical saved food rehearsals may use `--all-residents` with the default
`--rehearse` mode to load existing western residents as well as the desert group.
Reports include the actual loaded managed count and real gift receipt; the23body
critical case passed with conserved bread and an eaten meal. An initial1/2HP
starving actor stops on its first actual meal or at the safety timer, rather than
pretending to satisfy the ordinary12food recovery target.

`VILLAGERS_PLAY_STARVATION_GUARD=1` with a positive play-tick budget enables a
server-side last-starvation-interval stop (food0, health<=1, timer>=70). Default0
leaves interactive play unchanged. It freezes before normal quit and never pauses
hunger while actors continue working. Use copied recovery proof before advancing
a critically hungry primary, and distinguish food recovery from complete healing.
