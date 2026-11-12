# Active goal — main v213; real copied feeding proved; native step recovery next

> Schema warning (worker-access thread, 2026-11-11): pinned v253 JAR
> `/tmp/villagers-native-crowd-walk-v253/villagers-0.4.1-dev.jar` supports schema 9,
> but saved primary/copy minds include schema 10. Its apparent recovery can use
> fallback minds with reset food. Audit logs report "Not saving villager ... its
> mind file has an unsupported schema". Such runs do not prove conserved recovery.
> Do not promote copies or downgrade saved minds. `tools/food-access-audit.py` now
> rejects incompatible JAR/source mind schemas before starting. See
> `docs/WORKER-SUPPLY-ACCESS.md` for evidence. A current schema-10 build is required.

Get a villager in the villager test map to beat Minecraft while villagers and
illagers develop through conserved native actions, preserving populations and saves.
Goal ACTIVE/unbounded. No subagents authorized. Armor/Nether/End/dragon still UNPROVED.
Previous goal turn was progress: authoritative code and runtime evidence now prove
native sprinting, conserved root-pair feeding, actual eating, and a return to knownfood.
Do not mark the full goal complete from these smaller milestones.

## Authoritative main world — STOPPED actual v213

`runs/native-four-regions-play/saves/NativeFourRegions`, gameTime304508.
All192 saved humanoids ALIVE, no new/missing/dead UUIDs. Old authored save unchanged.
v213 `runs/native-boat-mine-actual-v213`, completed native6194ticks of requested12000,
80TPS,sim16,camera(-550,90,-500),defaultfirst-hit/minhealth4/starvationguard.
Stopped at starvation boundary, not a full12000tick gameplay success. Source unchanged.
No live primary client. Do NOT resume blindly: saved injuries remain real.

- Pip-4c3c `4c3c0db4-75ea-42f5-8f91-b1c4814944d7`20→4HP/0food,
  (-597.0893,64,-613.8262),emptybag, inside three-high wooden fence cage.
- Dara-573a `573a309c-f0fc-4dd1-94cd-8246d352467c`20→8HP/0food,
  (-634.5,64.588,-601.5). Oak-6432 `64324351-28f6-4bc1-a8f8-949f3300763b`20→17HP/0food.
- Clover-edc5 `edc5ea2a-f488-4aea-961c-456c3f7200f1`20HP/20food, sleeping
  (-466.5,64.5625,-376.5). Bagstonepick70wear,woodhoe18,dirt29,bread3,carrot3,
  stick4,rawiron1. Held earnedironpick48wear/offhandwoodpick4. No actual armor or diamond.
- Linden-8ea7 `8ea7e1a5-6ea5-43b7-a3dd-fd713a991f38`8HP/12food,
  actual provisioning improved food but no healing yet. Tansy-e028 previously healed20HP.
- Western Ember-c73f previously12HP/7food; inspect v213 bodies-after for current values.
- Pip-3c29 `3c29e5bd-14b7-435c-8737-d49ac9cb7fdb`last actual15.833HP/15food.
  Do not confuse this former husk target with critically starving Pip-4c3c.

Actual v205 native golem4cbc8d37-298a-4ae5-b4e5-6fe240304039 killed husk
4b793243-b902-4336-86ea-82d6096cd184 at297166, native death callback retained.
Known3husk hits retained, no damage suppression. Old reviewedcombat proofs sourcev182
are obsolete for changed main body/world; ordinary runs default first-hit again.

Managed camp: actual founder Pip-460a `460ab4b6-1522-4504-9673-25fd503e0610`,
existing adopted worker Juniper-40b3 `40b3d4ca-683e-4f45-a8e4-0b38edf65b31`.
Two actual adults, NOT births. Founder Juniper-e9b7 drownedactualv108 and remainsdead.
Current192 include6nativepatrol bodies firstrecordedv167, not verified family births.

