# Accelerated learning and playable delivery

Checkpoint: November 7, 2026. The paired visible run and playable0.2.0 artifact are available. Dated throughput
and policy-isolation checks are in [accelerated-pair evidence](evidence/2026-11-07/accelerated-pair/README.md).
Long-run learning/survival acceptance is pending. This document describes the
experiment and delivery contract; it does not report an improvement that has not
been measured.

## What is being trained

This mod uses a small contextual action-value learner, not an LLM. Each managed
villager combines authored utility with its own learned values for provided
actions. Current values describe expected immediate outcomes in thresholded
contexts, rather than a language model, a general world model, or an evolved
brain. Navigation recovery, tool permissions, door handling and emergency refuge
priorities are authored capabilities. Their successful execution is not evidence
that the learner invented them.

Learning belongs to an individual UUID in one world. Its disk record contains
identity, temperament, needs, home, action values, experience counts and bounded
history. The current scope is learning during the individual's lifetime. A longer
or faster run does not establish genetic inheritance, new races or consciousness.

The planned per-entity switch has two modes:

- **Adaptive:** load the existing personal weights and continue ordinary outcome
  updates after completed or failed attempts.
- **Frozen:** load those same existing weights and use them in choices, while
  skipping outcome-learning updates. Drives, safety, navigation, actions,
  persistence and ordinary event memories continue normally.

Frozen does not mean zeroed weights, an immobilized NPC, disabled needs, or a
vanilla brain. The comparison tests the benefit of *continued adaptation from a
specific checkpoint* against keeping that checkpoint's policy fixed. A novice
control or vanilla-AI control would answer different questions and must be
labelled separately.

## Fix the movement bottleneck before comparing policies

The current source can repeatedly request a path marked reachable while the NPC
fails to advance physically. The refuge layer can retain movement ownership
indefinitely, preventing ordinary choices and their outcomes. More tick updates
cannot teach an agent to escape an authored loop that never returns control.

The small recovery target is a bounded progress check keyed to the actual refuge
goal: remember progress toward the route or a physically reached waypoint, detect
a sustained stall, stop the failed route, record the failure, and temporarily
cool down that target. Try one bounded local recovery or alternative permitted
goal. Preserve personal home ownership and do not teleport, break blocks, grant
gear or call the recovery learned behavior. A resident safely resting or standing
near its guardian must not be classified as stalled merely because it is still.

Ordinary resting and safety-driven shelter must share the same grounded bed
interaction checks. Euclidean proximity through a wall must not authorize sleep
or count as entering a generated house. Root fixes to these mechanics must be
present in both compared clients before the checkpoint fork.

## Responsibilities and independent copies

The root controller owns the adaptive/frozen switch and identical gameplay fixes.
`TrainingTrial` is the intended bounded experiment controller: it applies the
requested tick rate, observes elapsed world ticks, records the end condition,
and restores the normal rate. Runtime preparation owns the two independent,
visible Minecraft clients and the saved checkpoint fork. Evidence collection
records both roles and the exact source/artifact hashes used.

Start from one complete, gracefully saved generated-village checkpoint. Copy its
world, entity data, minds and relevant configuration into two distinct runtime
directories. Initial UUIDs may match between these two copies because they are
forks of the same snapshot; their files and subsequent histories must be fully
independent. Capture initial checksums and the original-cohort roster before
resuming either copy. Retain the source checkpoint unchanged.

Never point two clients at the same save, `session.lock`, mutable configuration,
agent directory, evidence directory or pending command queue. Do not symlink the
two worlds or minds together. Shared read-only game libraries/assets can reduce
disk usage, but every simulation's writable state stays in its own directory.
Two windows looking at a shared server are one simulation, not this paired trial.

Each role needs explicit identity in the window title, runtime path and run
metadata. The adaptive client enables learning for the original managed cohort;
the frozen client disables only new weight updates for that cohort. Record how
later births or admissions are handled. Neither side receives free food, tools,
seeds, repaired buildings, replacement residents, or additional golems.

Both clients must remain rendered and observable. Do not run either headless or
hide an overloaded baseline behind a different fixture. Keep observer mode,
location/spawning conditions, loaded region, golem policy and modpack identical.
Player tool/seed handoffs and targeted capability probes are interventions;
retain their records and separate them from natural-supplies survival evidence.

## Bounded acceleration and fair time comparisons

The initial target is **40 requested ticks per second for 48,000 elapsed world
ticks**, equivalent to two ordinary Minecraft day/night lengths, then an automatic
return to **20 requested ticks per second**. Count the interval using elapsed
simulation ticks from the recorded start, not the absolute day number, time-of-day
commands, screenshot number or wall-clock duration. An existing checkpoint may
start partway through a day.

At a fully sustained 40 TPS, 48,000 ticks take approximately 20 wall-clock minutes;
at sustained 20 TPS they take approximately 40 minutes. These are ideal durations,
not observed throughput or a guaranteed speedup. Record actual elapsed ticks,
wall time, requested rate and server tick durations. If a client cannot sustain
the requested rate, report its measured throughput and the reason for any early
stop. Faster rendering or changing the clock alone does not accelerate learning.

The existing cognition schedule runs every ten simulation ticks. At 40 TPS that
means up to four checks per wall-clock second, while its cadence per simulated
time stays the same. Food use, cooldowns, weather, mobs, pathfinding and needs
also progress faster in wall time. Apply the same requested rate and duration to
both copies. Comparing adaptive at 40 TPS with frozen at 20 TPS for the same wall
window would expose the agents to different amounts of world history.

Restore 20 TPS when the bounded interval completes or aborts. Record that reset;
do not leave the playable session accelerated by accident. Preserve interrupted
results rather than extending only the worse-performing side until it looks
similar. A restart must not silently restart the entire training budget.

