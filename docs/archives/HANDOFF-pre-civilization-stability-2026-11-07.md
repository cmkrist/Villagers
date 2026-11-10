# Villagers Project Lead — focused continuation

Updated 2026-11-07 after ledge terrain survey and native rejection of the assumed
builder approach. Previous checkpoint: archives/HANDOFF-pre-ledge-feasibility-2026-11-07.md.
Read this and evidence/2026-11-07/ledge-access/README.md; do not repeat old audits.

## Full continuing goal

Lead Villagers toward a performant Minecraft society that crafts/builds, has
children, protects its village, and develops interesting histories, language,
relationships and adaptation. Keep usage low; carry this FULL goal plus a sharp
specific next task into fresh contexts. The source/thread audits are done. The
broad goal remains ACTIVE; never mark it complete for a bounded feature pass.
No new workers/timers; completed audit_domain, audit_tools_trials and
visible_current_source stay finished. No need to wake old workers for status.

## Workspace and current validation

Canonical /home/niko/.internal/source/Villagers. Most source/docs/tools untracked,
README modified; NEVER reset/clean.0.4.1-dev UNRELEASED, no commit/PR/release/deploy/
service change by Lead. Installed0.4.0 unchanged, SHA4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0.
Current dev/source pins: ledge-access/artifact-pins.json and source-main.zip.
MC26.3,Loader0.19.5,FabricAPI0.162.0+26.3,JDK25.0.5+8,Gradle9.8.1.
Mind schema8, work-sites schema6.300Java+10Python PASS, final build53517EXIT0,
ledge-access/build-final.log. Initial83685 failed a diagnostic command parenthesis,
fixed before native runs. Only ConstructionProbe main source changed.258recorded
evidence/source bindings checked in ledge-access/verification.json. Archived farm
verifier261 and soil verifier399 pass
against archived source, not current main. No source edits after passing build;
later changes docs/evidence only. Normal check tools/test.sh --no-daemon --max-workers=2;
do not rerun unchanged suites. Native runs below exited0 and were collected.

User chose limited autonomous building with real materials and protection for
player structures. Do not ask again. Read design/terrain-permission.md before
terrain work. No blanket demolition/mining/chest looting, block-type ownership
guesses, free supplies, death restoration or body/clock/weather/growth setters.
Generic BREAK/PLACE remain disabled. Exact finite grants and real costs required.

## Current primary — TWO adults, all three deaths retained

Continue runs/ledge-approach/play, stopped104097. Fern5ad3 and Nelle358 alive.
Juniper231e died toZombie66151; offspring589b starved81487 (already adult); Dara c7c3
starved98739. Original cohort2alive/9dead/0missing excludes additional descendant
death. Loaded2. Terminal events/actual loaded census override stale lifecycle
alive/health fields. Old child mind file is not a living child.

NEVER promote older farm-memory102883, soil-transfer-reopen100878, protected98067,
dawn-food86062 (3live), farm-regrowth69646 (4live), or pre-death5live comparators to
erase current outcomes/restore resources. Parents remain read-only references;
never merge same-UUID histories. Default user runs/play remains separate, last
independently4originalalive/7dead/0missing/nochild33104, unchanged.

Current Fernhealth20/food16,22wheatseeds,7beetseeds,1sand,1wheat,realironhoe wear19.
Nellhealth20/food19,1bread,still stranded on clear low ledge(-256.83,68,779.39);
home(-236,72,766),nativehomepath1node/unreachable,notclipped. Ninefarmcells exist,
but adequate yield, distribution and population sustainability remain unproven.

## Latest accepted spatial knowledge feature

Read design/farm-memory.md and evidence/2026-11-07/farm-memory/README.md.
Old searches saw crops only within8 and longer trips knew only native assigned
composter. Farmers now record actually visible crops on farmland, including
immature crops, from ONE candidate in the existing cached radius8/y+-3 scan.
Native sight/eye<=8/home/protection checks precede recording. No wider scan,
chunk request, unseen map, peer-private-state lookup or inherited food knowledge.