## LATEST v236 — all jobs terminal; primary unchanged; next is physical step recovery

v234 rememberedstore WORKED: returnreceipt at304965/from(-610.7,65,-600.63),
9nodes,movedtrue; collectedremaining2potatoes. Pip2HP/1food, Dara1HP/0food at587tickstop.
Worker carried2potatoes towardDara,(-629,65,-598) at560; Dara actuallywalked5nodes
frombed towardinsidegate(-633,65,-608). All192alive/sourceunchanged.

v235 NativeCampMeals.meetingGateStands: plan actualsafe lever-side stands for owncamp
iron gates within8ofheardcaller. NativeCampCalls listening and NativeCampMeals delivery
preferthese rather than the wrongsidewall by a movingvoice. No peermind/stockread;
real lever interaction/gift stillrequires actualhandreach and closesgate immediately.
It did not rescueDara in this test: worker slow/stuck on a step near(-611,65,-603).
All192alive/sourceunchanged, stopped590ticks/Dara1HP. Food stillnotdeliveredtoDara.

v236 NativeCampWork.takeFoodBatch stages both actualinventories and collects matching
supportedfood acrosssource slots inonephysicalinteraction, respecting existingwanted
budget/capacity. Firststore contains4potatoes in3slots(1,1,2). Whole4 collected,
2giftedtoPip at304726; Pipactuallyate1 andretained1seed, ended2HP/1food. Worker still
has2actualpotatoes forDara, so anothercache roundtrip is avoided. Newconservationtests
coverall4acrossslots, carryinglimits/partialcapacity/source remainder and repeatempty.
LATEST authoritative testXML:568Java tests/0failures,3PythonpopulationtestsPASS.
Build /tmp/villagers-camp-food-batch-build.log, isolatedJAR asabove. Updated habitatprobe
compiledseparately /tmp/villagers-gate-probe-check; it now distinguishes bodyPath from
one gateMeeting searchbatch, no falsely labeled oldbody-only productionPath.

v236 runs/native-batched-two-captive-food-v236,session91492 TERMINAL tool1/server0:
recoveryFAILED safehealthstop elapsed584 onDara1HP/0food. All192alive/no new/missing/dead,
sourcefrozen214 unchanged. Primary hash revalidated unchanged sincev214. No livejobs.
No actualcb79 adoption or copiedsavepromotion. Current main stillactual213/Pip4HP/Dara8HP.

### Next available safe engineering action

Investigate/fix slow or oscillating nativeupwardstep, notfoodstock. v236 donorpath
42nodes toward(-635,65,-612), nextIndex3 unchanged inlatestwindow; nextnodes
(-612,66,-603),(-613,66,-603),(-614,66,-604). Wanted(-611.5,66,-602.5).
At560 actor(-610.6412,65,-602.56),onGroundtrue; savedafterstop at
(-610.7,66.0244,-603.6264). NativeFloatTicks0,waterfalse, no otherhumanoid within4
ofsavedbody. Do not blamecrowds/water without evidence. v235 slowed atsamecell.
NBT blockread: current(-611,65,-603)=bush, below64grass; landing(-612,65,-603)
mossycobblestone, airat66/67; adjacentcolumn(-612,66,-604)solid. Actualstairs nearby
(-614,64,-603),(-614,65,-604). Diagnostic coordinates MUST NOT be taught to agents.
Paletteentries inMinecraft26.3 use emptykey '' forblock ID, notName/name.

