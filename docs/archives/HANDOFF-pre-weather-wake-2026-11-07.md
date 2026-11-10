# Villagers Project Lead — focused continuation

Updated2026-11-07 after native covered standing-goal refuge, actual sleep, night
continuation and bounded failed-search backoff. Previous checkpoint:
archives/HANDOFF-pre-refuge-floor-2026-11-07.md. Source audit is complete.

## Full continuing goal

Lead Villagers toward a performant Minecraft society that crafts its own things,
has children, protects its village and develops interesting histories, language,
relationships and adaptation. Audit/coordinate threads, improve current behavior
and future ideas. Keep usage low with fresh focused contexts carrying this full
goal and a better handoff. Do not mark the broad goal complete for a bounded pass.
The application-source audit is done; do not repeat it or old acceptance tests.
No new workers/timers for resource coordination. Continue concrete progress.

## Workspace, checks and boundaries

Canonical /home/niko/.internal/source/Villagers. Most source/docs/tools untracked,
README modified. NEVER reset/clean. Source0.4.1-dev UNRELEASED; no commit/PR/release/
deployment/service change by Lead. Preserve deployed0.4.0 and user clients/worlds.
MC26.3,Loader0.19.5,FabricAPI0.162.0+26.3,JDK25.0.5+8,Gradle9.8.1.
Mind schema7,work-sites schema5. Current286Java+10Python PASS,0failures/errors/skips,
last build82885exit0 at evidence/2026-11-07/refuge-floor/backoff/build.log.
Latest source passed full286Java+10Python; no new unit tests for diagnostic maps.
Normal checks: `tools/test.sh --no-daemon --max-workers=2`.
Dev JAR SHA1e35d0a4564b57c6cd1ee47f2b5be8791c55bae29fb72b386bbe3f54ad2fee8c.
Source+installed0.4.0 SHA4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0.
Installed /home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar.

User chose limited autonomous building using real materials, protecting player
structures. Do not ask again. Read design/terrain-permission.md before edits.
No blanket logging/demolition/chest looting/free supplies/block-type ownership
inference. Finite grants/protection schema5:8farms/81cells each,8stations,8forestry,
32zones/4096blocks each,128KiB metadata. Protection wins; outside edits revoke;
dirty-session marker revokes edit grants but retains protection. Raw chunk editors
can bypass native setBlock hooks: revoke before unsupported edits. No global
crash atomicity. Resource paths still require full permitted routes; do not widen
those guards by analogy with bounded partial home-return paths.

## Resource custody — no active hold or new worker

All latest owned chains terminal, no pending command, active owned Java/build/game/
local-model job or automated build/release timer. Native budgets use existing tick
callbacks. Earlier audit_domain,audit_tools_trials,visible_current_source finished;
do not wake them just for status.005closed/spent,006expired-unused,007earlyreleased
09:54:54. No hash/unit/model/engine/successor/grant001/inference approval. Historical
resource evidence immutable. No shared-scope ownership inference or PID exceptions.

Original adaptive601856/10936249,601946/10936303,602326/10937021 ABSENT; frozen
wrapper601857/10936249 ALSO ABSENT since at least17:53. No Lead stop/signal or cause/
replacement inferred. Head received changed-wrapper report18:09. Latest persistence
capture: frozen daemon601990/10936325 and game602372/10937054 still exact. Preserve
these, all user clients/worlds,Modrinth,Buddy and native Niko. Other services are
not exclusive custody or admission. New coordination needs fresh exact ownership
and explicit finite agreement; no periodic resource polling.

