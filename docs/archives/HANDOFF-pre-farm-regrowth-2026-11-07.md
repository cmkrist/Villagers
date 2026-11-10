# Villagers Project Lead — focused continuation

Updated2026-11-07 after native first beetroot income→harvest-funded replanting,
second stock-funded replant and restart retaining yield/wear. Audit complete.
Read this,then next-task files only. Previous checkpoint:
archives/HANDOFF-pre-harvest-bootstrap-2026-11-07.md. That previous handoff's
grain-only diagnosis was wrong and is corrected below; do not implement it.

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
JDK25.0.5+8,Gradle9.8.1. Mind schema7,work-sites schema5.

Current286Java+10Python PASS,0failures/errors/skips. Latest manual build73369exit0:
evidence/2026-11-07/harvest-bootstrap/build-final.log. Normal check:
`tools/test.sh --no-daemon --max-workers=2`. Dev JAR SHA
9fc82af2d1a57dfb67e04e64096efd650497c3a85f9d3c317bfa0269c33be7e3.
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
Head received changed-custody report. Frozen wrapper601857/10936249 is now ALSO
ABSENT at17:53,18:03 and natural18:09checkpoint; no lead stop/signal or cause inferred.
Frozen daemon601990/10936325 and game602372/10937054 retain exact identities.
Head notified once at18:09; harvest-bootstrap/custody-checkpoint.json holds exact
metadata. Preserve all user clients,Modrinth,
Buddy and native Niko. Buddy service historical identity is not exclusive custody
or current admission. No implicit deploy/restart authorization.

## Accepted terrain policy

User chose limited autonomous building using real materials,with protection for
player structures. Do not ask again. Read design/terrain-permission.md and
forestry.md. Unknown historic structures remain outside explicit edits. No blanket
logging,demolition,chest looting,free materials or block-type ownership guesses.
Finite grants/protection persist in schema5,128KiB:8farms/81cells each,8stations,
8forestry jobs,32zones/4096blocks each. Protection wins;outside writes revoke;
dirty-session marker revokes edit grants and retains zones;bad metadata disables
construction. Native Level.setBlock hook can be bypassed by raw chunk editors:
revoke before unsupported bulk edits. No global crash atomicity claim.

## Latest crop-production checkpoint — actual missing seed corrected

Read design/harvest-replant.md and evidence/2026-11-07/harvest-bootstrap/README.md.
The default AdventureActions crop search already includes all four crops; false
only excludes wheat. VillagerController.stockCropTarget is UNCHANGED. The earlier
grain-only early-return diagnosis was wrong. Actual blockers were no beetroot
seeds in either equipped adult, the old pre-existing-seed gate and physical local
discovery. Native Fern jobsite actually equals(-226,72,752); Dara(-251,75,736).

HarvestMaterials reserves one matching existing planting item FIRST or one from
the actual native loot sampled once at reach. Full-slot CAS, one-use BEFORE slot
writes (even unchanged full bag/real overflow), copied native input, exact crop/
soil/tool/body/loaded native sight/protection rechecks. Same crop age0 before bag
commit; release actual overflow and apply native tool wear only after success.
Failed payment gives no income/wear/world edit. 40tick attempt/success backoff,
200unfunded; no RNG reseeding/roll loops. Existing radius8/y±3/100tick discovery.
One last diagnostic receipt per actor, counters reset on load; actual stock/wear/
terrain/mind memories persist. No global entity+world crash atomicity promise.
Five material tests plus one policy test bring suite286Java+10Python PASS.

Native runs/harvest-bootstrap/play copied270files from stopped earned-station54428.
No new supply, terrain/crop growth/age, actor/body/clock/RNG/action setter; readonly
watch selects actual parents. Existing fixture/history acknowledged; no merges.
15542exit0,2400ticks54429→56829/final56832: early54525 Fern no beetroot/seeds,
12wheatseeds,bread1,wheat1,food20. End beetroot2,seeds4,ironhoe8→10/250,
crops2,fromHarvest1,fromStock1. SelfFoodDeficit10→8,food20: real edible reserve
gain2,NOT evidence he ate the beets. Dara ate one old bread,NOT farm-derived food.
Second retained receipt54594 at(-230,72,752): native1beet+4seed; prior1beet/1seed,
after2beet/4seed,overflow0,hoe9→10,age0. First receipt overwritten; no invented
individual loot count. Both originally ripebeets(-228,72,751),(-230,72,752) now
defaultage0(saved Name-only omits defaults);8farmland remain,6wheatstillimmature.