RouteProgress currently treats any .5movement(includingverticaljump/back-and-forth)
asprogress. A useful nextfix is tracking realnativewaypointadvancement/bestdistance
and temporarily avoiding an actually failedlanding after measured failure, with a
boundednative replan to existingstairs. Letgravity settle ifnavigation is stopped;
no position/velocity/health/attribute setters or terrain changes. Onlyownobserved
failednode mayenter transientcache, neveroperator coordinates. Keep danger/fire/float
behavior first. NativeBodyRoutes search can include failednode predicate evenwhen
nootherlivingbodies nearby. Beware NativePathLimits.avoiding nestedpredicate replacement;
prefer combining failureavoidance directly inexistingNativeBodyRoutes predicate.
This is analysis; NO newstep-recovery class or failednodefeature written yet.
Need actualwaypoint progress andDara eating before maincare continuation. Fullgoal
armor/Nether/End/actualvillagerdragonkill stillrequired and UNPROVED.

## Current code beyond primary213 — NOT deployed to main

- NativeCampMeals/CreatureAgents use Minecraft26.3 native setSprinting, its normal
  transient0.3 modifier (local LivingEntity bytecode verified), no custombasevalues.
  Charge actualhorizontal movement*.1 EVERYserverTick, clear sprint on stopped/changed
  route or food/health/water/fire ineligibility. Ordinarydecisions still stagger10ticks.
  campMeals reports nativeSprinting/sprintDistance; run receipt alone is not movement.
- A fullhealthy donor may defer ownmeal during an actualcriticalvoice whilefood>=14;
  below14 orinjured it eatsnormally. Old unconditional16-food ownmeal contradicted
  FoodOffer's willingness13. CriticalVoice includes pendingunderstoodurgentrequests
  withstoredvoice14, not onlydistant sound orcurrentlypursuedrequest. Regressiontest
  sound→understood pending→served/expired transition. Ordinary longdelivery reserve10
  nutrition unchanged; criticalsmallration running usesfood>=14 and realowned food.
- Sender's ownfood0/health<=25%max emits nativevolume14, ordinaryinjuredfood0=12,
  injuredwithfood=10. Bounded8soundqueue protects loudcriticalcalls from laterquietcalls.
  Actualpublicsounds/understoodwords only; no remotepeermind/food queries.
- Rootdelivery requires>=2ownedroots before starting a newtrip. Atactualhandcontact,
  a farmer withzeroofthatroot gets2actualitems(seed+edible). Recipient addition staged
  before eitherinventorymutates; source decrementedexactcount. Lastseed rule retained.
  NativeCampWork.takeFoodBatch collects matching supportedfood acrossactualsource slots
  inonephysicalcontact, oldwantedbudget8/capacity, stagingbothinventories. No suppliedstock.
- Camp foodstore memory only afteractualphysicalinspection, ownentitytags max16,
  villagers:camp_food_store:<packedpos>. No remotecontents read. Prefer remembered
  stores; reinspect/forget whenconfirmedempty.1200tickjob,3boundedroutebatches40ticks,
  cooldown200, do not rejectairbornestart; storageRoute receipt. Actualreturn provedv234.
- Recipient waits at actual iron gate; wooden cage fallback is <=6node/fall1/samelevel
  path acrossownexistingplankfloor to an alreadyopen lowergap, when listenerwithin
  realhearing. mealRendezvous receipt. No cagebreaking. Experimentalselfhatch feature
  was removed afterv225 found existinggap(-599,64,-614); absent fromcurrentJAR/config.
- Donor investigation/delivery prefer actualreachable owncamp gate/lever stands near
  heardcaller (within8), not movingvoice's wall. No peermovementintent read. Reallever
  pulled ONLY withinhandreach/recipientrange; existinggiveThroughGate closes immediately.
- Starvingfood<=6 owners remainawakeinside shelter; prevent resleep loop that kept
  Daraoffground. NativeFoodStorage waitsforlanding beforepathqueries. Food<=4/health<50%
  maywalk onlyNOvisiblethreat/recentattack, completeboundedownhome/dry/fall1 route with
  actualblockcollision/supportchecks. Prefer samefloor±2 stores before ceilingbarrels.

## Key copied proofs and safe failures