Latest captured native chains, all exact identities terminal:
974567/15518475,974699/15518532,974866/15519033;
980593/15568378,980698/15568437,980883/15568956;
984484/15599701,984586/15599768,984708/15600029;
989551/15649677,989695/15649734,989884/15650239.
Short backoff36106exit0 finished BEFORE census; exact tuples unavailable. Explicit
refuge-floor/backoff/custody-gap.json: no fabricated tuples, vacuous empty-chain
proof, inherited exception or relaunch just for missing metadata. Known-runtime
lock/no-live-client and original terminal0 are separate evidence, not an exact
historical census. All future resource admission needs fresh independent exact data.
Other native original handles48332,64985,63551,12867 directly exited0. Final accepted
source native handle12867, fixed world runs/refuge-final/play stopped62043. No active
owned job/pendingcommand/timer. Build handles27152,42914,15207,82885 exited0.
Metadata: evidence/2026-11-07/{refuge-diagnosis,refuge-floor}/custody/results plus
refuge-floor/{persistence,backoff,final-acceptance}. Original user worlds untouched.
Head <t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/2a6b199a-0bbd-4884-887d-119d63e29b97>.
Lead e551a259-e755-4223-9927-474c3c52b7ca. Recruitment completed0.4.0, no old jobs/
timers; coordinate before altering its bed/home guards:
<t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/9d5b9bc5-09e6-4ff5-99c1-a09d3982e712>.
Read a named thread before answering about it. Do not infer a deployment grant.

## Latest source and acceptance — actual covered standing goals

Read design/refuge-standing.md and evidence/2026-11-07/refuge-floor/README.md.
61recorded-data/source assertions pass. Source0.4.1-dev stays UNRELEASED.

Native diagnostic idle probe58782: collisionFree Juniper(-281.5226,70.875,727.9137),
home(-269,75,733). Bedpath targets(-269,76,733),canReachfalse,15nodes,end(-269,75,731)
OUTSIDE northwall. SavedbedY75,roof top slabsY77,southdoors. Diagnostic-only baseline
also later entered/slept at62038: DO NOT claim a causal survival-rate A/B improvement.
The prior guessed within28 partial-policy cause was not established and was NOT
changed. This is a better complete destination, not permission to accept bad paths.

RefugeRoutes retains working bedpaths, otherwise tries a covered standing goal
within2.5 of same permitted bed: full standing box clearance, safe dry sturdy support,
roof, native bed sight, loaded chunks and exact-end complete1..128node path. Max75
cells/3homepathqueries per plan, cached valid goal rechecked/reused. Existing40tick
planning, danger/crossedthreat/homebounds/stalls/door/stamina/arrival rules remain.
No actor/block/velocity setter, newhome/bedclaim/material/terrain authority. SurvivalPolicy
and TraderRecruitment byte-identical to diagnostic baseline. Failed floor searches
back off200simulation ticks in same cell; movedcell/home/cover allows immediate retry.
Ordinary bed/threat/guardian/escape checks continue, cached valid floor goals unaffected.

Development-only bounded lastRefugePlan + lastFloorRoute retain actual values/counters,
not a growing log. Selected-actor watcher captures observed-departure previous/current
states; departure could be unload and needs separate lifecycle proof. That new branch
was not exercised in these cohorts. No schema/worker/external timer/model addition.

Both controlled pre-death comparisons copy identical278files from harvest-bootstrap
57237; they NEVER overwrite/merge farm-regrowth's retained-death branch. Only readonly
watch/status/oneidleprobe, no supplies/actor/body/clock/growth/RNG/action/terrain setters.
Earlier fixture interventions remain historical background, not generated baseline.

Diagnostic48332exit0→62038/save62041,mean2.343257ms. Initialfloor64985exit0→62038/save62043,
mean3.248798: JuniperfloorRoutes4,lastcomplete34nodes60839to(-270,75,734), then actual
covered/sight/proximity SHELTER and sleep,0route stalls/partialroutes. Night63551exit0,
4800ticks62044→66844/save66847,mean3.506383: Juniper19HP/food3,alive/sleeping, all5live
past old separate-branch death66151. No repeated/full-night/population survival proof.

Night revealed stuck Nell repeated384queries/128homeplans,9600candidatechecks fromsame
cell. Finalbackoff36106exit0,400ticks66848→67248/save67252,mean5.24418: unmovedsame1node
bedpath,14queries/10plans,150checks. Lower repeated work, NOT measured tick-time speedup.
Its exact census gap is explicit above. Finalsource positive12867exit0→62038/save62043,
mean3.034036: JuniperfloorRoutes4,9queries/5homeplans,225checks; lastcomplete6nodepath60645
to(-271,75,734), actualsleepatbed(-268.5,75.5625,733.5),0stall/partial,5live/7olddead/0missing.

