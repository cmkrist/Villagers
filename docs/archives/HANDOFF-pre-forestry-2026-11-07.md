# Villagers Project Lead — focused continuation

Updated2026-11-07 after native clipped-start diagnosis,physical movement recovery
and restart. Previous handoff: archives/HANDOFF-pre-home-route-2026-11-07.md. Read this,then only the next task's files.

## Full continuing goal

Lead Villagers toward a performant Minecraft society that crafts its own things,
has children,protects its village and develops interesting histories,language,
relationships and adaptation. Audit code,coordinate threads,keep improving current
behavior and future ideas. Keep usage low; use fresh focused contexts carrying
this full goal and a better handoff. Do not mark the broad goal complete because
a bounded feature passed. The application-source audit is already complete; do
not repeat it. User permits useful research/experiments; no new workers for resource
coordination. Continue actual progress,not endless status reports or repeated passes.

## Workspace,versions and custody

Canonical /home/niko/.internal/source/Villagers; T3 cwd symlink points here. Most
source/docs/tools untracked,README modified. NEVER reset/clean or overwrite releases.
This lead published no PR/commit/release. Source0.4.1-dev is UNRELEASED; preserve
user-deployed0.4.0. MC26.3,Loader0.19.5,FabricAPI0.162.0+26.3,JDK25.0.5+8,Gradle9.8.1.
Mind schema7; work-sites schema2. Current262Java+10Python pass,0failures/errors/skips,
final build55558exit0. Normal check: tools/test.sh --no-daemon --max-workers=2.
Dev JAR SHA e72327176c2026f803df5a80ff4ac94766413d9a8c4ba764be5e0a53e88921e1.
Latest evidence: evidence/2026-11-07/home-route/README.md,verification.json,
source-main.zip. Older source pins naturally differ after later intended edits.

No active new Java/game/build/worker,pending command or automated build/release timer.
Native budgets are existing tick callbacks,not external timers. Pack/model/JOML
warnings remain; full compatibility,population capacity and tail latency unproven.
Earlier native audit_domain,audit_tools_trials,visible_current_source finished;
roadmap thread hit provider limit. Recruitment completed0.4.0 and reported no
transient jobs/timers. Coordinate before changing its home/bed guards:
<t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/9d5b9bc5-09e6-4ff5-99c1-a09d3982e712>.
Deployment evidence retained in recruitment-integration/. Source+installed0.4.0 SHA
4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0 independently
still matches. User-owned minecraft-buddy-server.service was PID802933/13174911,
Restart=on-failure/5s: historical metadata,not current identity/exclusive custody.
No implicit deployment/restart/service update authorization from this lead.

005closed/spent;006expired-unused;007EARLY RELEASED09:54:54. No active hold/model/
hash/unit/engine/successor/grant001/inference approval. Historical receipts immutable.
Never infer shared-scope Java ownership or inherit PID exceptions. Future admission
needs fresh exact metadata and explicit finite agreement; no periodic polling.
Head: <t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/2a6b199a-0bbd-4884-887d-119d63e29b97>.
Lead thread e551a259-e755-4223-9927-474c3c52b7ca.

Historical adaptive wrapper601856/10936249,daemon601946/10936303,game602326/10937021
are ABSENT; no stop/pause/signal from this lead,no cause/replacement inferred.
Head received one changed-custody report. Frozen wrapper601857/10936249,daemon
601990/10936325,game602372/10937054 retain exact identities in latest home-route/
persistence/results.json. Preserve them; NEVER restart missing clients or upgrade
their directories. Old-source20TPS/historical48000tick trials,not current proof.
Preserve Modrinth,Buddy,native Niko and every other user's clients/worlds/services.

## Accepted terrain boundary

User chose limited autonomous building using real materials,with protection for
player structures. Do not ask again. Read design/terrain-permission.md and
station-production.md. No blanket breaking,demolition,chest looting,free materials
or block-type ownership guesses. Player-bed registry is not whole-build ownership.
Unknown historic structures stay outside explicitly approved edits.