Sameworld1537,400ticks56833→57233/final57237 retainedstock/hoe10/defaultreplants,
food20/deficit8; noextra harvest, loadcounters0/lastHarvest{}. Its tool terminal
response was truncated and later handle unrecoverable: collector exitCode0 is
hardcoded, not independent evidence. Native log normalshutdown/BUILD SUCCESSFUL
and exactchainterminal confirm finish; never relaunch on lost observation.
Five-actor mean2.780779/3.876113ms,notpopulation/tailproof. Cohort4originalalive+
EXISTINGchild,7olddeaths,0missing. Legacy newSelectedParentChildren1 is birth36379,
NOT new child. Both tables persist. Parent270files/sourcepins unchanged.
Owned terminal951632/15254361,951740/15254418,951886/15254918;
955586/15300159,955689/15300217,955802/15300471. Noactivejob/timer/pendingcommand.
verify-results.py48assertions; archive source-main.zip SHA
116305c66be991839acfbf979bafae32f7ed5edcdd5b7228fa80323092b8c56d.
Current devSHAabove; deployed0.4.0 unchanged. No mainJava edits after source pin.

## Earlier earned-wood construction checkpoint

Read design/station-production.md and evidence/2026-11-07/earned-station/README.md.
Source changes: StationProduction.hasPurpose now admits fed(food>6)adults with
actual table materials,without grain/planting supplies;existing hungry food-work
exception stays. WoodCrafting.canMakeTable is cheap inventory-only,no copies or
native recipe/world lookup. No-job fast path skips native table scans/reservations
when no station job is admitted. All body/home/reach/sight/protection/native recipe/
output/capacity/real table item/placement checks and budgets remain. No schema change.
280Java+10Python pass;two new regression tests cover a fed seedless mangrove
woodworker and urgent-hunger/material intent boundaries. No new worker/model/timer.

runs/earned-station/play exactly copied260stopped files from runs/forestry-root/play,
which alreadycontained the actual gifted meal,native2log income and paid propagule.
No new wood/plank/table/food/seed,actor/health/hunger/fatigue/age/clock/growth/RNG setter.
Only observer teleports,native table-site grant/protection/removal,read-only watch.
Parent world stayed unchanged across all3runs;no history/actor/inventory merging.

Native site(-281,75,745),sand74,AIR76,four clear adjacent stands,within home(-269,75,733).
6283exit0,400ticks52415→52815/final52819:grant9fae0d6b-e491-4669-bd75-1ba2be6284d0,
protection909ea3e8-48e7-4147-8126-45bb165c25e2. Recipes0/build0,earnedlogs2/root1
untouched,targetAIR,grant/protectionretained. Binds source-protected.zip.

32508exit0,1200ticks52820→54020/final54023:removedONLYthat protection UUID. Early
native status captures actual4mangrove planks after1recipe/log2→1. End2recipes and
1built table;log1/root1,zero planks/tableitems,food10/health20. Builder professionnone,
no grain/seeds/hoe needed. Raw chunk actualCRAFTING_TABLE at target,selectedsupport/
headroom/access/earlierpropaguleunchanged. Completed station grant retired.

47102exit0,400tickrestart54024→54424/final54428:table/selectedterrain exactlypersist,
log1/root1,recipe/buildcounters0,grantstillretired. Native load can compact slots;
compare counts. Newest source-main.zip matches approved+restart source hashes.
Native five-actor means3.797294/3.149041/3.68097ms,snapshotsnotcapacity/tailproof.
Same4originalsurvivors+existingchild,7olddeaths,0missing. No original clients touched.
Exact chains all terminal:
929684/14954361,929792/14954419,929950/14954928;
932264/14981652,932366/14981709,932586/14982059;
934113/14999781,934221/14999839,934336/15000093.
No pending command or owned automated release/build timer;frozen3same/adaptive3absent.

verify-results.py passes40data/source assertions;source-main.zip SHA
bb933cea1ef873f037011f44803d988171162ba2d434b8fc3326c93980beda8f.
A verifier draft incorrectly expected the early plank note to remain in final
short memory ring;it was legitimately evicted. Early-observation.json retains it,
final has table/build notes. Corrected check uses actual capture times;no source/
game change to satisfy it. Do not treat short memories as durable building registry.
That older dev SHA was d661408f65b5634dda480cd875c8813f96ddde477cfa3fd4845e1e57eb376ef5;
its archived source is preserved, no longer current. Source+installed0.4.0stillmatches.
Do not re-run old verifier against changed main sources. No PR/commit/release/deployment.

## Forestry proof retained — do not repeat

Current productive history contains native wild seed→3explicit leaves→1separately
approved dry root→2logs→paid propagule. source278native74594exit0 and12321restart
passed inventory/terrain/authority retirement/no replay. The earned-station child
spent ONE of those two logs on its new table. Exact old selected resource blocks
and raw sources under forestry-access/. Read that evidence only if needed.
Plant(-269,72,723),mud71,AGE4/STAGE0,nonhanging/nonwaterlogged persists. Former
upperlogs untouched. Natural tree growth/secondincome cycle NOT proven.