Source-main.zip binds firstfloor/night; source-backoff.zip binds finalbackoff+positive
and CURRENTsource, SHA219b75ea5f248673b4527280d84dd19db0414b241670385f7fe2d4ce03b942cc.
Earlier pinned source differs: do not re-run old source-bound verifiers on newer main.
Native means are snapshots, NOT scale or tail-latency proof. Deployed0.4.0 unchanged.

## Latest evidence — farm meals and failed overnight defense

Read evidence/2026-11-07/farm-regrowth/README.md,verification.json and relevant
raw snapshots only.46recorded-data/source checks pass. No new main Java changes.

runs/farm-regrowth/play copied278stopped files from harvest-bootstrap/play at57237.
Only read-only watch/status, no food/seed/tool/crop/terrain/growth/actor/body/age/
clock/RNG/action setter. Earlier supplied fixture resources remain historical
background, not generated-baseline proof. Parent remains unchanged; no histories
merged. Main23286exit0 directly,12000ticks57238→69238/final69242. Same-world81323
exit0 directly,400ticks69243→69643/final69646. Mean tick snapshots2.97398/3.348638ms
for five then four actors, NOT population capacity or tail-latency proof.

Fern began with two actual earned beetroots, four earned beetroot seeds,12wheat
seeds,wheat1,bread1. At58731 he donated old bread to existing child Fern-589b;
both native memories/counters and childfood13 confirm this, NOT farm-derived food.
At61211 beetroot1 and actual beetroot meal note; at69238 beetroot0 with TWO meal
notes, food19. Restart retains consumed stock and hoe10/250; food18 afterward is
ordinary metabolism. This proves actual earned crop consumption, NOT sustained
food. No new harvest or bread crafting. Seeds12/beetseeds4/wheat1 remain.

All8farmland moisture7,6nearbywater,clear overheadcolumns to100. Saved wheat
(-230,72,751) naturally5→7; other wheat ends3,4,5,3,4 then one3→4onrestart.
Both beetroot replants remain defaultage0 (saved Name-only omits defaultproperties).
End native night=true; food work suspends at night, so unharvested ripe wheat then
is not a selection-bug diagnosis. Tables/earlier paid propagule remain inherited;
no autonomous regrowth/second wood cycle proven.

Zombie killed Juniper231e4c00-19df-4be5-a5ba-5ebc6d29e27f at66151 near
(-273.96,75,729.76). Log explicitly says slain by Zombie; lifecycle terminal death/
mob66151 survives restart. Last persistedfood3, NOT starvation cause. Original
cohort now3alive/8dead/0missing, plus existingchild gives4living. NEVER resurrect,
restore dropped resources or reset this world. Lifecycle historic alive:true/HP20
are stale snapshots; use terminal event/current loaded list. No new birth.

Before attack,61211: Juniperbody(-292.4076,73,738.2492),food7,HP20,home(-269,75,733),
19homePathPlans,0partialHomeRoutes,intentNORMAL,exploring,rest reports bedunavailable.
Selected saved bed is lime head facingeast at(-269,75,733),roof top slab y77.
A roof does NOT establish a reachable safe route. No native pre-death path node
receipt yet explains the failure. Do not claim a causal fix from this alone.

## Next focused work — continue real food production, inspect Nell without forcing rescue

Do not repeat audits or accepted floor-route/centering/wood/table/beetroot cases.
Current retained-death continuation runs/farm-regrowth/play is still stopped69646,
4living (Juniper dead66151), wheat(-230,72,751)ripe,beets0,seedbank4,wheat1,wheatseeds12.
Apply current final source only by an ordinary isolated continued observation; preserve
all world history and user clients. Go through natural dawn toward another actual
harvest/native bread/meal/reserve refill. No growth/time/metabolism/RNG adjustment or
new supply. If crop area/soil is inadequate, inspect actual bare ground/irrigation
before proposing useful finite paid expansion under existing protection rules.

Separately useful during that observation: one idle native routeprobe for Nell
e358e923-f9d2-4d5b-a04b-b2531c8f02d9. Latest comparison position(-256.828629,68,779.387396),
home(-236,72,766),1nodepath,canReachfalse,0centeringattempts,occasionalESCAPE/threat.
Collision/water/support cause UNKNOWN. Inspect real body/terrain/paths before any
rescue fix. Respect existing idle/awake/nontrading probe refusal; never stop active
navigation merely for inspection. A different home goal does not repair a bad start.
No teleport, restored resources, free weapons, blind partial route relaxation,
home-bound widening or unauthorized excavation/construction.