WorkSites/WorkSiteStore/ConstructionSites schema2:128KiB,8farms/81cells each,
8station grants,32protected cuboids/4096blocks each. Schema1 migrates without station
authority. Dirty-session marker revokes all edit grants,retains protection;invalid/
failed metadata disables construction. Synchronous bounded durable control writes,
including completion/outside revocation; no polling IO. No global crash atomicity
across Minecraft chunks/entities/inventories/metadata.

Native Level.setBlock HEAD hook retires grants BEFORE outside writes,even same-state
attempts. Farm footprint soil/crop/headroom; station11cells:target/support/headroom
plus four standing/headroom pairs. Protection wins,overlapping edit footprints
rejected,completion retires grant. Player edits not cancelled. Raw chunk writers
can bypass hook: revoke before unsupported bulk edits.

Commands: /villagers site farm 0..4 under/around operator feet; /villagers site table
at operator feet; site protect x1 y1 z1 x2 y2 z2; site list/remove UUID. Gamemaster
player required. Station needs air/sturdy support/no farmland or fluid,headroom,
2access cells,no beds/doors/block entities within horizontal2/vertical1. Reach,
sight,home,load,collision,authority and actual inventory rechecked. Generic break/
place remain false. Broader buildings and material renewal remain future work.

## Earlier verified chain: station then hoe

WoodCrafting native recipes: log→4matching planks;2planks→4sticks;2planks+2sticks→
1woodenhoe;portable2x2 four planks→actual table item.11wood families inclstripped.
Exact native item/count/no byproducts,detached reservations,full-inventory CAS and
output capacity; stale/replay denied. Failed later steps retain intermediates.
Material gifts consume real items; production supplies nothing automatically.

StationProduction safe adult needing table for wheat or missing hoe/held seed.
Explicit assigned jobs:32horizontal/8vertical broad phase,28home bound,max8sites,
no terrain scan. Shared2path batches/dimension/sec,max3paths/batch,native path bounds,
40retry,80stall,400continuous trip/400backoff,100scan/backoff,20workticks/recipe or
placement. ToolProduction works at existing/newly built table. Survival/player/
food priorities win,no elective utility credit,no new thread/per-poll IO. Builder
caches new table; long-distance station knowledge after reload remains open.

Exact202-file construction-world copy -> runs/station-production/play. Inputs:
daylight,observer teleports,withdraw prior OPERATOR table(-226,71,744) into observer
slot9,grant/protection,two logs+one seed gifted to Dara. No villager movement,hunger,
age,mind,decision or forced production. Earlier supplies remain labelled.
Protected95482exit0:600ticks40143→40743/final40747; actual same-state revocation,
regrant+protection means0recipes/builds,logs2retained. Only protection removed:
98862exit0,1200ticks40748→41948/final41952,0build because actor moved beyond old8block
lookup. Failure retained in approved/. Fixed explicit job lookup,same world/no
newinputs:77189exit0,1600ticks41953→43553/final43557;2station recipes/1table+3tool
recipes/1hoe,2sparesticks/1seed. Raw save:newtable(-233,72,746),oldtableAIR,hoe0/59,
bread3 after normalmeal,grant retired. Read-only11639exit0,400ticks43558→43958/final
43962:same block/tool/materials,counters0,no duplicates.5live/7dead/0missing;
mean4.05–5.42ms snapshots only. Source construction202files and deployed0.4.0 unchanged.
All four exact process chains terminal in phase results; latest859422/13908892,
859523/13908958,859640/13909273 terminal. Source archive/current verifier retained;
older range file reconstructed and exactly matched its old SHA. No need repeat
these runs without a new concern. Supplied wood is NOT autonomous forestry.

## Earlier prerequisite: rest recovery and forestry survey

Read design/rest-recovery.md,design/forestry.md and rest-recovery/README.md.
RestProgress requires80consecutive quiet/safe ticks; travel,quarter-block movement,
unsafe footing,other-work gaps,location changes,rollback or replay give no credit.
Original0.12rest effect/ordinary fatigue rate unchanged. Valid unreachable/occupied
bed now permits existing ground rest; ownership/sight/native sleeping enforced.
Rest bed paths use shared work admission,40retry,WorkJourney bounds/backoff,128nodes.
VillageSurvival NORMAL no longer bypasses40tick planning deadline; home/cover,
danger and dusk invalidate. Calm displaced residents may take finite native partial
returns only if2..128nodes/end>=1closer/loaded/within max(28,distance)+8. Positive
partial branch pure-tested but NOT taken natively; ordinary home territory unchanged.

