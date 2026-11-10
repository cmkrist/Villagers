# Real harvest materials and immediate replanting

Unreleased 0.4.1-dev. Mature wheat, carrots, potatoes and beetroot remain the only
supported crops. An actual usable hoe, intact farmland, loaded terrain, physical
reach within three blocks, native outline visibility and protection checks are
required. This operation grants no general terrain edit authority.

Target discovery no longer requires an existing matching seed. It does not roll
loot: the existing radius-eight/y±three scan and controller caches remain bounded
at 100 ticks. The default search includes all four crops; its boolean argument
only excludes wheat when false. No controller grain-fallback change was necessary.

At physical reach, sample native loot once with the real tool and actor. Build a
detached inventory receipt: consume one matching owned planting item first, or
one matching item from this actual loot if stock is absent. Merge the remaining
real drops into the detached bag; retain exact overflow for release after success.
No payment means no terrain change, income or wear. Inputs are copied, not mutated.
There is no reseeding or repeated roll within an attempt. Attempts back off for
40 ticks; unfunded native loot backs off for 200 ticks per actor.

Compare every live inventory slot, tool, crop, soil, actor state, reach, sight and
protection after loot callbacks. Set the same crop to age zero, recheck, then
commit the inventory receipt once. A stale receipt cannot commit. Mark it used
before writing slots, including when a full bag means the final bag is identical
and only overflow changes. Release exact overflow and charge native hoe wear
only after successful replant and receipt commit. Failed post-write validation
attempts to restore only the exact authored age-zero crop, leaving unrelated
replacement states alone. Arbitrary mod callbacks and process crashes across
entity/world saves are not globally atomic.

Each actor keeps counters and one last successful harvest receipt, not a growing
event history. Debug snapshots expose actual native drops, before/after bag,
planting source, overflow and tool wear. These diagnostics reset on load; actual
native crops, inventory, wear and existing mind memories persist normally.

Five material tests cover exact payment, missing/wrong inputs, full-bag overflow,
stale receipts and one-use semantics. A policy test distinguishes inspectable
mature crops from an authorized paid replant. Native evidence verified beetroot
with zero initial beetroot seeds, both payment sources, and restart without replay.
That first cohort established new edible reserves. A subsequent unchanged-source
[regrowth observation](../evidence/2026-11-07/farm-regrowth/README.md) recorded both
earned beetroots being eaten and natural wheat maturity, but also a zombie death.
Repeated food adequacy and village defense remain unproven.
