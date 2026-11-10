# Villagers Project Lead — focused continuation

Updated2026-11-07 16:03UTC. Source audit already done. Read this,then next-task
files only. Historical detail: archives/HANDOFF-pre-forestry-2026-11-07.md.

## Full continuing goal

Lead Villagers toward a performant Minecraft society that crafts its own things,
has children,protects its village and develops interesting histories,language,
relationships and adaptation. Audit code,coordinate threads,keep improving current
behavior and future ideas. Keep usage low; use fresh focused contexts carrying
this full goal and a better handoff. Do not mark the broad goal complete because
a bounded feature passed. The application-source audit is already complete; do
not repeat it. User permits useful research/experiments; no new workers for resource
coordination. Continue actual progress,not endless status reports or repeated passes.

## Workspace, validation and resource custody

Canonical /home/niko/.internal/source/Villagers; T3 cwd symlink resolves here.
Most source/docs/tools untracked, README modified. NEVER reset/clean. No PR,commit,
release or deployment by this lead. Source0.4.1-dev is UNRELEASED. Preserve deployed
0.4.0 and every user client/world/service. MC26.3,Loader0.19.5,FabricAPI0.162.0+26.3,
JDK25.0.5+8,Gradle9.8.1. Mind schema7,work-sites schema4.

Current273Java+10Python PASS,0failures/errors/skips. Latest manual build22680exit0:
evidence/2026-11-07/forestry/build-route-candidates.log. Normal check:
`tools/test.sh --no-daemon --max-workers=2`. Dev JAR SHA
25b55aad9cdd391834cd3fdae88007b32f4dde78bca33207acbdcea8db81830e.
Source+installed0.4.0 JAR SHA still
4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0.
Installed /home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar.

No active new owned Java/game/build/worker, pending command or owned automated
build/release timer. All current native chains terminal. Native budgets are existing
tick callbacks. Earlier audit_domain,audit_tools_trials,visible_current_source
finished; no new workers. Do not wake finished workers just to poll status.
005closed/spent,006expired-unused,007EARLY RELEASED09:54:54. No hold/hash/unit/model/
engine/successor/grant001/inference approval. Historical evidence immutable.
Never infer Java ownership from shared scopes,inherit PID exceptions,restart missing
clients or change other services. Any future coordination needs fresh exact custody
and explicit finite agreement. No periodic polling. Head:
<t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/2a6b199a-0bbd-4884-887d-119d63e29b97>.
Lead e551a259-e755-4223-9927-474c3c52b7ca. Recruitment completed0.4.0 and reported no
transient jobs/timers; coordinate before changing its home/bed guards:
<t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/9d5b9bc5-09e6-4ff5-99c1-a09d3982e712>.
Read a named thread before answering about it; no need to re-read completed audit.

Historical adaptive wrapper601856/10936249,daemon601946/10936303,game602326/10937021
remain ABSENT. No stop/pause/signal by this lead,no cause/replacement inferred.
Head already received changed-custody report. Frozen wrapper601857/10936249,
daemon601990/10936325,game602372/10937054 still same identities in latest
forestry/leaf-refresh/revocation/results.json. Preserve all user clients,Modrinth,
Buddy and native Niko. Buddy service historical identity is not exclusive custody
or current admission. No implicit deploy/restart authorization.

## Accepted terrain policy

User chose limited autonomous building using real materials,with protection for
player structures. Do not ask again. Read design/terrain-permission.md and
forestry.md. Unknown historic structures remain outside explicit edits. No blanket
logging,demolition,chest looting,free materials or block-type ownership guesses.
Finite grants/protection persist in schema4,128KiB:8farms/81cells each,8stations,
8forestry jobs,32zones/4096blocks each. Protection wins;outside writes revoke;
dirty-session marker revokes edit grants and retains zones;bad metadata disables
construction. Native Level.setBlock hook can be bypassed by raw chunk editors:
revoke before unsupported bulk edits. No global crash atomicity claim.

## Current forestry prototype and exact checkpoint

Read design/forestry.md and evidence/2026-11-07/forestry/README.md. Current source
adds ForestryWork,ForestryMaterials,WorkSites.Forestry schema4,explicit native
clearance,ConstructionLeavesMixin and FamilyProbe forestryRoute. Max8jobs,4logs,
4leaves; mature real seed first,then explicit leaves,logs,paid plant. Native empty
hand loot,full inventory receipt CAS,real retained seed,empty main hand,reach3/
native sight/loaded/home/body/soil/protection checks. Generic break/place stays off.
20ticks seed/leaf/plant;log native hardness*30,min20. No axes or general demolition.
Pending footprint shrinks;outside writes retire wholejob. Metadata crash scope
stays conservative,no world/entity/inventory global atomicity claim.

