# Finite soil supply for farm expansion

Unreleased0.4.1-dev. The arid village has no bare farmable ground within ordinary
home work bounds. Soil jobs let villagers gather actual dirt farther away, carry
it to an exact bare-sand destination, pay for replacement and leave one ordinary
till/plant permission. They do not manufacture soil or authorize general mining.

The owner command is `/villagers site soil sx sy sz tx ty tz`. It requires a nearby
gamemaster player, both endpoints loaded/within32blocks, one exposed dirt/grass
source with clear headroom/sky and one supported bare sand destination with nearby
water. Approval is explicit consent for those cells, not block-type ownership
inference. Player structures and unknown land gain no automatic edit permission.

Schema6 adds at most four soil jobs, each reserving one of the eight pending farm
slots. Old schemas1..5 migrate with no soil authority. A legacy version cannot
smuggle a soil job; future/invalid metadata fails closed. Source/support/headroom
and destination/support/headroom are watched. Protection at either endpoint blocks
the entire transfer. Outside standard block writes revoke pending authority before
the write; dirty shutdown revokes soil jobs while keeping protection. Raw chunk
editors remain outside that hook's guarantee. No global crash atomicity is claimed.

A fit adult with more than6food may undertake an explicit assigned day trip. Its
source must be within64blocks of home and its destination within28. Only that job's
eight-block source corridor, capped72from home, is admitted under a live claim.
Ordinary roaming remains28; other work's home/path validators stay unchanged. Native
rest, danger, poor body readiness, expiry or lost authority restores normal safety
control. A trip is capped1200ticks with80tick motion-stall detection and bounded
failure backoff. Paths must be complete, loaded and reach their exact standing goal
within128nodes. No partial-resource paths or chunk generation.

Planning shares the existing two-batch/dimension/second budget: at most three path
queries per admitted batch and75local standing candidates. Candidate progress is
bounded across batches. A real reachable path or actual interaction reach is needed
to claim work; stranded residents cannot lock a job by merely selecting it.

Digging and filling each require40ticks at native reach/sight with empty hands.
A held nonweapon is stored intact; real weapons are never put away for this job.
Native loot must be exactly one dirt for gathering or one displaced sand for filling.
A detached whole-inventory receipt checks capacity/components, rejects stale state
and commits once. Filling consumes one actually held dirt and retains the native
sand. No overflow is silently lost. Body, permissions, crop/headroom, support,
inventory and world state are rechecked around the write. Failed post-write checks
restore only the exact authored state and retire uncertain authority.

After a successful paid replacement, the same planned footprint becomes one fresh
finite farm-cell permission. This is a stage of the original approved job, not new
land authority. Existing hoe/seed costs, path checks, protection, native growth and
completion retirement govern tilling/planting. Soil income is never replayed after
the source has been consumed. If a carrier loses its actual dirt, none is restored.

This remains an owner-assigned prototype. Villagers perform the real supply chain;
autonomous discovery, land claims, larger construction and adequate household yield
remain ongoing society work. Native acceptance is recorded separately from code
and unit coverage under `docs/evidence/2026-11-07/soil-transfer/`.
