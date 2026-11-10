# Paid crafting-table construction

Unreleased0.4.1-dev extends the accepted limited-construction rule to crafting
tables. An operator standing in the intended empty block grants one finite job
with `/villagers site table`. `/villagers site list` shows the grant and
`/villagers site remove <UUID>` revokes it. These commands require gamemaster
permission and a player. This is explicit authority to use that location, not
ownership inferred from its block type or surrounding buildings.

The target must be air over sturdy support with air above, no fluid or farmland
support, and at least two usable adjacent standing cells. Beds, doors and block
entities within horizontal2/vertical1 prevent construction. The same checks run
at work and placement, with actual home range, eye reach, sight and entity collision.
Villagers never replace an existing block. Generic break/place remain disabled.

A station grant watches11positions: target, support, headroom and the four adjacent
standing/headroom pairs. Explicit protection overlapping any watched cell wins.
Standard outside `Level.setBlock` attempts at those cells revoke the grant before
the write, even same-state attempts. Normal player edits remain possible. Completion
retires the grant. Mods that mutate chunks directly bypass this hook; revoke grants
before unsupported bulk editing. This does not discover historic player ownership.

Work-sites schema5 retains up to8station grants alongside finite farm/forestry
authority and protection. Older supported schemas migrate without inventing new
permissions. Invalid/future metadata fails closed. Clean restart preserves
grants;an unclean-session marker revokes edit grants and retains protection.
The128KiB,8farm/81cell and32protection/4096block bounds remain.

An adopted safe adult needing a table for held wheat or a missing farm hoe can
take the job. A fed adult (food>6) with real table materials can also fulfill an
explicit station job without grain/planting supplies. This optional builder path
yields to urgent hunger;food-critical preparation retains its existing exception.
Real held wood becomes planks using the loaded server recipe, then
four real planks become one table through the loaded2x2recipe. The actual table
item exists in inventory before a later placement operation consumes it. Exact
output/count, no byproducts, unchanged live inputs and full capacity are required.
Blocked later work retains earlier real intermediate materials. An owned table
can skip crafting. No resources, actor movement or work completion are supplied
by normal production code.

Explicit assigned sites can be considered within a32block horizontal/8vertical
broad phase, with the normal28block home restriction and loaded-site checks. This
is travel to an announced job, not discovery of unseen natural resources. There
are at most8candidates, not a terrain-volume scan. Actual paths remain bounded
and share2planning batches/dimension/second, with at most3path attempts per batch,
40tick path retry,80tick progress stall,400tick continuous trip and400backoff.
Without an admitted station job,work returns before native table searches or
recipe reservations. A cheap inventory-only material check avoids item copies
for idle nonbuilders. Site lookup/missing inputs back off100ticks. Each recipe and placement needs20
consecutive work ticks. Safety, trading, player reactions and food work keep
priority; authored construction earns no elective utility credit.

After placement, the builder's existing table cache points to the new station so
tool or bread work can use it. The block itself persists natively. The first earned forestry-log→paid table chain is now native-verified. Long-distance
station knowledge after reload,renewable wood output,other building recipes,
village planning and sustainable production remain separate work. Metadata changes
use bounded synchronous durable writes, including grant completion; ordinary AI
polling performs no IO. Larger-site/population latency is still unmeasured.

Rollback only removes an unchanged authored table when a synchronous post-write
check fails. Atomic crash recovery across chunks, entity inventories and metadata,
or arbitrary third-party callbacks, is not claimed.

See [native evidence](../evidence/2026-11-07/station-production/README.md) for the
protected case, material costs, continuation failures, actual paid construction
and restart results.

[Earned-wood evidence](../evidence/2026-11-07/earned-station/README.md) verifies
protection,no farming-input requirement,real plank/table intermediates,placement
and clean restart without replay. No logs/planks/tables were supplied in that
trial;the materials came from the earlier native forestry history.
