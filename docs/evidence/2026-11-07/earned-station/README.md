# Earned forestry wood becomes a paid crafting table

Native chain verified: one previously harvested mangrove log→four native planks→
one real table item→one placed crafting table. One log and one root remain.
Protection initially blocked all spending; removing only that protection enabled
the existing job. Table/materials survived restart without repeated work.
280Java+10Python pass; verification.json records40data/source assertions.

## Source changes and performance

StationProduction previously required farming inputs even for an explicit station
job. It now also admits a fed adult (food>6) holding enough supported wood or an
actual table. Hungry food-critical station preparation retains the existing
exception. Every native recipe,capacity,body,home,reach,sight,entity,grant/protection
and placement recheck remains. No grain/seed gift is needed to enable a woodworker.

WoodCrafting.canMakeTable is a cheap inventory-only check with no item copies,
recipe reservation or terrain scan. A no-admitted-job fast path avoids native
table searches and recipe allocations entirely,with the existing100tick backoff.
This matters when many residents hold wood but no table job exists. Path budgets
and metadata bounds remain unchanged. No schema,worker,timer or model job added.

## World and actual inputs

runs/earned-station/play exactly copied260stopped files from runs/forestry-root/play.
That parent history contains Juniper's actual donated meal,then native seed/root/
log harvesting and paid replanting. Juniper began with two earned logs+one root,
food10/health20,no grain,planting seeds or hoe. No fixture histories were merged.
The original parent world remained unchanged throughout all three native runs.

Read-only disk survey around the recorded beach position shortlisted137air sites
on known support. This was not native path/ownership acceptance. Operator native
site approval validated(-281,75,745): AIR over sand,clear headroom,four adjacent
standing positions,no nearby bed/door/block entity. Only observer teleports,
one finite table grant,one explicit protection and its later removal,plus
read-only watch/status. No wood/plank/table/food/seed supply,actor movement,
health/hunger/fatigue/age,clock,growth or RNG setter in these runs.

## Recorded native runs

6283exit0,400ticks52415→52815/final52819. Native table grant
9fae0d6b-e491-4669-bd75-1ba2be6284d0 and overriding one-cell protection
909ea3e8-48e7-4147-8126-45bb165c25e2. Native recipes0/build0; actual two logs+root
untouched. Site stayed AIR;grant/protection retained. This binds source-protected.zip.

32508exit0,1200ticks52820→54020/final54023. Removed only that protection UUID;
no replacement job/materials/actor/clock changes. Early native status captures
one recipe completed: log stock2→1 plus four actual mangrove planks. Final status
records two recipes and one built table,log1/root1,zero planks/table items. Native
loaded recipes and actual item reservation/consumption performed each stage.
Raw chunks show CRAFTING_TABLE at the authorized target,unchanged sand support/
headroom/access cells,and the earlier paid propagule still stage0 on mud.
Completed station grant retired. Food10/health20. Builder profession is none;
this does not depend on native farmer status or supplied planting stock.

47102exit0,same-world400tickrestart54024→54424/final54428. Table and selected terrain
persist exactly,one log+one root remain,recipe/build counters0,grantstillretired.
No replay or repeated material charge. Minecraft inventory-slot compaction is
not treated as a resource change.

The first verifier draft wrongly expected the early plank note to remain in the
short memory ring at final cutoff. It was legitimately evicted. The retained
early snapshot contains the plank note;final snapshot contains table crafting/
build memories. Verification now binds each note to its actual capture. Native
intermediate inventory,recipe counters,final material costs and raw blocks are
independent confirmation. No game/source behavior was changed to satisfy it.

The final source only adds the no-job scan fast path after the protected cohort;
source-main.zip matches both approved/restart source pins. All280checks pass in
build.log (initial) and build-final.log (current,handle1843exit0).

## Next food-production evidence

Stopped food-gap/report.json surveys17×17×7cells around(-226,72,752),retaining raw
chunks,and joins existing native shutdown equipment/state. Eight farmland/crops:
two mature beetroot at(-230,72,752),(-228,72,751);six wheat age1..5,all immature.
Fern has a usable iron hoe8/250;Dara a usable wooden hoe0/59. Another actual crafting
table exists at(-233,72,746),not the old guessed anchors. No new tool/table supply
should be substituted for inspecting the actual food-selection/path issue.

Correction from the subsequent harvest-bootstrap investigation: the original
diagnosis of a grain-only early return was wrong. The default crop search already
includes all four crops. Both equipped adults lacked beetroot seeds, and the old
search/harvest required a matching seed in advance. Discovery also depends on
being within the existing radius-eight scan. No stockCropTarget change was needed.
Native memory subsequently confirmed Fern's jobsite at(-226,72,752).
The next cohort reserves one real seed from actual native loot when owned stock
is absent; it produced two beetroots and replanted both crops without new supplies.
See [harvest evidence](../harvest-bootstrap/README.md). This corrects interpretation
only: the original raw records, source archives and verification remain intact.

## Custody and limits

All runs kept4originalsurvivors+existingchild,7old deaths,0missing. Mean tick samples
3.797294/3.149041/3.68097ms for five actors,not population/tail-latency proof.
Existing pack/model/JOML warnings remain. Native screenshot is an overview,not
standalone proof of the inventory/recipe chain.

Exact captured chains,all terminal:
929684/14954361,929792/14954419,929950/14954928;
932264/14981652,932366/14981709,932586/14982059;
934113/14999781,934221/14999839,934336/15000093.
Original frozen3identities remain exact;historical adaptive3remain absent,no
cause/replacement inferred. No original client/world/service changed,no pending
command or owned automated release/build timer. No resource hold/model/hash/
inference grant;existing005/006/007 evidence untouched.

Source0.4.1-dev stays unreleased;no commit/PR/release/deployment/service change.
Source+installed0.4.0 SHA remains
4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0.
Source/archive/dev-JAR hashes are in verification.json. Natural tree regrowth,
repeated food/wood output,material logistics,general buildings,parent care,defense
and population performance remain ongoing work toward the full society goal.
