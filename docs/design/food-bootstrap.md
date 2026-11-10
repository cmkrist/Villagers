# Finite grain bootstrap

Current source, November 7, 2026. Implemented and compiled; live world acceptance
is pending. The last courtship run left five original residents alive and six
dead from starvation. Preserve that history. This change does not establish
survival improvement or ordinary birth.

## Boundary and behavior

`hayForaging` is an explicit boolean setting in `config/villagers.json`, default
`false`. Missing and invalid values remain false. No existing runtime config or
world was changed in this source pass. This preserves the earlier crop-only
terrain policy unless a world owner opts into the specific extension.

When enabled, hungry villagers, or adults already provisioning a family, can
use visible exposed hay as a finite grain source. Existing edible inventory,
loose food, held grain and eligible crops retain precedence. The hay fallback
requires a known existing crafting table and fewer than three wheat, with no
held hay waiting to unpack. It does not open chests, take player inventories,
create crops, supply tools or allow other block breaking/placement.

Hay candidates are loaded, within the ordinary home boundary and within a
9-by-5-by-9 local scan. They must have air above and an unobstructed ray to the
actor. The actor cannot remove its own supporting hay or intersect the block.
Search is cached for 100 ticks, at most 405 positions per scan; paths are requested
at most every 40 ticks through the ordinary forage action. Unreachable targets
time out with that action. These are work bounds, not a population benchmark.

Gathering requires permission, the enabled setting, a living managed actor in
the same level, no sleeping/trading, real reach and visibility, available hay
inventory capacity, and at least 20 ticks of consecutive work at reach. The
validated hay block is replaced once; native bare-hand loot is deposited once,
with any overflow dropped into the world. Work is charged player block-breaking
exhaustion. State is rechecked after loot callbacks. No other block is eligible.

The block operation can consume player-placed hay as well as generated hay;
there is no origin/ownership distinction. Exposed means air above, not proof
that a bale is decorative or structurally unimportant. This is why the extension
is opt-in. It does not grant general terrain permissions.

Unpacking a possessed hay item is a separate inventory operation. It consults
the server's current 1-by-1 crafting recipe and accepts exactly nine wheat,
without byproducts. A detached transaction checks every live slot and complete
output capacity before consuming one hay. Unsupported datapack outputs are
refused. It needs no table; turning grain into bread still requires the existing
real table, reach/sight, wheat-row recipe and inventory transaction.

Gather/unpack notes persist through existing mind saving; temporary search/work
state resets normally and actual items remain in native entity inventories.
There is no new mind schema or artificial nutrition reward for gathering.

## Verification and next acceptance

`tools/test.sh --no-daemon --max-workers=2` passed 173 Java tests and 10 Python
checks. Six added regressions cover exact hay-to-wheat-to-bread conservation,
failed-output rollback, reclaimed capacity, stale-plan/replay rejection, missing
hay, and strict opt-in/default migration. Engine-backed tests still emit the
third-party JOML/Unsafe runtime warning; there were no compiler warnings.
See [source verification](../evidence/2026-11-07/hay-foraging/README.md).

Before claiming live acceptance, use a backed-up isolated world with the explicit
setting enabled. Identify genuinely available hay/table and normal actor access;
record one real block removal, native drop, exact inventory conversions and need
change, plus saved inventory/history on reopening. Check disabled mode, blocked
reach/visibility, interrupted work, full inventory and missing/changed recipes.
Do not supply or move actors/resources silently to manufacture a pass. If a
fixture is required, label it separately from the preserved cohort.

This is a finite bridge. Renewable supply still requires accessible farms,
adequate crop area, conserved tool/seed acquisition and replacement, and sensible
household reserves/sharing. Canyon escape and the unattributed shutdown executor
are separate blockers. No new game was launched in this source pass.