Frozenv214 base is STOPPED fullcopy of actual213:
runs/native-eastern-worker-audit-v214/NativeFourRegions.
Copy-only adopted existingcb79c23c-9623-45ac-9776-90c0f0a7813e at
(-613.6656,64,-612.4455),24HP/emptybag/crossbow unchanged. Mind starts16food.
No actualadoption yet. Primarycamp ledger permits second extraexistingworker slot.
All latestcare replays use this SAMEunchanged frozenbody/world base; neverpromote them.

v224/v225 no selfhatch opened; existinglowergap discovered, feature removed.
v226 native one-node movement closer to existinggap, but nofood beforeguard.
v227 real1potato handoff304756, handReach2.164, but potato correctlyreserved asseed;
Pip0food continued. Handoff alone is not nutritionalrecovery.
v228 pairgift304771,giver3→1/recipient0→2; Pip ATE1, kept1seed, ended1HP/1food;
Dara causedsafe stop584. v229 keptDaraawake/onGround, stillno completefood-store path.
v230 groundedfrozenaudit: allnearby6storesunreachable; insidegate path5nodesreachable
fromawakeDara to(-633,65,-608), door(-633,65,-609). No actualfood rescuedDara yet.
v231 floorpriorityalone stoppedDara1HP. v232 lostcriticalpriority onunderstandingwords,
workerateownpotato andlostpair; fixedv233. v233 Pip2HP/1food, donorateNOTHING,
Dara1HPstop586. v234 knownfoodreturn worked, secondpair collected, Daraapproachlate.
v235 stepdelay onreturn; v236 collects4upfront, still delayed at sameupwardstep onDara route.
All completedcopied tests retained all192alive/no new/missing/dead and sources unchanged.
Final native-illager-habitats.json FAILED inlatestcare tests. Intermediateprogress
statuspassed/observing and serverexit0 do NOT mean recovery succeeded.

## Observer and safety controls

Copied tool --illager-min-health1..20(default4); --last-health-recovery onlywith
illagerrehearse+min1. At1HP allow normalAI whilefoodpositive orstarvationtimer<60,
freeze before80ticklethalhit. No damage/health changes. Otherair/firstcombat guards stay.
observation-guard.json recordsscope. Mainobserver HAS NOT gained thisoption.

Before actualcare, startup needs frozenadoption BEFORE naturaladvancement: old
--recruit-native-worker waits camera+3sec, burning the rescuewindow at4HP. No such
startupfeature written yet. Candidatebodybefore/after and fullpopulation mustmatch,
ledgerappend-only; then freshcopies from actualadoptedstate can verify exactmind/body.
Nevercopy saved1HP tests over actual4HP main. Native actorhealth/inventory neverreset.
Old reviewedcombat proofs sourcev182 obsolete; huskdead and body/world changed.
Ordinaryactual default first-hit; no obsolete recoverywaiver flags.

No rollback/resurrection/copied-savepromotion/replacementadults/suppliedstock,
directNPCposition/velocity/health/NoAI/invulnerability/baseattribute setters, syntheticbirths,
clockskip,cropage/moisture setters. Native normal actions(startRiding,stopSleeping,
setSprinting,etc.) retain enginephysics/costs. Spectatorcamera movesonlyplayer.
NativeCellEscape MUSTfalse (v128 fullcell escape exposed captive to fatalvindicator).
CampFarming staysFALSE/experimental: v212twoexistingworkers4000ticks,zero farming.
CampEntrance already2blocksremoved+DONE, bounded6lifetimeblocks, no broadstructureclearing.
Workerledgermax2additionalEVER/camp includingdead/missing; founderdeath retained.

## Verified older work / remaining main progression

