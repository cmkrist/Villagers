# Architecture and roadmap

The implemented prototype and the next integration contracts are distinguished below.
Rendered acceptance of the new generated-village trial remains pending until a
dated evidence report records its outcome. The broader research proposals live in
[Creatures](research/creatures.md) and [modern solutions](research/modern-solutions.md).
Those proposals do not imply that planning, relationships, heredity, or language
already exist.

## Integration and ownership

`VillagerMixin` wraps the call to `Brain.tick` inside
`Villager.customServerAiStep` with MixinExtras' `@WrapOperation`, so it chains with
other mods wrapping the same call. Ordinary villagers retain that call. An adopted
villager carries the persistent `villagers:managed` entity tag and uses
`VillagerController` instead. `/villagers adopt` applies it; `/villagers release`
saves the identity, removes the tag, stops the current activity, and refreshes the
vanilla brain. Adoption is explicit outside the disposable development lab.

The controller owns action selection and issues navigation requests through
Minecraft's existing navigation system. Movement and collision continue through
the entity engine. Trading interaction remains possible and pauses ordinary
actions. The replacement stops vanilla brain-driven profession routines,
restocking, breeding, and brain-driven door interactions. These functions need
deliberate restoration or replacement before adopting a production village;
`AdventureActions` is the integration boundary for explicit, permission-checked
interactions such as doors and held tools. Retaining a navigation object
does not preserve all behavior formerly driven by the brain.

Code layout: `Villagers` is the entrypoint, session registry and public API;
`VillagerController` runs the decision loop and actions; `VillagerCommands` holds
the operator commands; `LabCommands` and the trial/probe classes are development
tools, registered only with `-Dvillagers.lab=true` (see `DevMode`).
`persistence.MindPersistence` reads and writes mind files on a background thread.
A villager whose mind is still loading idles instead of blocking the server tick.
Saves that can never succeed (a protected newer-schema file) are dropped with one
warning. Other failures retry, with errors logged at most once a minute per villager.
Per-villager runtime state is cleared in one place, `Villagers.forgetRuntime`.

Memory has two bounded buffers. `memories` (48) holds routine action outcomes and
notes for learning and debugging, and turns over within minutes. `episodes` (64)
holds memorable events (bed claimed, gift received, hurt or protected by a player),
each with a stable id and the players who share it. Each mind also stores its
`genetics.Genome`, from which traits and tendencies are expressed. Saves are schema 5,
which adds food state, loaded age, lineage and death. Older schemas upgrade in
place, with no episodes, a founder genome derived from the UUID and saved
personality, and a food bar derived from the saved hunger. Settings come from `config/villagers.json`
(`config.VillagersConfig`).

## Adventure capability boundary — next integration contract

[Adventure village design](design/adventure-village.md) defines the next test
target: naturally generated villages, their original residents and shelters, and
their naturally present iron golems. The flat laboratory remains a regression
fixture and its failed survival result remains evidence. A new village trial
must not reset that result or its surviving identities.

The desired restrictions resemble a player's adventure capabilities, but mob
entities do not acquire a player's `GameType`. They need a capability policy
applied to every authored action. The planned `AdventureActions` boundary checks
body capability, actual inventory/equipment, reach, cooldown, target validity,
world policy, and a grounded completion result before reporting success to the
controller. The learner may request only registered actions; a learned preference
does not grant a new permission.

General `BREAK` and `PLACE` permissions remain false. The user's clarified
exception is a precise `HARVEST_REPLANT` capability for mature wheat, carrots,
potatoes and beetroot: require a real hoe, reserve the corresponding real seed or
planting material, use native crop loot and applicable tool durability, and
immediately replant the same crop at age zero while preserving the farmland.
Reject immature crops, missing materials, invalid substrate or failed replant
preconditions without treating the action as a completed harvest. This scoped
operation does not grant general block interaction or crafting permissions.