OBSERVED_FARM episodes persist in schema8, max8 within existing64episode buffer;
merge radius4horizontal/2vertical, evictoldestfarm. Old1..7 addsNOunseenfarms,
legacy7cannotsmugglenewepisode; olderbinariesprotectfuture8. Observation changes
no nutrition, family cooldown, mood reward or learned utility. When food supplies
cannot meet a goal, known patches precede nativecomposter fallback. Active target
stable; complete1..128loadednodes inside ordinary28home,shared2batches/dim/sec,
40replan/400deadline/80stall/400backoff. Nearbyclearviewmissingcrop forgets memory
atmost8records/100ticks, not remote ripeness queries. Nativeforgotten-crop behavior
is not yet tested. Permission/material/harvestcommit guards remain independent.

Native96337EXIT0 saved102883: Fernlearned(-22672 751)at101494 and(-23072 753)at102814,
2returntrips/2arrivals/0rejections,lastcompletepermitted9nodepath. Source composter
(-22672 752) differs from observed target. Actualwheatharvest102004(-22872 753):
1grain+2seeddrops,pay1seed21→22,hoe18→19,replantage0. Nelllearnednone. Mean3.181ms.
Reopen99411EXIT0 saved103286: exactsame2episodeIDs/positions/ticks,anotheractual
1trip/1arrival/complete5nodepath,nomaterial/harvestreplay,mean3.966ms. Bothadults
survive; all actualdeaths persist. Source/currentparentunchangedduringtrials.
Smallcohortmeans,notlargerscale/tailbenchmark. Existingthird-party pack/Realms
errors remain; no fullcompatibilityclaim. Not proof memory prevented starvation.

## Sharp next task: revise staging, then implement paid step construction

Current saved survey confirms Nell has ONE connected foothold(-257,68,779), not
clipping. Seven virtual AIR→DIRT additions connect her to near her bed in a
conservative full-block model. See ledge-access/route-candidate/report.json for
exact placements/order and raw chunks; actual blocks remain untouched. This is
not native construction/route acceptance. The previously consumed dirt staysAIR.

CRITICAL native result at103735: Fern's proposed staging goal(-256,70,779) is NOT
reachable. Nativepath37nodes/allloaded/canReachFALSE ends(-256,74,778), four blocks
above. It also leaves ordinary28home. Do NOT accept partial/bounded nodes as a
resource route, walk her down, teleport anyone or widen home globally. The model
failed to account for builder approach. Next probe candidate existing stands:
(-254,69,781),(-255,73,779),(-254,73,780), then derive actual reachable work stances.
A possible extra bridgeblock(-255,68,781) from lower69stand is ONLY a hypothesis.
Native face LOS can be occluded by the builder's own floor when placing down a
cliff. Cell-center distance is not sufficient; any edge stance must use ordinary
MoveControl/physical support/collision/reach, never entity setters. Do not invent
acceptance or place the finished staircase instantly as a fixture.

Added play-only /villagers constructionprobe route ACTOR X Y Z: ONE read-only
native createPath query, max64actor-distance,loadedgoal,currentmanagedactor,
shared WorkNavigation admission. Does NOT moveTo/changebody/world/grants. Records
native nodes/canReach/bounded/allloaded/home-range/navigationpathidentity. Outside
play world denied; budget/sessionbusy refuseswithNOquery. Startup request in200tick
run refused; later manual600tick request succeeded as a query, route itselfFALSE.
For future manual query, arm watch atstartup, waituntiljoined/sessionready, then
queue. Do not remove shared budget just to obtain a result. Preserve refusal.

Supply shortlist has7freshbareGrass within64.07..65.81fromFernhome. ExistingSoilTrip
64limit excludes them. A NEW explicit bounded construction/supply job could permit
sources<=68 with its own narrow corridor; do NOT widen/reuse old soil permission.
Every source/target must be individually approved/loaded/realstatechecked; map
coordinates imply no ownership. No steps capability/authority is implemented yet.
New schema or separate metadata must failclosed/migratewithoutinventingnewgrants,
respectprotection/source+targettouchrevocation/dirtysession/quota, use actual native
dirt income/wholebagCAS/payment and progress, neverfreeblocks. Need7+actualscaffold
materials before committing an approach with no current return route. Validate
builder and Nell movement only after ordinary paid sequential construction.

