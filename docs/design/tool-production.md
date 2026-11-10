# Tool production from real held wood

Unreleased0.4.1-dev can replace a missing farm hoe using owned wood and an existing
reachable crafting table. This extends the accepted real-material construction
direction; it does not grant terrain edits, tree ownership or permission to take
materials from player containers.

An adopted adult carrying a supported planting item and lacking a usable hoe can
prepare one. Immediate survival, trading, player reactions and food work retain
priority. Production also runs when food stock is already sufficient and during
an adult's birth cooldown. Babies, fatigue>=.75, night, sleeping, fire, water and
insufficient air prevent work. The authored operation earns no elective learning
credit. Normal equipment switching stores displaced equipment rather than deleting it.

The chain is finite: one supported log/wood/stem becomes four matching planks;
two planks become four sticks; two planks and two sticks become one wooden hoe.
Existing planks/sticks can skip intermediate steps. Mixed plank stacks are allowed
only if the server's loaded recipe accepts the actual input. The implementation
supports the11ordinary oak/spruce/birch/jungle/acacia/dark-oak/mangrove/cherry/pale-oak/
crimson/warped wood families, including stripped wood. Custom recipes must return
the exact supported item/count and no byproducts; otherwise no ingredients commit.
This is a fixed bootstrap capability, not general recipe discovery.

Each step works on detached ingredient stacks, validates the live inventory has
not changed, and commits only if the entire native output fits. Stale plans cannot
consume anything or replay. A later blocked recipe retains earlier real intermediate
products; there is no promise of an atomic three-recipe transaction. Native entity
saving persists held/inventory items. No schema, thread, queue or per-poll IO added.

Production requires a loaded actual table within three-block eye reach and native
sight, with home/path restrictions. It uses the shared bounded table search (radius8,
y±3,100tick cache), at most3paths per admitted batch and shared2batches/dimension/
second. Each recipe requires20consecutive work ticks. A continuous production trip
times out after400ticks and backs off400; missing ingredients/table back off100.
Safety or other work gaps reset consecutive work. Larger-population latency remains
unmeasured.

Real sneak-right-click gifts now accept these wood ingredients and sticks, one
actual held item per handoff, with existing reach/sight/capacity checks. No materials
are automatically supplied. The play-only `woodprobe supply <UUID>` explicitly
labels one log/one seed operator input; it invokes no recipe or movement action.

[Native evidence](../evidence/2026-11-07/tool-production/README.md) verifies the chain
from a supplied log, equipment, ordinary birth-cooldown independence and save/reopen.
Station crafting/placement is now implemented under a separate explicit grant;
see [station rules](station-production.md). Autonomous wood gathering,renewable
tool supply and a self-sustaining economy remain open. Preserve explicit work-site/
protection rules when later operations edit the world.

## Replacing a tool worn out by actual work

A native fixture now lets ordinary harvesting break an initially declared iron
hoe with one use remaining. The controller collects an explicitly granted hanging
propagule and log, pays the native plank/stick/wooden-hoe recipes, and harvests
again using the replacement. Food work may interrupt forestry before the second
granted log and paid planting are complete. Forestry now safely stores the hoe
to finish those obligations rather than becoming permanently blocked by its
occupied hand. No equipment is discarded to make space.

The clean partial-save variant stops with the replacement already used, one log
still granted and one retained seed still owed. Reloading the same saved actor
finishes only the second log and planting; a further restart checks conserved
items, tool wear and no repeated recipes or resources. A separate full-storage
fixture leaves the held hoe, inventory, terrain and grant unchanged. Details and
the earlier failed cases are retained in
[tool-renewal-native](../evidence/2026-11-07/tool-renewal-native/README.md).

This supplied peaceful fixture tests replacement and resumption. Its two declared
logs and one seed are finite; it does not demonstrate tree regrowth, repeated
renewal of terrain authority, indefinite tool supply or a sustainable economy.

## A spare for an actually heard customer

A fed, fit adult that already has a usable working hoe may now prepare one spare
after an understood, currently affordable local trade request. Its personal
15nutrition reserve and working tool are retained. There must be no saleable
spare already held. ToolDemand names one customer, expires after12000loaded
ticks, rejects replayed timestamps and drops on unload. It is an observation,
not a paid order, terrain authority or debt.

The existing native recipe, owned wood, actual table, work interval, route and
inventory checks still govern production. Intermediate planks/sticks remain if
work stops; no output is supplied and the next step reads the real inventory.
The completed spare stays in inventory while the actual working held hoe stays
equipped, even when the spare is less worn. A later actual quote/acceptance and
emerald transfer are required for a sale. Only the real sale to that customer
fulfills its signal. See [local trade](local-trade.md) and the separately retained
[production trial](../evidence/2026-11-07/market-production-native/README.md).
