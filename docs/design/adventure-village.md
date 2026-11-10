# Naturally generated adventure village

Design checkpoint: November 7, 2026. Status: target requirements and acceptance
plan; generated-village and tool-use acceptance are **pending** until a dated
evidence report records the rendered run and audit. Existing evidence remains linked below.

The next experiment asks whether the managed villagers can use an ordinary
Minecraft village's existing shelter, resources, and defenders while retaining
individual histories. They should gain grounded player-like interactions with
real tools. General block breaking and placement remain denied, with the user's
explicit exception for ripe crop harvesting followed immediately by replanting.
This is a capability policy for mobs, not Minecraft's player Adventure `GameType`.

## Preserve the experiment's starting conditions

Use the project's isolated internal-drive runtime with the pinned Minecraft 26.3,
Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, and Vanilla Perfected 1.0.0+26.3
versions. Record the exact seed and normal generation settings with structures
enabled. The run uses a distinct disposable save; it does not convert the flat
VillagerLab into a natural village or modify the user's player world.

Select a village that the pinned generator actually produces. A deterministic
structure search and spectator camera movement may locate it. Record the search
rule and location so the test can be repeated; do not cherry-pick an unusually
safe village after observing outcomes. Normal terrain, generated structures and
ordinary spawning are part of the environment. Capture the loaded initial state
before adoption or other agent interventions.

The trial must use naturally present residents, existing houses, beds, doors and
iron golems. Do not summon a fixed population, give food, tools or seeds, repair houses,
place barriers or lighting, add guardians, make agents invulnerable, teleport
agents out of danger, or respawn the dead. Original inventory and chest contents
remain what generation and ordinary simulation supplied. Trial observation must
not become an invisible resource source.

At admission, audit the original villagers and naturally present golems by UUID.
Continue reporting those originals even when entities die, unload, or leave the
camera's tracking range. Record later natural births or spawns separately; they
do not replace original-cohort losses. A persistent mind file is history, not
proof its entity remains alive.

The earlier flat-lab experiment remains a failed survival trial: five confirmed
deaths among eight originals, with three remaining at its recorded checkpoint.
Preserve that save, its identity files, logs, and
[failed evidence](../evidence/2026-11-06/survival-trial/README.md). A fresh village
changes the environment and experimental question; it does not overturn that
result.

## Ground every capability in the body and world

`AdventureActions` is the intended interaction boundary. The controller submits
an action and typed target; the boundary verifies it and reports progress,
completion, interruption, or a specific failure. Learning is updated from the
actual result. A visual animation, attempted navigation, or an item label alone
cannot establish successful use.

| Capability | Required checks and effects | Default boundary |
| --- | --- | --- |
| Walk and return home | A reachable target, bounded home range, route progress, timeout, safe interruption and fallback | Existing terrain and navigation; no automatic bridges, tunnels or teleports |
| Operate a door | Allowed existing door, actual interaction reach, valid state change, path/occupancy checks, bounded retry and close policy | Explicitly supported wooden doors; no inferred permission for iron doors, locks or arbitrary modded mechanisms |
| Eat | Real possessed or reachable allowed food item, one actual consumption, corresponding need change | No generated food, duplicate drops, or merely observing another agent eat |
| Possess/equip a tool | Real item transfer or existing inventory/equipment, compatible slot and body, single ownership and visible state | No conjured equipment or copy on equip |
| Use a tool | Registered item/action pair, valid living or interaction target, range and sight, elapsed cooldown, applicable durability expenditure and grounded result | Only explicitly permitted use; equipment alone grants no general action permission |
| Defend or assist | Target classification, actual equipped capability, reachable target, attack cooldown and observed damage/outcome | No remote damage, imaginary shield, universal attack capability or free healing |
| Break/place blocks | Central policy check before any world-changing operation | General `BREAK` and `PLACE` remain false; only the separate `HARVEST_REPLANT` operation below may replace its validated crop |
| Harvest and replant | `HARVEST_REPLANT`: mature wheat, carrots, potatoes or beetroot only; actual hoe, reserved real seed/material, reach, native loot and applicable durability; immediately restore same crop at age zero over intact farmland | Narrow user-authorized exception; no immature crop destruction, other block changes, or success without replanting |
| Transform another block with a tool | Separate explicit authorization for the exact transformation | Denied by default; a hoe, axe or shovel does not implicitly permit tilling, stripping or path conversion |
| Use a chest or trade | Explicit access/ownership rules, range, item transfer and conservation, transaction result | Future until implemented and tested; no assumed right to empty generated chests |