No newworker needed. Do real construction, food access/distribution, communication,
paid buildings/children/defense toward FULL goal; don't settle on diagnostics alone.
Nativefood memory is done, not omniscient sharing. Keep all3deaths; family guards
cannot be relaxed to repopulate. No user terrain preference question is needed.

## Retained capabilities and constraints

Paidsoil native1DIRTgather/carry→SANDreplacement/retainedsand→seed+hoeplant/reopen
passed. Source(-24765 713)AIR,target(-23271/72 753)farmland+wheat,grantsretired.
Schema6max4soiljobs,reservefarmquota(totalfarms+soil<=8),protectioneitherendblocksall,
standardoutsidewritesrevoke,dirtyshutdownrevokeseditskeepsprotection. Rawsavedchunk
editorsbypasshook; no globalcrashatomicity. Explicitsoilsource<=64,target<=28,
corridor8/max72 underlive1200tickclaim/day/readiness/food>6 ONLY; other work/home
rangesunchanged. Details/399archivechecks in soil-transfer/. Do not rerun old
collectors against changed main; recordedverifiers bind source archives.

Actual native renewable beet/wheat harvest/replant→bread/eating, native recipes,
voluntary realfoodsharing/reopen and earnedwood→table hold. Preserve current fullslot
CAS/oneuse/overflow/material/protection guards. FoodHelp16visiblepeers/100ticks,
600request/400trip/80stall,realwords/hearing/donorwillingness. Ordinarychildborn36379
consumed3bread/12nutrition EACHparent,mutualconsent/spareHOME/genes+27words/reopen;
nowdead81487. Family guards cannot be relaxed to repopulate. Hay defaultfalse.
Rain is not dusk: use native scheduledActivity.REST for refuge and daytime wake.
Nativeclock differs fromgameTime by94578; NEVER gameTime%24000. Rest/threats own
movement; no survival-rate claim from narrow route tests. Reuse focused prior
handoff/evidence if touching forestry, family, harvest or refuge code.

## Resource custody and launch discipline

Latest exact chains captured live then terminal verified:
65237:1067997/16401938,1068128/16402604,1068502/16403404;
27902:1069517/16414036,1069622/16414097,1069772/16414622.
First capture of65237 hadonly2jobs; later custody-complete.json capturedall3LIVE,
collectorusescompletefile. Do not overwritefirstreceipt or claimitcaptured3.
65237EXIT0 saved103492;27902EXIT0 saved104097.
Both exactcopiedparentsunchanged;2adults/all3deathsretained.300Java+10PythonPASS.
200tickcasehasNOscreenshot (10secstopbefore15secimage); collectormakesoptional.
NOrestartsolelyforimage/census. All earlier native chains retained historically.
No active transient owned test/build/game/localmodel job,pendingcommand,automatic
release/buildtimer or newworker. Originalfrozendaemon601990/10936325 andgame602372/
10937054 exact/running/untouched; originaladaptive601856/10936249,601946/10936303,
602326/10937021 andfrozenwrapper601857/10936249 absent externally. No cause/ownership/
replacement inferred. Preserve userclients/worlds,Buddy,Niko,Modrinth. Sharedscopes
attribute no other Java. Future tuples inherit NO exception.005closed/spent,
006expiredunused,007earlyreleased09:54:54; no hold/hash/unit/engine/successor/grant001/
inferenceapproval. Historical resource evidence immutable. No periodicresourcepolling.
Head <t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/2a6b199a-0bbd-4884-887d-119d63e29b97>.
Read a namedthread before answering about it. No review/newworkers required.

Native launch: capture exactowned chain in SAMEorchestration after10s using
/tmp/villagers-capture-owned.py TAG OUT(O_EXCL). Recover SAMEconfirmedhandle; never
relaunch solely on lostobservation. Keep native tests finite and preserve outcomes.
Carry this FULLgoal plus a sharper specific next task into a fresh context; broad
goal remains ACTIVE. No deployment/service approval follows a test pass.