Schema5 root command permits1existing loaded dry root with fresh authority,not
inherited leaf permission. Old schemas migrate with empty missing roots. Protection
and outside-write revocation include pending root;actual direct root no-op native
revoked wholegrant without cutting. Read-only ForestryProbe/shared native stand
checks identified root(-270,74,722)blocking bothlogs from validmudstand. No worker
teleport,home widening,forced trees,food rate changes or hunger bypass.

Original runs/forestry-refresh/play remains a separate hungry seed1/quota2copy,
and runs/forestry-access/play is its separately revoked test copy. NEVER transfer
food/resources/histories from the fed productive worlds into those old copies.
Existing firststarvation/failed/stale reports remain documented;do not re-run or
use stale copied HOME-route report as forestry evidence. Full goal stays active.

## Earlier food help — preserve, do not repeat gift tests

FoodHelp lets a hungry actor with no usable ration seek visible local adult peers
within16/existing home bounds,at most16peers/100ticks,sharedpathbudget,400trip/80stall/
400failurebackoff. Selection never reads peer stock/private needs. Existing actual
WANT FOOD decoding,600tick per-session request budget,donor willingness and
within3/mutualsight/hearing/samelevel/awake handoff remain. Real transferred food,
not direct nutrition. Child may ask adult; frozen skips; danger/trade/food work
interrupts. Successful transfer adds actual memories and session counters.

Native50239exit0 proved one authored request,Dara bread3→2,Juniper food6→10after
actual meal.58593exit0 restart retained nutrition/history/stock,no extra gift.
Planned approach before contact and native foreign/refusal branches remain open.
Source Metabolism/MealPolicy/BreedingPolicy unchanged. The hungry forestry gate
food>6 is checked at work and commit; preserves real seed/grant while waiting,
without disabling hungry food work.23918exit0 native seed1/quota2 retained with0
forestry batches. Read food-help/README.md only if this behavior is next scope.
Request cooldown is session-local,not durable authority. Full broad goal active.

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

## Next focused work — prove a repeatable food cycle

Do not repeat audits,table/wood/seed/root/foodgift or first beetroot acceptance.
Current productive fixture runs/harvest-bootstrap/play is stopped57237 with real
beetroot2/seeds4,hoe10/250,8crops/0mature,6wheatages1..5(nowlast1→2naturally),
two default-age0 beetroot replants. Native tables(-233,72,746),(-281,75,745).
Juniperlog1/root1 remain; no history merging or resource refill.

Read harvest-bootstrap/README.md,verification.json,persistence/food-gap/report.json
and relevant growth/food-production code only. Source audit is done. Next useful
scope is natural maturity→paid second harvest→actual meal/reserve refill and
adequate output rate. Six immature wheat could eventually feed native bread.
Do not change growth/clock/RNG/metabolism or supply food/seeds/tools to manufacture
sustainability. Native crop-production testing for wheat/carrot/potato remains open.

Alternatively inspect useful paid farm expansion/irrigation constrained by the
accepted terrain policy. The four new beetroot seeds are real earned stock, but
planting selection may choose the existing12wheatseeds first; do not force the
bag or widen authority silently. Explicit grants/protection, native tool/seed
costs, real growth and bounded paths/scans remain. Avoid broad speculative fixes.
No grain-fallback source patch is needed: the previous diagnosis was disproven.

Keep evidence focused: baseline actual stock/ages/tool/needs, exact native income,
replant payment, actual consumption or stock refill, then persistence if behavior
changed. Preserve existing child's birth36379 and seven deaths. Session counters
reset on load and lastHarvest overwrites; do not assume a lost receipt or short
memory still exists. Primary/persistence snapshots are bound to their own times.

Natural regrowth/secondwoodcycle,adequate repeated farmoutput,material transport,
generalbuildings,landmark/history knowledge,parentcare,defense,adaptation and
population/tail-latency performance remain. Preserve the FULL society goal and
user0.4.0/allclients/worlds/services. Source0.4.1-dev stays unreleased. No resource
hold/model/hash/inference grant. New jobs need exact fresh custody,no PID inheritance.

Native boundedplay100..12000ticks(0interactive),normal quit adds shutdownticks.
Capture own exact chain early with /tmp/villagers-capture-owned.py TAG OUT(O_EXCL).
Recover existing live handles instead of relaunching on observation loss. Pass
this full goal and a sharper focused handoff into the next context. No new workers,
periodic polling or implied service deployment. Keep goal active.