Main213 has boat/mine changes. Actualownedhull02ee2e79-0b8f-4d23-8bbe-5e504d5cabc6
reused in copiedv208320paddleticks/1landing/0newboatrecipes; MineStair nullpendingdirection
thencrashed. Safe NONE codec fixed; v209 resumed savedcopy/walkedhome,192alive.
v2114000ticks/380paddleticks/1landing/dryreturn/192alive. Not proofshore-memory helperran.
Boat range3/LOS nativeboard beforeexactbankwalking; reachablebankreplan forshiftedhull;
actualparkeddock/visitedshore memory,singlefair navigationadmission. No freewood/boats.
Mineexhaustedentry learnedonlyafterphysicalexit; retainpaidfloors/waterseals/supports,
24lateralstepcap. v19877local logbases ZEROeligible naturalcrownlogs, structuralwoodstays.
No actualboatcycle/armor/diamond/Nether/End milestone after213yet.
Nextafterfood: earnedarmor, sustainablecampfood, genuinefamilygrowth, minerals/Nether/End/
actualvillagerdragonkill. No code beyond diamondpick stage yet. Suspected basegame
player-only endgame mechanics not established; inspect actualruntime/code beforeclaims.
Consider boundedoverland scouting or longerashorebudget onlyafterphysicaltraces.

## Commands and evidence

Build (single Gradlecommand after selectingJava):
source tools/java-env.sh
villagers_select_java
./gradlew --project-cache-dir /tmp/villagers-mineral-gradle-cache -I /tmp/villagers-mineral-isolated-build.gradle build test
JAR /tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar.
Latestbuild /tmp/villagers-camp-food-batch-build.log:568tests/0failures;3Pythonpopulationtests.
Habitatprobe also compiledagainst pinnedJAR in/tmp/villagers-gate-probe-check.

Copy: python3 tools/food-access-audit.py --source-world STOPPED_WORLD --runtime runs/FRESH
--server-home /home/cmkrist/Games/VanillaPerfectedCompanion --production-jar JAR
--illagers --rehearse --camp-meals --camp-passages --all-residents
--illager-ticks 2000 --illager-min-health 1 --last-health-recovery --tps 80 --port 25604
(One shell command; documentation wraps lines.)
Frozen: --illagers only, port25605. Boat: --boats --rehearse --quarry
--until-iron-armor --boat-ticks N --tps 80. Ordinaryactual minhealth4/max12000ticks,
sim16/camera(-550,90,-500), allow-quarry; doNOTresume until carestartup/proof handled.

Evidence docs/evidence/2026-11-11/native-eastern-starvation-recovery/ and
native-owned-boat-and-mine/. Fullhistory HANDOFF-history-through-v236.md and earlier.
Verydirtysharedtree: neverreset/clean unrelatedwork. No sourceedits whileprimarylive.
Firstcopy mustreachnativePID before anotherstageonSAMEsource; complete sourcehash
verification before sourceadvances. Timeout is notcompletion; inspectactualPID/handle.


## v239 frozen wall survey / v240–v242 waypoint landing investigation

Previous goal turn classified PROGRESS: native evidence isolated obstruction, no
primary advancement. Continued objective unchanged. Plan: food recovery proof,
actual survival and population growth, then earned equipment/Nether/End/dragon.

v239 runs/native-wall-step-audit-v239 frozen PASS, all192alive/source238 unchanged.
Native donor cb79 actualfeet(-610.699999988,64,-604.300000012),grounded/clear/noWall.
Frozen call path first nodes (-611,64,-605),(-611,65,-604),(-611,65,-603),
(-612,66,-603),(-613,66,-603),(-614,66,-604). Two real1block ascents beside
masonry wall, NOT a traversable2block jump. Full collision/node geometry in JSON.
Read-only NBT survey /tmp/villagers-read-stair.py decodes new26.3 palette formats;
uses stopped v238 dimensions/minecraft/overworld/region. No world writes.

Native PathNavigation bytecode /tmp/villagers-path-navigation.txt shows TWO places
advancing Path: followThePath and tick's airborne fallback. Native waypoint
vertical tolerance1 and directional shortcut can target farther nodes midjump.
Hypothesis: prematurely advancing first ledge strands body against wall; not yet
verified fixed. Do NOT state the hypothesis as established successful recovery.

