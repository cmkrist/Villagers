# Finite protected mangrove work

The accepted terrain policy permits limited building using real materials, with
protection for player structures. Unreleased0.4.1-dev now implements a finite
mangrove job. This does not identify historic player trees/builds from block type
or grant general logging, excavation, root removal or demolition.

An operator explicitly selects one mature hanging propagule,1..4 exact vertical
mangrove logs and one native-valid planting cell. A managed adult must collect
actual native seed loot and retain a real propagule before cutting anything else.
Optional clearance requires separate explicit grants for at most4 exact mangrove
leaves and one exact dry mangrove root. Only after clearance may the approved log
quota be cut. Planting consumes
one actual propagule after native placement and revalidation. Completion retires
the job. Loot uses native Block.getDrops with an empty tool: exactly one seed/log,
or empty/at most2 sticks for leaves; unsupported output/capacity fails closed.
The actor's main hand must be empty. Axes and general tool harvesting are future.

A granted job can safely store a held hoe in an actual empty inventory slot
before returning to bare-hand work. It retains the tool, its wear and the existing
seed/log/planting obligation when food work interrupts forestry. Automatic idle
hoe equipping waits during that actor's active eligible forestry job; an actual
food harvest can still equip the stored tool. Weapons are never put away by this
rule. Full storage leaves equipment and terrain unchanged. Loot and planting
commits still require an empty main hand, including their final rechecks.

Commands require a gamemaster player near loaded positions:

```
/villagers site forestry sx sy sz lx ly lz count px py pz
/villagers site forestry leaves UUID x1 y1 z1 x2 y2 z2
/villagers site forestry root UUID x y z
/villagers site forestry move UUID lx ly lz count
/villagers site list
/villagers site remove UUID
/villagers site protect x1 y1 z1 x2 y2 z2
```

The leaves cuboid has volume at most4 and admits existing leaves/AIR, with at
least one leaf. AIR grants no clearance. Leaves are ordered by descending native
DISTANCE to avoid unnecessary changes to remaining leaf distances. Move is an
explicit replacement of log authority after seed collection: a fresh identity,
no increase in outstanding quota, and the same owner/seed/soil/planting duty.
Leaf/root clearance also gives fresh authority; ordinary progress can never add
cut cells. Root permission admits only one existing loaded dry MANGROVE_ROOTS
block. Its exact native loot is one root item, never substituted logs or planks.
Old leaf grants cannot clear roots.
These commands assign finite jobs, not automatic resource discovery.

Work-sites schema5 retains at most8 forestry jobs in the existing128KiB control
file. Positions stay within16 horizontal/12 vertical blocks of the seed and
within the resident's existing28block home range. Old schemas migrate without
inventing leaf or resource permissions. Dirty-session recovery revokes all edit
grants and keeps protection; bad metadata disables construction. Clean partial
seed/clearance/log progress persists. There is no global crash atomicity across
Minecraft chunks, entity inventories and metadata.

Protection wins across the whole current footprint. That footprint includes
pending logs/leaves/roots, seed/support until harvested, and planting soil/target/
headroom. Standard outside Level.setBlock attempts retire the whole affected job
before writing, including direct same-state attempts. Player edits are not
cancelled. Completed resource cells no longer carry edit permission. Unsupported
raw chunk editors can bypass this hook and require explicit prior revocation.

There is one narrow caller-specific exception: LeavesBlock.tick writes identical
states when refreshing distance. Its exact setBlockAndUpdate call may retain a
job only for an unchanged pending approved MANGROVE_LEAVES block. A changed
distance, persistence, waterlogging, block type, decay, another cell or a direct
outside same-state write still revokes. The exception grants no terrain change,
loot or new cells. It has no timer and wraps only that synchronous native call.

Work requires a safe awake adult, empty main hand, loaded target, home bounds,
actual eye reach3 and native collider sight. Log/leaf/root removal rejects entities in
the block and above it; planting requires empty native space and the captured
soil type. Seed/leaf/plant work takes20 ticks; bare-hand log/root work uses native
hardness times30, minimum20. Full receipt comparisons and slot comparisons deny
stale inventory; writes/rechecks/rollback stay on the exact authored cell.
Successful breaks exert real food exhaustion. Optional forestry now also requires food above6 at work and commit. A hungry
resident retains its real seed and permission while seeking food; food-work
eligibility itself remains unchanged. See [food help](food-help.md).

Planning uses the shared2 batches/dimension/second budget, at most3 native paths
per batch, at most128 nodes, a100tick lookup interval,80tick movement stall and
1200tick continuous-job cap. Failed routes back off400ticks. Candidate stands are
bounded to100 positions around each target; AIR/AIR, sturdy support, hypothetical
eye reach/sight and home bounds filter them. A cursor rotates attempted stands
across retries instead of always trying the nearest3. Read-only forestryRoute
reports candidate count, batches/rejections and the last path. It does not carve
routes, force-load chunks, teleport residents or accept unsafe partial paths.

Native evidence now demonstrates the complete first material chain: one wild
propagule collected, three approved leaves and one separately approved root
cleared, two actual logs harvested, and one paid propagule planted. Inventory,
selected native blocks and completed-job retirement persisted across restart
without replay. A direct same-state root write still revoked fresh root authority.
This used the same society history in which a neighbor had already donated a real
meal, plus one labelled operator daylight setting. No wood/seed supply or body/
growth setters. Natural growth and a second harvest cycle remain unverified.
See [current evidence](../evidence/2026-11-07/forestry-access/README.md);
[earlier failed trials](../evidence/2026-11-07/forestry/README.md) remain retained.

The play-copy-only `/villagers forestryprobe UUID` uses the actual production
standing checks and native ray result, without creating paths or starting work.
It identified the dry root occluding both selected logs from a valid mud stand.
It does not waive hunger or authorize any cut. Reports are bounded and written
only on explicit invocation. Broader physics/growth-protection and population
performance cases remain open before expanding this work into general terrain
modification.

The read-only terrain survey covered56 chunks, Y64..111 and119 nearby log columns,
all mangrove;64 nearby hanging propagules included mature age4 blocks. Only
Juniper's current home range covered inspected logs. These bounded observations
are not ownership or path guarantees. Preserve the current fixtures and failed
attempts; preserve explicit authority and food resilience before extending cuts.