All other blocks remain protected. A tool being present does not authorize
stripping logs, tilling soil, path conversion, or other transformations. Opening
and closing an existing permitted wooden door is another explicit interaction,
subject to distance, occupancy, progress and obstruction checks. Eating consumes
a real allowed item. Equipment use must respect possession, applicable durability
and cooldown rather than merely displaying a held item or granting a damage
bonus. Exact supported items and interactions must be enumerated in code and
acceptance evidence. `HARVEST_REPLANT` implementation and rendered acceptance
remain pending at this design checkpoint.

Generated baseline villagers receive no free hoes, seeds or other equipment.
A player's handoff must transfer actual items and enter the intervention audit;
a provisioned capability test is reported separately from the untouched natural
survival baseline.

`VillageTrial` is the planned generated-world fixture and audit owner. It records
the seed, pinned pack, world-generation settings, difficulty, admission tick,
original UUID cohort, naturally present golems, existing homes, resources, and
interventions. It may select and observe a generated village and admit residents
for the experiment. It must not build substitute shelters, give food or gear,
spawn replacement residents or guardians, or silently move agents to rescue them.
Its inspection and spectator-camera operations must be distinguishable from
changes to the simulated population or resources.

These class names identify integration responsibilities, not a claim that the
capabilities or trial have passed rendered acceptance. See the design's test
matrix for pending checks.

## Agent type, body, and role

The implemented managed population is villagers. The shared identity foundation
is a world-scoped entity UUID, bounded personal history, and a persistent disk
record. Extending that foundation to other bodies should distinguish species or
body type from role: a villager can be a trader or explorer, while a golem can be
a guardian. Personality is another dimension, not a substitute for either.

An in-game bed remains appropriate for a villager household. A golem's home may
be absent; a future guardian territory or patrol anchor needs its own typed
meaning and must not be validated as a villager bed. Any stored type/territory
addition requires an explicit compatible schema policy. Existing identity files
must not be silently reinterpreted or discarded.

For the first generated-village iteration, iron golems retain their vanilla AI
and serve as existing protection and part of the baseline. The prototype must
not redirect a golem brain or grant it villager eating, sleeping, trading, or tool
capabilities merely because both bodies can have identity records later.

Future guardian agents may share bounded threat observations, coordinate patrols
and rescues, and remember local encounters. Such signals need a source, location,
world tick, confidence, expiry and recipient bound. Receiver decisions must respect
actual reachability and body capabilities. These proposals are not implemented
inheritance, autonomous race evolution, or evidence of emergent cooperation.

## Needs, choice, and outcomes

`AgentMind` stores hunger, fatigue, and loneliness in `[0,1]`, a stable name,
UUID-seeded temperament, a home record, bounded action values, experience counts,
and at most 48 event records. Curiosity and sociability currently influence utility;
the stored boldness trait does not yet define a learned danger response.
Loaded simulated time advances drives. Unloaded villagers do not accumulate an
offline needs debt.

Hunger follows the player's food rules (`mind.Metabolism`, a port of `FoodData`):
a 0–20 food level, saturation spent first, one point per 4.0 exhaustion, and the
player's costs for running (0.1/block), swimming (0.01/block), attacking (0.1),
damage taken (the damage type's exhaustion) and food-powered healing (6.0 per
health point). Walking costs a player nothing, so villagers also burn a configurable
baseline, `baseExhaustionPerDay` (default 80, scaled by appetite traits), about a
full bar per loaded day. At 18+ food a hurt villager regenerates on the player's
timers (`natural_health_regeneration` is respected); at 6 or less it cannot run
from danger. At 0 food it takes 1 starvation damage every 80 ticks. Unlike a
player, this ignores difficulty and Peaceful, so starvation always kills. Eating
uses the item's real nutrition and saturation and is impossible on a full bar.
The observed `hunger` is the empty part of the food bar.