## Evidence needed from this run

Use matching elapsed-tick checkpoints for the initial state, intermediate state,
end of the 48,000-tick interval and return to normal rate. Keep wall timestamps too.

| Measurement | What it establishes |
| --- | --- |
| Original UUIDs alive, confirmed dead/converted, and unloaded/unknown | Original-cohort continuity; missing camera entities are not automatically deaths |
| Frozen values, visits and learning-update count before/after | The comparison actually held learned policy state fixed |
| Adaptive value changes and later choices under comparable observations | Continued updates affected policy, beyond counters or labels |
| Completed actions, failures, refuge stalls and progress | Whether agents received useful action outcomes or remained blocked |
| Food/tool/material possession and available opportunities | Whether harvesting or combat could occur at all |
| Door operations, successful harvest/replant, sword hits and applicable wear | Grounded capability outcomes; attempted commands or rejection messages are insufficient |
| Health, shelter use, damage and guardian presence | Concrete survival conditions, with authored safety separated from learning |
| Requested/observed tick rate, server tick duration, save growth and rendered client state | Achieved acceleration and its practical cost |

A naturally unequipped villager has no permitted hoe-based harvest or sword
strike. Zero successful farming/combat in that condition does not show that the
learning algorithm failed to acquire those skills; it may have had no opportunity
to exercise them. Conversely, survival without those opportunities cannot prove
successful farming or combat learning. Record opportunity counts and the exact
action/outcome definitions used.

A single fork of roughly eleven original villagers supplies an engineering smoke
test. It does not establish statistical superiority, robust long-term survival,
or many-agent capacity. The two histories can diverge through action timing,
randomness and environmental interactions after the identical checkpoint. Report
absolute outcomes and residual differences, retain a worse result, and repeat
paired checkpoints/seeds before making a general improvement claim.

The prior flat-lab deaths and the generated-village smoke results remain in the
[evidence directory](evidence/). A new paired trial adds evidence; it does not
replace earlier failures with a cleaner story.

## Playable delivery

Deliver a versioned Fabric mod JAR with its SHA-256, source revision, pinned
Minecraft/Fabric/API requirements, pack version and concise installation notes.
The research pack and development automation are separate from the mod JAR. Do
not distribute user saves, account/session data, a forced training launcher, or a
claim that a JAR contains universally trained villagers.

A player-ready slice should include the supported opt-in adoption/release flow,
normal 20 TPS, visible status, real handoffs, explicit capability restrictions,
and the actual tested scope. Tool probes and generated-trial commands stay
restricted to disposable development fixtures. State any functionality still
pending in the release notes instead of treating build success as gameplay
acceptance. Existing test scripts remain supplemental to the rendered checks.

To continue the trained population, preserve or deliberately copy its complete
world checkpoint and world-scoped identity directory. Copying only the JAR does
not transfer per-NPC learning. Do not merge complete `mind.json` files into a
different UUID or world: identity, household, needs, history and weights belong to
the source agent. A future transferable skill preset needs an explicit schema,
matching action/context definitions, bounded value validation and a policy for
which fields may transfer while retaining the recipient's identity. No automatic
cross-identity training export is claimed here.


## Current launch and controls

Preparation and source checksum are captured in the paired evidence manifest.
`runs/adaptive` and `runs/frozen` currently run independent visible clients. Each
started40TPS and reached a measured80TPS after the pacing gates passed. Frozen
weights/visits/update counters match the exact source checkpoint; adaptive updates
grew. The maximum accelerated window is48000 additional simulation ticks before
automatic20TPS return. This is throughput/control verification, not a survival
benefit claim. Adaptive learning already runs during20TPS loading warmup; its
updates must not be attributed exclusively to the subsequent accelerated window.

To interact with the newest training minds now, type `/villagers play` in the
**adaptive** window. It stops that accelerated run, retains the live world/minds,
and returns20TPS. `tools/play.sh` opens a separate normal-speed copy from the
starting checkpoint, not an automatic merge of trained minds. The installable JAR,
checksum and quickstart are in [releases/0.2.0](../releases/0.2.0/README.md).

## Restart state in current source

The world now owns `villagers/training-checkpoint.json`, containing experiment
identity, role/learner, original start time, consumed game ticks, latest world
clock, stage, and overload-backoff state. Graceful shutdown records a suspended
run and drains writes; reopening resumes only its remaining 48,000-tick budget.
A restored run starts at the configured staging rate (or 20 TPS after backoff),
with fresh session-local throughput measurements. Completed, failed, and explicit
`/villagers play` states remain at 20 TPS across restarts. Role/learner mismatch
or invalid/future checkpoint formats refuse automatic acceleration and preserve
the marker. The play command also works before the natural cohort becomes ready.

Original experiment elapsed ticks are retained when a saved world's clock rolls
back. Checkpoints are updated with evidence audits (every 200 server ticks) and
at transitions. Abrupt process/disk failure can leave the last marker behind the
last observation; exact crash-boundary exposure is not claimed. A clean shutdown
writes the final state after the evidence queue drains. Do not treat recovery
from a crash as an exact matched experimental endpoint.

Historical worlds containing training evidence but no checkpoint are labelled
`legacy_untracked` and stay at 20 TPS. Their old completed interval cannot be
reliably reconstructed from a restart, so they receive no automatic new budget.
Start a new bounded experiment from a fresh, independent saved checkpoint world
and capture the new baseline/manifest. Preserve earlier evidence and identity
files; do not delete a live world's marker to manufacture a fresh interval.

Unit regressions cover state round-trip, remaining exposure after shutdown,
clock rollback, terminal-state retention, overload backoff, incompatible roles,
invalid paths/budgets, and corrupt/future-format preservation. A rendered
stop/reopen run on current source is still required for integration acceptance.