Exact221-file station-world copy -> runs/rest-recovery/play. NO new simulation
inputs:only read-only watch/status,all prior fixture inputs still labelled.
8238exit0:1200ticks43963→45163/final45169,Juniper fatigue1→0.4334375,5ground bouts,
0bed bouts,2bed-path rejects,30refuge/home plans. Body stays(-234.206902,73,766.7),
home(-269,75,733),health20,inventoryempty.87520exit0:400ticks45170→45570/final45576,
first LOADED report45207 fatigue0.43375/ground0 proves persisted recovery; final
fatigue0.325/ground1,1bed reject,10plans. Initial watch45185 had no loaded actors;
do not use it as a fatigue observation.5live/7dead/0missing,mean4.57/5.03ms snapshots,
source221files and deployed0.4.0 unchanged. Source archive/verification retained.
First865756/14026938,865893/14027006,866152/14027647 sampled live/terminal. Reopen
sample was late/jobs[]; never infer its PID. Exit0/save/free runtime lock/no tagged
client establish terminal. No new active jobs/timers/workers/pending commands.

Juniper recovered fatigue but DID NOT MOVE HOME:partialHomeRoutes0,home still
unreachable. Do not mark return/forestry complete. Survey stopped station world:
56recorded chunks,Y64..111,119near-home log columns allMANGROVE;only Juniper home
range includes inspected columns,other adults none.64nearby hanging propagules,
including mature age4,may provide real seed material. Raw chunks/reports under
forestry/terrain-survey/. These are bounded observations,not ownership or path proof.
Do not implement a short straight oak/dirt-root assumption for these root/branch
mangroves. Next inspect ACTUAL native route topology from Juniper to owned bed,
then finite protected resource/propagule/planting jobs without new supplied logs.

## Latest result: native clipped-start recovery

Read home-route/README.md,verification.json and design/floor-centering.md. Exact
229-file rest-world copy -> runs/home-route/play,NO new resources/clock/actor/
terrain/action setters. Read-only idle HomeRouteProbe captured a real obstruction:
Juniper body(-234.206902,73,766.7),height1.95,onGround,top74.95 intersecting2topslabs
atY74.5. Native home path and4local paths each1node/canReachfalse;doors enabled.
Not a distant-forest/bed-ownership problem:body was clipped under the roof beside
stairs. Probe only inspects idle residents,may warm path caches,neverassignspaths.

FloorCentering/FloorCenteringPolicy target ONLY centre of CURRENT horizontalcell,
down<=1.25,width<1,sturdy nonhazardous unchanged support,actual destination noCollision.
Calm/awake/standing/notriding/nottrading/grounded/idlenav/actualblockcollision only.
Normal MoveControl(.35) and gravity,NO setPos/velocity/pose/collision/block edits.
Max40ticks,probe40,no-candidate200,afterattempt400backoff. Success requires real
collision-free grounded lower body. Support/obstruction/danger aborts. Source has
no new IO/schema/thread/external timer. Four pure geometry bounds tests added.

88305exit0:800ticks45577→46377/final46381,probequeuedtoo late;exactunusedbatchremoved
underlock,runretained.15058exit0:800ticks46382→47182/final47186,inspection46535,
bodyunchanged.Successful handle75151exit0:
1200ticks47187→48387/final48391,1centeringattempt/1success/0failures,3calm partial
home routes. Body(-245.083396,72,741.562533),home distance25.01 (initial47.80).
55186exit0:400ticks48392→48792/final48796,0repeat centering,continuedmovementto
(-253.337298,73,740.427937),home distance16.86. Same owned home(-269,75,733),5live/
7dead/0missing. Actual bed arrival/forest resource reach NOT yet proven. Positive
partial-return branch is NOW native-verified. Later pathprobe correctly refused
while actively navigating; do not stop movement just for an idle query.