### The crop exception is one bounded operation

The user explicitly permits ripe harvesting with immediate replanting while
protecting all other blocks. `HARVEST_REPLANT` covers four vanilla field crops:
wheat, carrots, potatoes and beetroot. It requires the crop's mature state, an
actual possessed hoe, real interaction reach, and reserved planting material:
wheat seeds, a carrot, a potato, or beetroot seeds respectively. Reserve one from
owned stock first, or from the actual native loot sampled once at physical reach.
A possible future drop is insufficient: if this actual sample cannot fund the
replant, leave the crop and inventory unchanged and release no loot. Verify the
existing farmland and replant preconditions before changing the crop.

A completed operation uses native crop loot, accounts for the reserved material
and applicable hoe durability, immediately puts the same crop back at age zero,
and leaves its farmland intact. The audit must distinguish obtained produce,
reserved/consumed planting material and actual inventory changes; it must not
both keep and drop the same item. A rejected attempt must not gain loot or claim
success. Missing ingredients, an immature crop, a different block, unreachable
target, or invalid substrate does not become an ordinary break/place fallback.

This narrow replacement operation leaves general `BREAK` and `PLACE` false.
It authorizes no crafting, tilling, log stripping, path conversion or arbitrary
block interactions. Opening an existing door is a separate explicit permission.
Modded crops/items require explicit future support. Native beetroot production,
harvest-funded replanting and restart persistence are now recorded in
[harvest-bootstrap](../evidence/2026-11-07/harvest-bootstrap/README.md).
See [transaction and budget rules](harvest-replant.md); native acceptance for
the other three crops and sustained food adequacy remain open.

Generated baseline villagers receive no free tools or seeds. If a player hands
over a hoe or planting material, transfer the actual item with conservation and
record the giver, recipient, item/count and tick. Such a handoff is a labelled
intervention and the resulting capability demonstration is separate from the
untouched natural survival baseline. The harness must not synthesize a player's
handoff to supply a missing hoe or seed.

Villagers' authored hunger still influences decisions without causing starvation
damage. A pending harvest capability is not yet a demonstrated renewable food
economy. Do not interpret agents remaining alive without food as proof of
resource self-sufficiency. Chest access with permission, exchange/trading and
additional renewable resource loops remain future designs; they must respect
the capability policy and must not rely on hidden test gifts.

## Villager and golem agent types

Use one identity model with world scope and entity UUID, then distinguish body
or species, current role, capabilities and personal history. These concepts must
not collapse into a single personality field. In a future generalized schema,
a villager and a golem can each own a disk record without sharing the same need
model or action set.

A villager's optional `Home` is a dimension and bed position. A guardian may have
no bed home; territory, defended population and patrol anchors require separate
typed records. Keeping that distinction avoids assigning a golem a bed or
requiring it to eat and sleep. A golem body also does not automatically support
player equipment mechanics. Persisting a proposed new type requires explicit
schema compatibility and retention of earlier records.

The first generated-village run leaves golem AI vanilla. Natural guardians are
both existing protection and part of the environment used by the vanilla
comparison. Record their presence, health, deaths and movement; do not replace a
lost golem or give the managed village an extra one.

Later guardian work can add bounded threat signals, patrol coordination and
rescue attempts. Each signal should carry its source identity, dimension,
location, observation tick, expiry and confidence. Recipients must revalidate
stale threats, respect capability and path constraints, and avoid all-population
broadcast or pairwise scans. A rescue memory should describe a completed
observable intervention, not just accepting a task. None of this presently
establishes heredity, a new biological race, or emergent social cooperation.

## Trial harness and audit contract

`VillageTrial` is the intended generated-world harness. Its audit should contain:

- Trial ID, world/save name, seed, generator preset, difficulty, pack hashes,
  revision, admission tick and observation duration in world ticks.
- Original villager and golem UUIDs, types, initial health, equipment/inventory
  available to the experiment, position, and existing home or defended area.
- Adoption state and comparison policy for each original, plus every later
  natural addition reported separately.
- Periodic server-side state and explicit death/removal reasons; distinguish
  unloaded/unknown entities from confirmed deaths and from client tracking loss.
- Actions, typed targets, reach/cooldown/permission failures, door changes,
  item consumption/transfers, player handoffs, applicable tool wear, and completed
  outcomes. For harvests, include crop type/maturity, reserved material, native
  loot, age-zero replacement and unchanged farmland.
