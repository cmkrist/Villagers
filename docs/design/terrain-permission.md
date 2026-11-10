# Limited autonomous construction

User choice recorded2026-11-07: **Limited autonomous building using real materials,
with protection for player structures.** This replaces the former unresolved
terrain preference. Do not ask for that broad choice again.

The intended society can expand farms and construct useful buildings while
conserving actual inventory, recipes, tools and work. Construction is not a supply
fixture: villagers must obtain and pay the materials they use. A future test must
distinguish supplied fixture materials from autonomous acquisition.

Player structures must be protected. Block type alone cannot establish whether
a block was placed by a player. Define bounded work sites and a durable protection/
ownership policy before enabling edits; unknown or protected structures, containers,
occupied buildings and player property cannot be treated as free resources.
New ownership tracking cannot by itself classify builds that predate tracking.
Approval of limited construction is not authorization for unrestricted excavation,
demolition, chest looting, instant placement or replacement of terrain without cost.

Use explicit narrow capabilities with reach/sight, loaded-site and native collision
validation, rechecks at commit, real recipe/capacity/material accounting, bounded
work/path budgets and persistent progress. Preserve worlds and never claim an
implemented protection boundary before its native behavior has been checked.

Current unreleased0.4.1-dev implements the first narrow operation: till and plant
explicitly approved bare ground using one real planting item and one hoe durability.
Generic BREAK_BLOCK/PLACE_BLOCK remain disabled. Existing crop harvesting and hay
foraging also respect the new explicit protected areas.

An operator standing on the intended soil can approve a radius0..4 footprint with
`/villagers site farm <radius>`. Only loaded bare dirt/grass with two air blocks
above and open sky is admitted. This command is an explicit grant, not automatic
inference that all dirt belongs to the village. Protect structures and their useful
surroundings with `/villagers site protect x1 y1 z1 x2 y2 z2`; protected areas always
win. `/villagers site list` shows identities; `/villagers site remove <UUID>` revokes
either kind. These commands require a player with gamemaster permission.

Each soil cell has a finite permission covering soil, crop and headroom. Completion
retires it. An outside standard `Level.setBlock` attempt in that footprint retires
it before the write, even if the attempt leaves the block unchanged. Player edits
are not cancelled. The native LeavesBlock.tick exception described in
[forestry rules](forestry.md) retains only unchanged pending approved leaves;
direct player same-state writes still revoke. Mods that write chunks directly can bypass this hook: explicitly
revoke grants before unsupported bulk editing. This does not discover or claim
ownership of historic buildings, nor protect every possible mod mutation.

World-local schema6 metadata is capped at128KiB,8farm sites of at most81cells,
8station grants,8finite forestry jobs,4soil transfers and32protected cuboids of at most4096blocks each.
Pending soil transfers also reserve one of the eight farm slots. Older schemas migrate
without inventing new authority. Clean restart retains permissions;
an unclean-session marker revokes farm/station/forestry/soil grants while retaining protection. Invalid
or unwritable metadata disables construction. Control changes use bounded durable
writes; ordinary AI polling performs no file writes. This is not a claim of atomic
crash recovery across Minecraft chunks, entity inventories and metadata.

Native evidence covers outside-write revocation, protection overriding a grant,
one paid new plot after explicit protection removal, and restart without duplicate
material costs. See [evidence](../evidence/2026-11-07/construction-sites/README.md).
Station crafting/placement now has its own finite permission and real-item cost;
see [station rules](station-production.md). Finite [forestry jobs](forestry.md) now implement bounded native-resource work;
the first native log-income and paid-planting chain now passes. Natural growth
and repeated renewable output remain unverified. General buildings remain
future work under this same boundary.

Finite [soil transfers](soil-transfer.md) now let villagers collect one native dirt,
carry it along an explicitly assigned bounded trip, replace one approved wet sand
cell and till/plant it with real inventory. Destination protection blocks even the
remote gathering stage. Native conservation, planting and reopen passed; discovery
and assignment of new land still require an operator. No extra terrain authority
comes from the larger travel corridor.