All4chains sampledlive/terminal; latest878977/14186394,879080/14186468,879197/14186792.
Mean4.86–5.30ms/fiveactors snapshots only. Source229files/deployed0.4.0 unchanged.
probe-source.zip binds258testdiagnostic;source-main.zip binds262testproduction.
Earlier source recovered from prior archive and exactly hash-checked. One probe
getter compilation error retained; corrected build and production build pass.
Do NOT repeat this recovery experiment or keep diagnosing the old fixed cell.

## Earlier verified behavior and important gaps

Default tools/play.sh uses runs/play/saves/VillageTrial:last independently verified
4survivors/7old deaths/0missing/NOchild,stopped33104. Never refill/reset/restore deaths
or merge fixture histories into it. Tools/table/nursery were labelled inputs.
Family fixture runs/family-company/play has ordinary child Fern-589b UUID
589b708d-b768-4a61-a59e-910db084b079,parentsFern5ad3.../Darac7c3...,birth36379 after
matching live mutual consent.3bread/12units EACH,real spare roofed bed/native HOME,
ordinarygen1/godChildfalse,genes+27words inherited,fresh learning0. Save/reopen stable,
no duplicate child. Mind birth deadline60379,nativeAge6000 countdown separate.
See family-company/README.md and persistence/ for exact parents/inheritance evidence.

FoodSupply15held nutrition independent of reproduction/adult cooldown/breeding
config; real components,seed retained,raw wheat<=9,body/day/fatigue gates. MealPolicy
fits nutrition,normal breadfood<=15,injury/urgent exceptions,32tick budget. No hunger/
food/growth-rate cheating. Household-food showed own-food intent/access during
cooldown,not sustained refill. FarmExpansion paid one new plot(-228,71/72,752),seed
12→11,hoe6→7,protection/revocation/restart pass.8fixture plots/6wheat/2beetroots;
renewable output unmeasured.25hay default-disabled. Strict real tool/seed/native
loot/replant rules. Earlier detailed evidence linked in archived handoff.

Gaps: autonomous protected wood acquisition/regrowth/tool renewal; persistent distant
station knowledge; adequate farm yield/actual reserve refill; parental care/proactive
food aid/native WANT FOOD sharing acceptance; Juniper actual bed arrival/resource paths
(ground rest and clipped-start recovery now work,lonely1,home(-269,75,733)); Nell canyon-stranded
(-256.83,68,779.39),no teleport/excavation. Consent/cooldown/kinship/real parental
food/spare-home protections remain. Real defensive melee/doors/survival exist;
full defense and sustained social/economic adaptation acceptance still open.

RecoveryCheckpoint64tickets/128KiB,whole64neighborhood loaded/ticking,no force-load,
coalesced existing writer:EMPTY reopen passes;nonempty/extinction/partial/failure/
crash cases pending. Exact Animatica0.6.2+26.3 idle4worker2sec graceful drain passes
native closes/reopens;busy/absent/future versions pending. Explicit bounded-play
resume handles PauseScreen/no overlay only,never automatically dismisses user menus
or affects other clients; positive paused path unverified. No OS input/focus
injection. Overview screenshots are not close-up action proof.

## Next focused work

Implement finite protected mangrove propagule/log/planting jobs and validate actual
resource paths from the now-mobile Juniper. Quiet rest AND clipped-start recovery
are verified; do not repeat them. Bed arrival is not proof of forest access either.
Preserve real food refill/parent care/defense/scale on the broader roadmap. No blanket LOGS harvesting:player builds contain logs. Define finite explicit
resource authority,revocation,protection,native loot/capacity/reach/work costs and
regrowth evidence. Do not silently treat distant forest as known/reachable home
territory or teleport workers. Station placement and hoe production now work; do
not substitute repeated supplied-material passes for renewable income. No implicit
service deployment. Preserve failures,stopped backups and exact input ledgers.

Keep full goal active. Bounded observation VILLAGERS_PLAY_TICKS=100..12000 (0default
interactive);normal quit adds shutdown ticks. Capture launch-owned JVM identities
early with existing capture helper,never by shared scope. Pass this full goal and
a sharper handoff to the next context. Do not restart the completed audit.