When the last villager within 64 blocks of its home starves (`GodChildren`), the
longest-lived villager that village had, living or dead, by loaded age, receives a
**god child**: a baby carrying that ancestor's genome recombined with itself at 10x
the mutation rate, with a full food bar and a `GOD_CHILD` memory. Only starvation
triggers this, and `godChildren: false` disables it. This deliberately spawns a
resident, so a generated-village trial that forbids replacement residents must
disable it or record it as an intervention. Villagers in unloaded chunks are not
counted as survivors.

The controller staggers checks at approximately **2 Hz** per loaded villager while
the engine continues physical simulation. Current observations contain needs,
health, day/night, detected danger, available food, and home validity. Local entity
queries and navigation still have costs; staggering is a design choice rather
than a load benchmark.

Five actions exist: `FORAGE`, `REST`, `SOCIALIZE`, `EXPLORE`, and `FLEE`. Masks
exclude unavailable food actions and restrict social/exploration choices at night,
low health, or (for socializing) extreme hunger. Daytime exploration remains available to a healthy hungry villager, and lack of known food adds search urgency; hunger must not force a foodless agent to rest forever. Exploration samples only permitted actions. A
detected threat selects flight regardless of learned preferences. Fire also
interrupts through the controller's shield. Detection shares the controller's
2 Hz checks; it is not a guaranteed instantaneous response or complete hazard
avoidance system.

The personal learner is a small contextual action-value estimator: 32 contexts
encode thresholds for hunger, fatigue, loneliness, health, and night. It combines
authored utility with learned values and uses 4% exploration among allowed
actions. An outcome update uses a constant step size of 0.25 toward a bounded
reward. This estimates outcomes of completed or timed-out actions, with no
long-horizon value bootstrap, learned world model, or invented skills.

Rewards use the relevant action's drive relief and health losses. Eating cannot
reward an unrelated social action merely because hunger fell. The controller
submits one update per completed or failed attempt, rather than reinforcing every
tick of a pending action. Interrupted flight is handled separately and is not
learned. These credit-assignment rules reduce obvious mistakes; they do not prove
causal attribution under every simultaneous world event.

The environment grounding remains authored. Eating consumes actual bread,
carrots, potatoes, or beetroot from inventory or reachable item entities. Resting
reduces fatigue after a dwell interval, and nearby social contact reduces
loneliness after another interval. Exploration uses vanilla random navigation.
Actions time out after roughly eight simulated seconds. This is learning between
provided capabilities and reward definitions, rather than discovering physiology
or social meaning from scratch. The proposed narrow harvest/replant capability
has not yet established a renewable food economy or a self-sufficiency loop.

## Two kinds of home

The in-game home is a dimension and bed position. The controller reuses a valid
vanilla home or looks for an available nearby bed through the POI manager, records
it, and rechecks it when loaded. This supports bed-based households as an initial
building block. It does not yet represent property boundaries, family membership,
house construction, furnishing, or shelter quality.

The disk home belongs to both the world and entity UUID:

```text
<world>/villagers/<UUID>/mind.json
```

For the development lab this is
`run/saves/VillagerLab/villagers/<UUID>/mind.json`. Display names never determine
paths. Copying a UUID to another world does not share its learned file, although
UUID-seeded initial traits can match. A folder is a persisted record, not an
operating-system process or unrestricted filesystem account.

`AgentStore` uses versioned snapshots and validates bounded finite state.
Snapshots are detached from live mutable arrays. Save requests coalesce to one
pending snapshot per UUID; a worker flushes them, and shutdown requests a final
flush. Admission can still read a saved mind synchronously. Unload saves state;
the retained disk record alone does not keep an unloaded entity thinking or revive
a dead entity.

Atomic replacement is attempted for file updates. Malformed records are
quarantined before a fresh seeded state is used; recovery preserves the damaged
evidence but cannot reconstruct lost learning. Unsupported future schemas are
protected from overwrite. Automatic schema migration, lineage transfer, and
complete death/world-copy policies remain future work.

## Evidence and test boundaries