Planning:2batches/dimension/second shared,3native paths/batch,128nodes,100standing
candidates,100tick lookup,80tick physical stall,1200tick job/400tick failure backoff.
Cursor rotates valid stands across retries; read-only status includes actual
candidate count/batches/rejections/last path. No partial resource routes accepted.
Native LeavesBlock.tick identical-state refresh exception affects ONLY current
pending mangrove leaves at that exact native call. Changed states or direct
same-state writes still revoke; no blanket no-op relaxation,new timer or worker.

Resource seed(-269,74,721),supportleaf(-269,75,721),plant(-269,72,723),mudbelow71.
Original upperlogs(-270,76/77,722) remain unchanged and lost authority on explicit
move to lowerlogs(-271,74/75,722). Exact leaves(-270,73,723),(-269,74,723),
(-270,74,723) nowAIR in second fixture; planting cellAIR; BOTH lowerlogs untouched.

First independent runtime runs/forestry/play: original home-route244files copied
without simulation setters. Protected41336exit0,seed91062exit0,lowerlogs18317exit0,
clearance60532terminal (numeric exit unavailable after context recovery,NULL honest
record). Source270→271→272. Actual seed1,then1approved leaf; grant revoked by
native adjacent unchanged leaf refresh. Finished56612,food0/health7. PRESERVE;
no revival/repair/reset. Unconsumed status saved/removed underlock. One labelled
operator daylight in lower-logs; no new food/log/seed/tools/body/health setters.

Second independent runtime runs/forestry-refresh/play copied unchanged home-route
244files,not the starving fixture. Current source273tests native49873exit0 gathered
another actual wild seed. Initial clearance failed proximity/load; late retry never
executed,retained. Then66587exit0 added explicit3leaf authority and one labelled
operator time set day. Native3leaves removed,seed1 retained,logs0/plant0. Current
61964exit0 route-candidate continuation cutoff52306/final52309. Clean restart
preserved completed leaves+seed+quota2; zero seed/log/plant repeats. ForestryRoute:
3batches,0standCandidates,0paths/rejections,cursor0,lastPathnone. Actor Juniper
(-272.4919794172449,66.875,718.0038283506153),food3,health20,seed1,no logs.
Current grant891a2b06-ccd6-4889-9b38-5d85398d87e2,seedTaken=true,remaining2,
uncleared[]; no protected areas/stations/farms in this fixture. No new daylight,
resources or grants in route-candidate run. Last read-only status was queued after
exit,old report rejected; exact batch saved/removed underlock. Use budget-native.

Third runtime runs/forestry-leaf-revocation/play is a257-file copy of the current
second fixture.39742exit0,200ticks,cutoff52510/final52514. Explicitly added ONE
actual supportleaf to clearance,fresh grant52fc6478-0f13-4304-9ebd-9f10cf4d4a32,
then actual direct native same-state leaf write. Report52325 beforetrue/afterfalse/
unchangedtrue;grantretired,leaf physically unchanged. Source secondworld257files
unchanged. No screenshot before15sec threshold; evidence collector handles this
honestly. Do not reuse this retired copy as the current active forestry job.

All source/native experiments are now terminal. Latest exact owned tuples:
route898812/14566192,898927/14566254,899139/14566711;
revocation900161/14582638,900262/14582695,900406/14583189;none alive at capture.
All8phases preserved4originalsurvivors+existingchild,7olddeaths,0missing. Native
five-actor means roughly2.81–3.20ms,longer samples only;200tick hook5.07ms.
No scale or tail-latency claim. Existing pack/JOML/model warnings unchanged.

verify-results.py passed508recorded data/source checks (mostly hashes,not new unit
tests). source-main.zip binds current273source including cursor/diagnostics;
clearance/source-main.zip binds272;leaf-refresh/approved/source-main.zip binds273
before cursor. Earlier build logs/failure retained. Old native source pins differ
legitimately after edits. approved/route-inspection.json is STALE copied home-route
report; do not use it as fresh forestry diagnosis. Historical family files also
need gameTime filtering; budget-native is authoritative at cutoff. No native log
loot/paid plant/full regrowth/renewable income acceptance yet; only pure chain
accounting plus native seed/threeleaf/protection/persistence. Do not claim more.