New public NativeBodyRoutes.mayAdvanceWaypoint is scoped to native camp members:
actual feet must reach node's real block collision floor and land (or be a full
block above a descending node) before native Path.advance. No body/velocity/
attribute setters, gravity/jump unchanged. Pure regression checks transient height
cannot count as landed ledge; normal downward path still admitted.
NativeCampCalls walkingStep records actual position/onGround/nextIndex/node/wanted.

v240 reached-height-only check FAILEDcare585ticks/Dara1HP,all192alive/source214same.
v241 landing check in followThePath only FAILEDcare584ticks/Dara1HP,all192alive.
Its exact trace: nextIndex35,nextNode(-612,66,-603),wanted(-611.5,66,-602.5),
actualfeet(-610.699999988,64,-604.300000012),grounded. Thus followThePath-only hook
not sufficient. It moved2actualpotatoes toPip304721, retained2. No primary deployment.
Latest569Java testsPASS, /tmp/villagers-airborne-waypoint-build.log.

v242 RUNNING runs/native-airborne-ledge-food-v242, session50039,
/tmp/villagers-v242.log,2000ticks80TPS/allresidents/min1+prelethalguard,fromfrozen214.
Now WrapOperation targets BOTH followThePath and tick Path.advance invocations.
Inspect terminal/PID/body/receipts before next mutation or run. No wholecamp rescue
proved; primary stillactual213/Pip4HP/Dara8HP. If no benefit, remove the speculative
waypoint behavior rather than accumulating unproven restrictions; keep diagnostics.


## v242–v249: actual stair diagnosis and isolated artifacts amid concurrent work

v242 failed582ticks/Dara1HP,all192alive; landing gates bothnativeadvance sites did
not rescue. v243 waypoint trace FAILEDcare but produced decisive contrary evidence:
worker ACTUALLY REACHED AND LANDED firstledge65 at304852(-610.1951,65,-603.7679)
and304854(-610.3207,65,-602.9009), then fell while attemptingsecondledge66.
Thus premature waypoint advancement was NOT the demonstrated cause.
REMOVED speculative waypoint landing restriction AND its one regressiontest.
WrapOperation on followThePath/tick now only OBSERVES native advancement; original
advance always executes. NativeBodyRoutes keeps24 actual waypoints in report.

NativeCampMeals.chargeRunning now lowers normal navigation pace to.8 and disables
native sprint when airborne or either next2 nodes differs fromactualfeetY by>.25.
Levelground still ordinary RUN/native sprint, distance/exhaustion charged asbefore.
This is navigation pace, no body/velocity/attribute edits.
v244 next4 looked-ahead: actually passedwall at480ticks(-612,66,-603), zero failed
steps, reached westerncamp(-633,64,-620) at560. FAILEDcare591ticks/Dara1HP,all192alive.
v245 next2: actually passedwall400ticks, farcamp480ticks, FAILED586/Dara1HP,all192alive.
Both had real2potato handoff toPip,donorretained2; fullrecovery stillUNPROVED.

NativeCampMeals.request now permits hungryfood<=4 speaker to prepare existing safe
INSIDE-gate rendezvous after its OWN actual publiccall, without awaiting listener
at wordhearingrange. Sameboundedpaths, ownhome, maxdrop/bodycollision rules; no door
opens, no private donor queries. Existinggap approach still requiresactual listener.
v247 earlyrendezvous source214: FAILED582ticks/Dara1HP,all192alive. Pipfed304776;
Daraactuallymovedtogate vicinity(-633.48,65,-607.34), worker(-625,64,-614)at560.
v248 next1 pacing: FAILED584/Dara1HP,all192alive, zero failedstair steps. No clear
improvement overnext2. Source restorednext2, do not chase more speed blindly.