Twenty-one pure-Java tests cover useful learned preference changes against a frozen
control, contextual separation, action masks, unrelated relief, bounds, detached
snapshots, reload continuity, world isolation, corruption, unsupported schemas,
and write failures. They do not exercise Minecraft movement, shelter navigation,
the full modpack, or population survival. The recorded longer flat-lab trial
confirmed five deaths among the original eight villagers; three survived the
reported interval. [That failed result](evidence/2026-11-06/survival-trial/README.md)
remains authoritative for its conditions.

`LabClient` supplies the separate rendered integration fixture behind
`-Dvillagers.lab=true`. It creates/resumes the named flat lab world, provides eight
villagers and finite resources, captures rendered frames, and records
observations. The local command file accepts a restricted command set only in
that fixture. Client-tracked entity counts are distinct from server population.
Follow [testing.md](testing.md) for the existing fixture. The new naturally
generated trial and its audit are specified in [adventure-village.md](design/adventure-village.md);
its acceptance status is pending until rendered evidence exists. Record actual
outcomes before making survival or capacity claims.

## Research-informed next steps

1. **Establish continuity and survival.** Repeat visible save/restart and threat
   experiments, inspect action traces and failure causes, and compare vanilla and
   frozen-learning cohorts under matching resources. Restore missing village
   functions deliberately. Add food acquisition/production before treating hunger
   as a lethal need.
2. **Improve grounded adaptation.** Add typed targets, progress/failure evidence,
   remembered resource locations, and short interruptible plans. Test changed
   rewards and blocked routes to establish useful relearning instead of permanent
   habits. Measure retention across interference and reloads.
3. **Measure population cost.** Profile loaded cohorts, full server tick time,
   navigation/query costs, decision age, memory, and save growth in the visible
   pinned pack. Set budgets from measurements. Current bounds and staggering do
   not establish that hundreds of villagers run acceptably.
4. **Add individual relationships.** Ground partner-specific trust and reactions
   in observed help/harm and bounded memory. Test matching encounters with
   different individuals and remove relationship memory as a control. Labels and
   proximity-based social relief alone do not establish relationships.
5. **Introduce constrained evolution.** Define inherited bounded traits, parentage,
   mutation, and viability checks separately from acquired action values. Lifetime
   weights should not silently become genetic inheritance. Compare multiple
   populations/generations with no-mutation and no-selection controls while
   preserving identity and lineage records. Survival, competence, and behavioral
   diversity all matter; a changed trait is not evidence of adaptation by itself.
6. **Add optional language last.** Express grounded memories and proposed typed
   goals through a budgeted slow layer. Any LLM component must remain outside the
   tick loop and its absence must leave survival behavior functional. There is no
   LLM dependency now.

The intended experience is recognizable individuals with history-dependent
behavior. Personification is a design goal; these mechanisms and their test
results make no claim about consciousness or personhood.


## Generated-village iteration integration status

`VillageTrial` now freezes natural resident UUIDs, village structure/resources,
and actual native golems before opt-in adoption. `LabClient` defaults to normal
world generation; the flat lab remains selectable. A creative-flight development
observer preserves ordinary hostile-spawning eligibility, including the usual
player-distance exclusion. Bodies receive no creative abilities.

`AdventureActions` implements wooden door use, real sword/hoe handoffs, checked
native defensive melee and the narrow mature-crop/replant exception. It records
physical event counters. `VillageSurvival` chooses covered reachable homes at
dusk, bounded home return, nearby vanilla golem protection, or checked escape
paths. Proximity alone cannot count as inside a house: roof and an unobstructed
bed interaction are required; a removed home invalidates cached plans. A homeless
actor can seek a reachable golem at night. These are authored capabilities,
separate from the contextual learner.

The build passes21 tests. Positive live door events have been observed in the
generated village. Actual farming/defense and multi-night survival require their
own recorded checks. A refused harvest probe with no eligible mature crop is an
inconclusive positive test and a successful no-mutation guard check. Crops/gear are
never silently supplied to the natural baseline.