- Every harness or operator intervention, including time/weather changes,
  population/resource mutations and camera operations. Unexpected setup gifts or
  construction invalidate the natural-conditions acceptance claim.
- Measured server tick time, cognition/navigation query cost, queue/save growth,
  loaded population and client rendering observations. No target population is
  certified from a single small village.

Keep screenshots and structured observations from the visible client. Server
records establish population and action outcomes; screenshots establish what was
rendered. Audit population snapshots before and after restart. Loading the save
must preserve original identities, learned records, existing resource depletion,
missing guardians and deaths rather than re-running setup grants.

Do not claim the harness prevents all possible world changes merely because it
records commands. Check authored action boundaries and inspect world outcomes.
Ordinary terrain updates and vanilla AI need separate provenance from managed
block actions; growth or a zombie breaking a door must not be mistaken for a
managed villager violating its permissions.

## Acceptance plan

Every check below is pending for the generated-village iteration. An implementation
or passing Java test can support it, but the final acceptance needs rendered
world evidence under the recorded conditions.

| Check | Pass evidence | Failure evidence to retain |
| --- | --- | --- |
| Natural starting village | Seed/settings, generated structure location, pre-adoption original UUID roster and inventory/resource audit | Summoned cohort, rebuilt shelter, supplied assets, or missing starting audit |
| Existing shelter and doors | Original agents physically enter/leave generated homes; permitted door state transitions and night return are recorded | Threshold traps, doors operated out of reach, repeated unreachable bed choice, exposed sleeping |
| Strict block policy | Capability rejections plus rendered attempts show general breaking/placing and every unapproved transformation are denied | Changed/destroyed protected blocks, immature crops, or a break/place fallback bypassing the crop exception |
| Ripe harvest and immediate replant | Each of four mature crops, actual hoe/material possession, native loot and applicable wear, same crop at age zero, farmland intact; tests of every missing precondition reject safely | Free seeds/tool, duplicate loot, immature harvest, lost farmland, missing replant, or expanded block permissions |
| Real tools | Existing legitimately obtained item, correct equipment, in-range supported use, cooldown and applicable durability changes | Free tool, duplicated inventory, remote damage, skipped cooldown or missing applicable wear |
| Threat and natural guardians | Ordinary hostile encounter with detection, shelter/escape outcome, guardian actions, damage and original survival recorded | Flee labels without escape, hidden rescue, added defenders, unreported resident or guardian death |
| Continuity | Graceful save/restart retains original UUIDs, homes, values, depleted resources and missing/dead entities | Replacement residents, reset inventory, recreated golems or lost records |
| Useful learning | Repeated observable outcome changes a later choice and persists, compared with the frozen learner | Updated counters without changed behavior, or safety-rule triggers called learned adaptation |
| Multi-day survival | All originals survive at least three complete normal-rate day/night cycles, with server-side health/death history and no replenishment | Any confirmed original death fails complete-cohort survival; unknown/unloaded state leaves acceptance incomplete; no improvement claim without comparisons |
| Scaling | Measured loaded cohorts only after small runs pass; report hardware and median/p95 server tick duration, saves, memory and FPS | Tick backlog, stale decisions, stalled writes or unmeasured population claims |

For comparisons, use separately generated saves with the same pinned seed and
setup rule, then match observed initial resources and populations as closely as
possible. Entity UUIDs can differ between independent saves: preserve UUID
continuity within each trial and compare initial body/role/resource conditions
between trials. Report residual differences and repeat seeds; one trial against
a different initial village cannot establish an AI advantage. Keep golem policy
consistent across managed, frozen-learning and vanilla controls.

If a test requires a player handoff of tools or seeds, other deliberate
provisioning, or a specially positioned threat, run it as a labelled interaction
fixture outside the untouched natural survival trial. A handoff must transfer
actual items and be audited. Its results can establish a capability while the
extra resources cannot be counted as natural-village survival evidence.


## Later source extension: optional hay gathering

The crop-only boundary above remains the default. Current source adds a separate,
explicitly opted-in `hayForaging` extension for visible exposed existing hay and
native grain unpacking. It is off for missing/older configs. No other terrain
capability follows from holding a tool. See [food bootstrap](food-bootstrap.md)
for exact limits and pending live acceptance. Historical baseline evidence is not
rewritten or represented as having used the extension.