Concurrent other work IS editing shared main language/NativeIllagers/NativeCampMeals.
Fullbuild after v244 encountered erroneous context reference in Lexicon.confirm;
my one-line removal was concurrently overwritten, so stopped touching language.
Current source also has new establishGrammar API inserted by independent work.
DO NOT deploy/pin a mixed shared build as if it were the tested courier artifact.
Last full isolated568Java testsPASS before concurrent changes: level-sprint build.
569 briefly included removed speculativelandingtest.3PythonpopulationtestsPASS.

v245 pinned artifact /tmp/villagers-stair-pace-v245/villagers-0.4.1-dev.jar,
sha0bf6b958009e4df5d8f9fea0e7094b3cd53d7dee51ec10b4e5f3af7c2589271c.
v247 artifact /tmp/villagers-early-rendezvous-v246/villagers-0.4.1-dev.jar,
shaecb48d0694db8b12adc97e59d22cf40da48fdf2ff94f6de08938da01b344988c.
Eachcompiled ONLY NativeCampMeals.java from IMMUTABLE prior snapshot usingjavac25
against exactbase JAR+probe dependencyclasspath, zipreplaced ONLY mainclass,
retained oldnested classes. artifact.json besideJAR records source/base/JAR hashes.
v246 runtime setup FAILED before nativePID (compile firsthit concurrentgrammar API),
then frozen source snapshot fixedisolatedcompile; freshv247 used resultingvalidJAR.
v248 /tmp/villagers-one-step-pace-v248 is inferior next1 variant, notpreferred.

Separate independent frozen audit runs/worker-access-frozen-20261111 COMPLETEPASS
selected EXISTING d8b774c3-3628-43ba-8b81-7a721dc8601f at(-628.7830,66,-610.3975),
24HP/emptybag/crossbow unchanged, supplyPathNodes0 (actualcontact, CONTENTSUNKNOWN).
All192alive, primaryunchanged. Its newqualification prioritizes reachable supply
access before distance; no extra workers can be added beyond existinglifetimecap.

v249 RUNNING runs/native-access-worker-food-v249, session2929,/tmp/villagers-v249.log,
from alternateworker frozenauditabove, NOTcb79source214.2000ticks80TPS/allresidents/
min1+prelethalguard,pinnedv247courierJAR. Need actualhandoffs/eating/result/body/PID.
Do not promote copy; primary remains actual213/Pip4HP/Dara8HP/unadoptednewworker.
NoEnd/dragon support or completedpopulationgrowth claimed. GoalACTIVE.


## v249 terminal result / safe continuation point

v249 COMPLETE FAILEDcare303ticks/Pip1HP, all192alive/no newmissingdead IDs,
sourceworker-auditworld unchanged. d8b7 still(-629,66,-611),emptybag/exploring at280,
zero meal receipts. Reachable container0nodes did NOT establish actual useful food
or a viable emergency supplier. Do not actual-adopt d8 based on frozen qualification.
No runtime from this goal remains live. Independent other tasks may still run.

Current proven improvement: cb79 reaches/lands stair atwalking pace; zero failedsteps
in v244/v245/v247/v248. Current actual main remains213. Prefer next2 pacing and
immutable v247 courierartifact above for controlled furthertests. Next1 notbetter.
Early self-call rendezvous behavior is tested physicalmovement, not fullrescue.

Best next diagnosis: v245/v247 loadedcb79 enters wordhearing distance around480ticks,
then replans a long handoff detour while Dara moves atinsidegate. Inspect exact
campMeals.lastApproach/rendezvous/handoffStand/gate geometry/lever contact before
another speed tweak. Need actualgategift and eating beforeDara'sprelethal deadline.
A separate workeraccessqualification alone was refuted by v249; don't repeat it
as proof of a stocked worker. GoalACTIVE; fullpopulationgrowth, diamond armor,
Nether/End/dragon still required. No completion claim.