Do NOT continue a pre-death comparator over the retained-death world. Positive separate
worlds are refuge-floor(playstopped67252) and refuge-final(play62043), each5living,
Juniper alive because those histories branched BEFORE his other-world death. Never
merge inventories, genealogy, bed claims or other same-UUID histories. The retained
branch286filemanifest stillmatches refuge-floor/retained-death-world.json.

Broader work stays sustainable food/wood income, paid expansion/buildings/material
logistics, parent care, defense, language/history/adaptation and population performance.
Keep full goal active; pass it and sharper actual evidence into the next context.

## Earlier verified behavior — preserve, do not repeat

Harvest bootstrap: actual native two beetroot harvests, one paid from native loot,
one from earned stock, hoe8→10, two replants, restart. The prior grain-only early
return diagnosis was WRONG: default crop search already includes all four crops.
VillagerController.stockCropTarget is unchanged. Actual missing pre-existing seed
was corrected by HarvestMaterials: stock first or actual sampled drop, detached
full-slot CAS/one-use receipt, native loot once at reach,40tick attempt/200unfunded
backoff, loaded sight/reach/protection/crop/soil/tool rechecks, real overflow/wear.
One last receipt resets/overwrites normally; short mind notes may evict. Details:
design/harvest-replant.md,harvest-bootstrap/README.md. Historical harvest source archive SHA
116305c66be991839acfbf979bafae32f7ed5edcdd5b7228fa80323092b8c56d; current refuge source
is bound separately above.
Do not rerun older pinned verifiers after future main edits. Old1537 terminal output
was lost; log+exactterminal used honestly. Both latest handles have direct exit0.

Native forestry wildseed→3approvedleaves→1separateapproveddryroot→2logs→paidpropagule,
then earnedlog→native4planks→tableitem→paidtable(-281,75,745),protection/restart/no
replay passed. Juniper carried remaininglog1/root1 before death; do NOT restore them.
Propagule(-269,72,723) onmud71,AGE4/STAGE0,nonhanging/nonwaterlogged. Naturalgrowth
unproven. Existing no-job scan fast path, hungry food>6 wood-work guard remain.
Read forestry-access/README.md,earned-station/README.md only if that becomes scope.

Earlier real food help: understood WANT FOOD, voluntary donatedbread actuallyeaten,
16visiblepeers/100ticks,600requestbudget,400trip/80stall,home/sight/willingness guards;
planned approach/refusal native branches still open. Current parent→child donation
adds acceptance, not free nutrition. Source metabolism unchanged.

Ordinary existing child Fern589b708d-b768-4a61-a59e-910db084b079 born36379 toFern5ad3+
Darac7c3: mutualconsent,actual3bread/12units EACH,spare roofed native HOME,genes+27words,
freshlearning,reopen no duplicate. Minddeadline60379 separate nativeage6000cooldown.
Legacy collector child fields are not new births. Preserve all family guards.
Default user runs/play world lastchecked4survivors/7olddeaths/0missing/NOchild33104:
never merge fixture histories into it. Actual paid farm plot(-228,71/72,752) and
player-protection/revocation/restart verified; hay25defaultdisabled.

Ground rest80quietticks and bounded FloorCentering(currentcellcenter,drop<=1.25,
MoveControl/gravity40ticks) previously brought Juniper near home via partial routes;
actual bed arrival remained unverified. Do not repeat those acceptance cases or
permit partial RESOURCE paths. Nell remains canyon-stranded, no teleport/excavation.
RecoveryCheckpoint EMPTY-only,Animatica idle4worker graceful shutdown and boundedplay
normalquit pass; other branches remain open. No auto-dismiss of user menus.

Native boundedplay100..12000ticks, normal quit adds a few shutdown ticks. Capture
own exact chain early with /tmp/villagers-capture-owned.py TAG OUT(O_EXCL). Recover
confirmed live handles, never relaunch on lost observation alone. Pass this full
goal and sharper evidence-focused scope into the next context. Keep goal ACTIVE.
