# Remembered farm travel — native production observed

November7,2026,~10:28UTC. Current source passed198Java+10Python checks. One
backed-up normal20TPS visible play run, no fresh supplies/bed/tool, actor/terrain/
time/weather setters, forced decisions/births, restored cohort, agent/model/hash
job or external timer. Existing user/research clients untouched. This fixes
resource access without claiming a naturally self-sufficient society.

## Native result

The observer requested normal quit at exactly31900→33100 (1200loaded ticks).
Final save census33104 includes four further ordinary shutdown ticks. Handle35613
exited0; worlds saved, Animatica's exact four-worker executor drained/terminated,
no watchdog/persistence failure.4alive/7dead/0missing; no new death or child.
[Console](launch.log), [budget-native snapshot](budget-native.json),
[final census](census.json), [results and current custody](results.json).

Fern travelled toward its own remembered composter(-226,72,752), opened/closed
one existing door, harvested one crop and crafted2bread. Stored notes record
arrival at the farm and two actual wheat-row crafts at(-226,71,744). Native
counters: cropsHarvested1, breadCraftedThisLoad2. Inventory wheat5→0, seeds10→12,
bread0→2; equipped ironhoe wear5→6. These inventories/counters/notes corroborate
5stored wheat+1native wheat harvest funding two3wheat bread crafts, with native
seed loot/replant and one hoe wear. No food was conjured or spawned by the probe.

The pre-existing iron hoe and station were supplied in earlier explicitly labelled
operator probes. Their continued use is **not** tool/station self-provisioning or
an untouched generated-economy result. Actual food cost still12units/parent:
Fern's2bread cannot yet fund3, and it continued preparation rather than acquiring
consent or a child. Nell remains on the canyon ledge with6bread. Other adults
remain unsupplied. No ordinary birth, society sustainability or parental care pass.

Both the crop and bread capability have earlier native evidence; this check adds
ordinary remembered-worksite travel leading to production with no new operator
movement/materials. It does not establish learned planning: the new trip is an
authored goal and receives no elective utility-learning credit.

## Source behavior and bounds

FarmWorksite uses the actor's existing JOB_SITE memory, same dimension, loaded
composter, real hoe and planting stock. It consults no private peer state or
unseen crop inventory. It requires the existing fully fed/healthy/rested/daytime
household gate, permitted home range and real native path. One trip<=400ticks,
physical stall80ticks, backoff400ticks, replans<=once40ticks. Superseded elective
actions receive no learning credit. Survival/trading/player attention interrupt.

WorkNavigation shares<=2planning batches/dimension/second across farm/table work.
Paths are checked<=128nodes and inside the unchanged home range. A table batch
considers<=75physical stand positions and<=3paths. The31block station broad-phase
margin is not wider roaming: actual stand/path/body remain within28, with real
3block eye reach and sight, solid floor/two air cells and visible initial station
discovery. Failed/stalled tables back off200ticks so they cannot monopolize work.
The previously rejected29block station was actually used from a permitted stand.

Wheat collection targets at most the raw inputs for the missing part of one
parent's reserve (<=9wheat before native harvest/batch overshoot). Wheat never
pays reproductive cost. At that target, family search can still harvest payable
roots while retaining seed. Separate100tick root/crop caches avoid per-think
profile toggling/rescanning. Full villagers do not run elective crop harvests
without an actual stocking goal. General break/place permissions stay denied.
These are source work bounds, not a population/throughput/fairness benchmark.

## Observer and verification

`VILLAGERS_PLAY_TICKS=1200 tools/play.sh` enables a single native stop request,
play-only,100..12000ticks;0(default) keeps normal play interactive. It begins when
a player joins the named disposable world, requests quit once and saves native
snapshot via the existing evidence writer. It changes no body/world/tick-rate
state and launches no follow-on job. Actual exposure includes ordinary shutdown
latency; four ticks here. The one15second screenshot was viewed:
[native rendered village](native-village.png). It is not alone production proof.

Initial build65003 and final8731 exited0. Ten added tests cover physical
stall/deadline/backoff/interruption, station/home geometry, raw food/seed target,
and default-off/one-shot/reset/bounds for observation budget.198Java+10Python pass,
zero failures/errors/skips, no compiler warnings. Third-party JOML Unsafe runtime
warnings remain. [Final build](build-final.log).

Final mean native tick3.526ms at20TPS with4survivors is a small-cohort snapshot,
not scale/tail latency or archive capacity evidence. Resourcepack/model errors
remain, no full compatibility pass. Full current-source gameplay includes earlier
successful save/reopen, not a new reopen of this changed worksite state.

Full stopped backup: runs/archives/farm-before-20261107T102746Z/VillageTrial.
Source digests unchanged during run. All sampled wrapper765169/start12609113 and
daemon765273/start12609175 identities are absent; game completed before later
process sampling, so no game PID is invented. Native normal quit/handle0 and
empty live-runtime scan/free lock establish terminal state. Original6exact
UID/start/exe/cwd/cgroup unchanged. No active source/build/game worker or timer.
007 remains released; no PID exception, model grant or new hash follows.

Next: measure actual ripe crop/irrigated soil capacity and fair distribution,
then add conserved production/tool/station/land provision where warranted.
This one harvest does not prove renewable output sufficient for four adults plus
children. Do not leave the cohort running indefinitely while reviewing evidence.
