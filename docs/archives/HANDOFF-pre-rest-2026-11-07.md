# Villagers Project Lead — focused continuation

Updated2026-11-07 after protected,paid table construction,subsequent hoe production
and clean restart. Previous handoff is preserved in
archives/HANDOFF-pre-station-2026-11-07.md. Read this,then only the next task's files.

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
Mind schema7; work-sites schema2. Current251Java+10Python pass,0failures/errors/skips,
final build7763exit0. Normal check: tools/test.sh --no-daemon --max-workers=2.
Dev JAR SHA dfba7eaf3854928dc5556185caff45ee5c0ab623586914de076f66bafdb129e2.
Latest evidence: evidence/2026-11-07/station-production/README.md,verification.json,
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
601990/10936325,game602372/10937054 retain exact identities in latest station
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

## Latest verified chain: station then hoe

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
food aid/native WANT FOOD sharing acceptance; Juniper unreachable-home REST fallback
(fatigue1/lonely1,body(-234,73,766),home(-269,75,733)); Nell canyon-stranded
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

Continue protected wood acquisition/renewal,or actual food refill/rest/parent care/
scale. No blanket LOGS harvesting:player builds contain logs. Define finite explicit
resource authority,revocation,protection,native loot/capacity/reach/work costs and
regrowth evidence. Do not silently treat distant forest as known/reachable home
territory or teleport workers. Station placement and hoe production now work; do
not substitute repeated supplied-material passes for renewable income. No implicit
service deployment. Preserve failures,stopped backups and exact input ledgers.

Keep full goal active. Bounded observation VILLAGERS_PLAY_TICKS=100..12000 (0default
interactive);normal quit adds shutdown ticks. Capture launch-owned JVM identities
early with existing capture helper,never by shared scope. Pass this full goal and
a sharper handoff to the next context. Do not restart the completed audit.