## Earlier verified behavior — preserve; do not repeat acceptance

Default tools/play.sh uses runs/play/saves/VillageTrial: last independently checked
4survivors,7old deaths,0missing,NOchild,stopped33104. Never refill/reset/restore deaths
or merge fixture histories into it. Family fixture has ordinary child Fern-589b
589b708d-b768-4a61-a59e-910db084b079,parentsFern5ad3/Darac7c3,birth36379. Actual
mutual consent,3bread/12units EACH,spare roofed native HOME,genes+27words inherited,
fresh learning and clean reopen/no duplicate. Mind cooldown60379,native adult-age
6000countdown separate. Read family-company/README.md only if needed.

FoodSupply15held nutrition independent of reproductive readiness; real components,
seeds retained,raw wheat<=9. MealPolicy normal breadfood<=15,injury/urgent exceptions,
32tick meal cadence. Actual reserve refill/sustainable yield remains unverified.
FarmExpansion paid1new plot(-228,71/72,752),seed12→11,hoe6→7;nativeprotection,
revocation/restart pass.8fixtureplots,6wheat/2beetroot;25hay default-disabled.

Native real held wood→planks→sticks→hoe and separate finite table construction
passed conservation/protection/revocation/restart. These used labelled supplied
wood; no claim of autonomous income. Table cost4realplanks,hoe native recipes;
2logs→table+hoe+2sparesticks. See station-production/README.md if needed.

Ground rest now requires80quiet safe ticks and permits recovery when bed blocked;
Juniperfatigue1→.4335→.325 persisted. Clipped-start FloorCentering uses only current
cell centre,drop<=1.25,normalMoveControl/gravity,40ticks;no setPos/velocity/block edits.
Native1success+3calm partial HOME routes brought Juniper home-distance47.8→25→16.86.
Do not repeat recovery or accept partial RESOURCE paths by analogy. Actual bed
arrival still unverified. See home-route/README.md only if relevant.

Other open work: native food aid/parent care,adequate farm yield/reserve refill,
distant station memory,Nell canyon-stranded(no teleport/excavation),full defense,
sustained adaptation and population/tail-latency acceptance. RecoveryCheckpoint
EMPTY reopen passes;nonempty/extinction/partial/failure cases pending. Animatica
0.6.2+26.3 idle4worker2sec graceful drain passes;busy/absent/future cases pending.
Explicit bounded-play resume never auto-dismisses user menus;positive paused path
unverified. Keep existing metabolism,consent,cooldown,kinship,food/home guards.

## Next focused work

Do NOT restart the audit, clipped-start recovery, station/hoe supplied-material
passes or the now-verified three-leaf experiment. Diagnose zero admitted stands
for the exact lower log targets using a read-only bounded native report: actual
pose/standing eye height, each AIR/headroom/sturdy/home/reach rejection, and first
native clip obstruction position/state. Current cursor cannot fix zero candidates.
Do not assume cleared leaves mean root-free sight. Never clear unapproved roots,
move residents, widen home territory or force partial unsafe paths for a pass.

Prioritize actual food resilience/aid before long further trials: optional forestry
still uses general mayWork(health>0), which allows a starving resident to cut wood.
Earlier continued fixture reached food0/health7; current independent fixture is
food3/health20. Existing provisionFood can return no supply and fall through to
optional work. Fix that survival priority and validate native food acquisition/
sharing from real sources without feeding fixtures and claiming sustainability.
Read actual nearby inventory/food state before choosing the next narrow step.

If a genuinely accessible finite log set exists, require fresh explicit authority
for a replacement set, retained real seed and nonincreasing quota. Then validate
native log loot, paid planting, clean restart without replay, eventual natural
growth and another cycle. No supplied-log stand-in for income. Broader buildings,
parent care, defense and scale stay on roadmap. Preserve all partial/failure
evidence and the user-deployed0.4.0. Source0.4.1-dev remains unreleased.

Keep full goal active. Bounded observation VILLAGERS_PLAY_TICKS=100..12000 (0default
interactive);normal quit adds shutdown ticks. Capture launch-owned JVM identities
early with existing capture helper,never by shared scope. Pass this full goal and
a sharper handoff to the next context. Do not restart the completed audit.
