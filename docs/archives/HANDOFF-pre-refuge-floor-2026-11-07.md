# Villagers Project Lead — focused continuation

Updated2026-11-07 after first earned beetroot meals, natural wheat maturity and
an overnight zombie death retained on restart. Previous checkpoint:
archives/HANDOFF-pre-farm-regrowth-2026-11-07.md. Source audit is complete.

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
last build73369exit0 at evidence/2026-11-07/harvest-bootstrap/build-final.log.
No main source change during farm-regrowth; unchanged suites were not rerun.
Normal checks: `tools/test.sh --no-daemon --max-workers=2`.
Dev JAR SHA9fc82af2d1a57dfb67e04e64096efd650497c3a85f9d3c317bfa0269c33be7e3.
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

Latest native chains, all terminal:
964014/15397275,964126/15397332,964344/15397846;
970934/15473196,971039/15473343,971170/15473604.
Metadata: evidence/2026-11-07/farm-regrowth/{custody-start.json,persistence/custody-start.json,results.json,persistence/results.json}.
Head <t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/2a6b199a-0bbd-4884-887d-119d63e29b97>.
Lead e551a259-e755-4223-9927-474c3c52b7ca. Recruitment completed0.4.0, no old jobs/
timers; coordinate before altering its bed/home guards:
<t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/9d5b9bc5-09e6-4ff5-99c1-a09d3982e712>.
Read a named thread before answering about it. Do not infer a deployment grant.

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

## Next focused work — diagnose real refuge access before another night

Read VillageSurvival, survival/SurvivalPolicy, HomeRouteProbe, relevant home-route/
floor-centering design and latest Juniper evidence. Candidate gap: usefulPartialReturn
rejects every partial home route when currentDistance<=28; at dusk an unreachable
full home route can then leave a resident NORMAL/exploring. Juniper had19homeplans
within24blocks, but actual partial-path availability is UNKNOWN. First obtain
native path/endpoint/covered/inside/blocked-route evidence. Keep read-only probes
from stopping live navigation; existing HomeRouteProbe has idle constraints.
Do not apply a speculative within28 relaxation without bounded native evidence.

Current continuation world is farm-regrowth/play, stopped69646, four living actors,
ripe wheat and no spare beets. Juniper is dead. Diagnose with remaining living
actors/offline terrain where possible. If a pre-death comparator is necessary,
create a clearly separate controlled branch from the preserved parent; NEVER
restore it over the retained-death branch or merge same-UUID histories/resources.
Preserve recruitment/player-bed protection, real door interaction, threat checks,
normal movement and stamina. No teleport, forced route/home, terrain demolition,
food-rate nerf, free weapons or artificial night/growth to manufacture a pass.

Broader work remains real sustainable farm output, useful paid expansion/irrigation,
material logistics, natural tree regrowth, general buildings, parent care, defense,
history/language/adaptation and population performance. Do not shrink the full goal.

## Earlier verified behavior — preserve, do not repeat

Harvest bootstrap: actual native two beetroot harvests, one paid from native loot,
one from earned stock, hoe8→10, two replants, restart. The prior grain-only early
return diagnosis was WRONG: default crop search already includes all four crops.
VillagerController.stockCropTarget is unchanged. Actual missing pre-existing seed
was corrected by HarvestMaterials: stock first or actual sampled drop, detached
full-slot CAS/one-use receipt, native loot once at reach,40tick attempt/200unfunded
backoff, loaded sight/reach/protection/crop/soil/tool rechecks, real overflow/wear.
One last receipt resets/overwrites normally; short mind notes may evict. Details:
design/harvest-replant.md,harvest-bootstrap/README.md. Source archive SHA
116305c66be991839acfbf979bafae32f7ed5edcdd5b7228fa80323092b8c56d binds current main.
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
