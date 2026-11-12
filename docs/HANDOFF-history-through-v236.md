# Active goal — primary v213; eastern food rescue before further main ticks

Get a villager in the villager test map to beat Minecraft, while villagers and
illagers develop through conserved native actions and preserving populations/saves.
Goal ACTIVE, unbounded. No subagents authorized. Nether/End/dragon victory UNPROVED.
Historical detail retained in HANDOFF-history-through-v223.md and prior archives.

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

## Turn update v224–v227

v224 copied self-hatch preparation trial and v225 eligibility diagnostic both stopped
safely/Pip1HP/all192alive/sourceunchanged. Diagnostic found EXISTING lowergap at
(-599,64,-614), with two upperfences retained. Recipient correctly did not break another
block. The experimental CaptiveFoodHatch class/config/CLI flag were REMOVED afterward;
none deployed to primary. No new cage damage occurred.

NativeCampMeals.waitAtGate now also offers a short complete native path (<=6nodes,
fall1, samelevel, existingplankfloor, withinownhome) toward an EXISTING lowerfencegap
when a managed samecamp listener is within actual hearing range. No block changes.
Records NativeFoodStorage.mealRendezvous. v226 physically moved toward(-598,64,-614),
1node, existinggap(-599,64,-614), no blocksremoved. Still stopped305ticks/Pip1HP;
all192alive/sourceunchanged. No actualmeal in that test.

Inspection of local actualMinecraft26.3 LivingEntity bytecode proved setSprinting
adds Minecraft's standard transient0.3 sprintmodifier; our earlier RUN1.1 navigation
pace never enabled that native action. Latest change enables native sprint ONLY while
an eligible tracked supply/delivery route is actually active, and clears it on route
stop/change, food need, water/fire or health loss. Charge actual horizontal distance
*.1 EACH SERVER TICK, retaining last segment when route changes. CreatureAgents now
runs this sprint housekeeping perTick; ordinary decisions remain staggered10ticks.
No customattribute/basevalue/velocity/position changes. NativeSprint/sprintDistance
reported in campMeals. All562Java+3PythonpopulationtestsPASS,
/tmp/villagers-native-sprint-delivery-build.log, usual isolatedJAR.

v227 RUNNING: runs/native-sprint-food-delivery-v227 from unchanged frozenv214,
2000ticks80TPS,allresidents,min1+prelethalguard,session81764,/tmp/villagers-v227.log.
Inspect exact progress/result/terminal/PID; no assumption of deliveredmeal or recovery.
Primary remains stoppedactual213/Pip4HP, sourceworld unchanged.

Potential future actualcare issue, NOT implemented: ordinary observer --recruit-native-worker
waits camera+3sec before adoption. At4HP this delay burns the rescue interval. If copied
rescue works, actual startup must freeze before natural advancement, adopt the verified
existing body before resuming, and use an explicitly verified equivalent prelethal
observer guard (currentmainobserver has no --last-health-recovery option). Preserve
allworld/body/stock identity; no copied-savepromotion or retrospective healing.

## v227 real potato handoff; v228 root-bundle recovery RUNNING

v227 native sprint+recipientrendezvous physically transferred1realpotato at304756
fromcb79 toPip, handReach2.16445/bodydistance2.379, giver1→0/receiver0→1,
usedEmergencyReserve true. This is a REAL handoff, but NOT nutritional recovery:
all managedvillagers canHarvest, so their lastpotato is protected plantingstock.
Pip1HP/0food still, sourcecopy stopped305ticks beforelethal; all192alive/sourceunchanged.
Do NOT weaken seedreservation or call handoff a rescue.

Latest NativeCampMeals reserves a usable rootdelivery only when it owns>=2actualroots
in a source stack; bread/othermeals still1. Atactualhandcontact a farmer withzeroofthat
root is offered2items, conserving1seed+1edible. Recipient partialcapacity is staged in
an independent SimpleContainer; if wholebundle cannotfit neitherbodyinventorychanges.
Donor exactstock is decremented bycount; receipt now includes count. Ordinary10nutrition
ownreserve remains unless willingfullhealth emergencydonor permitsreserve. No stock supplied.
Two meaningfulregressiontests: pair preserves seed whileallowing1meal, and partialfit
rejectswithoutmutatingeitherinventory. Latest564Java testsPASS,
/tmp/villagers-root-bundle-delivery-build.log. Primary remainsstoppedactual213/Pip4HP.

v228 RUNNING runs/native-root-bundle-food-recovery-v228, from unchangedfrozenv214,
2000ticks80TPS/allresidents/min1+prelethalguard,session91417,/tmp/villagers-v228.log.
Inspect result/body/receivedreceipt/sprintDistance/population. May expose anothercritical
resident afterPip getsfood; allregioncohort mustremainalive. No actualworker adoption yet.

## v228 verified Pip eating; next resident Dara / v229 RUNNING

v228 physically delivered2actualpotatoes at304771, giver3→1/receiver0→2,
handReach2.31856, samecb79. Pip actually ATE1 and retained1plantingroot:
at560ticks1HP/1food (no healing claimed). All192alive/sourceunchanged.
Test then stopped584ticks on Dara-573a1HP/0food beforelethal; source214same,
main remains213/Pip4HP/Dara8HP. Latestcare is not wholecohort recovery proof yet.
Worker had1remainingpotato and was seeking another unknown-content campstore,
(-597,63,-592); insufficient usable rootpair for another empty recipient.

Dara's snapshots expose a sleep/wake loop: criticalfoodwork wakes her, then
VillageSurvival puts her straightback inbed eachthink. Body repeatedlyoffground
at64.575/negativeverticalvelocity; all frozenwalkingpaths NULL. NativeFoodStorage
also refused all walking below50%health. Latest fix: starvingfood<=6 shelter stays
awakeinside, stops resleeping; preserve actualgravity/landing. Criticalfood<=4 actors
below50%health maywalk onlywithNO visiblethreat/recentattack, wholepath insideownhome,
complete bounded dryroute/maxfall1 with actualblockcollision/supportchecks. Wait for
nativeonGround before planning, never reject a transient airborne start. Contents
still unknownuntilactualcontact; no newfood/block/health/body setters.
564Java testsPASS,/tmp/villagers-starving-shelter-wakeup-build.log.
v229 RUNNING runs/native-starving-shelter-wakeup-v229 from frozen214,
2000ticks80TPS/allresidents/min1+prelethalguard,session36778,/tmp/villagers-v229.log.
Need inspect wholecohort/result/terminal, exact food and native landing/path receipts.

## v229/v230/v231 diagnostics; v232 critical meal deferral RUNNING

v229 wakingfix worked: Dara awake/onGroundtrue at(-634.5,64,-603.5). Foodroute
stillincomplete3nodes ending(-633,65,-603) forupstairsbarrels. Stopped583ticks/Dara1HP;
Pip actuallyfed1food/1HP, all192alive/sourceunchanged.
v230 frozenaudit from stoppedv229 PASS. Samefloorbarrel(-629,65,-613) alsoNOcomplete
route; all nearby6stores unreachable. Nowactualgrounded gate route to ironDoor
(-633,65,-609) IS reachable5nodes ending(-633,65,-608), maxdrop0. Earlier while
sleep/wakelooping all gatepathsNULL. Iron gate still physically blocks food access.
v231 floorpriority: NativeFoodStorage now prefers abs(storeY-bodyY)<=2 before Euclidean
closerupstairsstores. No newrange/remotecontents. Rehearsal stillstopped591ticks/Dara1HP,
all192alive/sourceunchanged. Floorpriority alone does not open the blocked route.
Frozenprobe now samples nearbygroundfloorstores plus6nearest, not onlyceilingbarrels.

v232 latestchange: healthyfullhealthdonor honors existing FoodOffer deferral when
food>=14 and actualcriticalvoice heard. CreatureAgents ownmeal normallyatfood<=16 now
defersONLY underthatcriticalcondition, stoppingdeferralbelow14 orinjured. Old behavior
ate1of4potatoes despite willingnesspolicy13, leavingonly3 for2emptyfarmerrecipients
needing2each(seed+meal). Nativecriticaldelivery sprint eligibility now>=14 to match
this policy (nativefuelchargedperTick); ordinarylongdelivery10nutrition rule unchanged.
Actualfood/health/metabolism neveraltered; no waivedseedreservation.
564Java testsPASS,/tmp/villagers-critical-meal-deferral-build.log.
v232 RUNNING runs/native-critical-meal-deferral-v232 from frozen214,
2000ticks80TPS/allresidents/min1+prelethalguard,session45639,/tmp/villagers-v232.log.
Needactualfoodhandoffs/eating/allcohortpreservation; mainstill213stopped.

## v232/v233 / v234 store-memory replay RUNNING

v232 criticalmealdeferral initiallyfailed: afterunderstandingPip words, the distant
soundcue wasremoved; deferral onlychecked currentpursuingvoice, not newlyunderstood
pendingrequest. Workerateone reservedpotato, lostusablepair, stoppedPip1HP/nofood302ticks.
v233 fix preserves pendingcriticalrequestpriority: criticalVoice includes fresh eligible
heardurgentrequests withstoredvoice>=14, before selectingdelivery. Newregressiontest
understandcriticalsound→remove soundcue→pendingrequest stillcritical→served/expired
notcritical.565Java testsPASS. Actualnativev233 pairgift at304755; Pip actuallyate,
ended2HP/1food (betterthan1HPprevious). DonorATE NOTHING,still16food/full24HP.
Wholecopy stopped586ticks/Dara1HP/0food, all192alive/sourceunchanged. Donor wandered
to newfieldstore(-593,63,-595) collecting1potato at305065 instead of returning to the
firstactualstore with2potatoes left. No fullcamprecovery or mainadoption yet.

Latest NativeCampWork remembers actualphysicallyinspectedfoodstore position in its own
entitytag villagers:camp_food_store:<packedpos>, max16. No remotecontentsread. Reinspect
atphysicalcontact; remove rememberedfoodstatus whenconfirmedempty. Prefer remembered
store before new visible stores when provisioning. Job budget1200 vs400, atmost3native
standsearchbatches40ticks apart before cooldown200; do not discard a rememberedstore
for one failed/airbornestart. Wait for nativeonGround before replanning. storageRoute
receipt records actualfrom/store/nodes/moved/failures for causality. All565Java testsPASS,
/tmp/villagers-remembered-food-store-build.log, usualisolatedJAR.

v234 RUNNING runs/native-remembered-food-store-return-v234 from frozen214,
2000ticks80TPS/allresidents/min1+prelethalguard,session12803,/tmp/villagers-v234.log.
Needinspect fullresult/body/actualreceipts. Primary remains actual213/Pip4HP/Dara8HP.

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

## Latest verification — v223 failed recovery safely; NO live tests

Evidence packages:
- docs/evidence/2026-11-11/native-owned-boat-and-mine/
- docs/evidence/2026-11-11/native-eastern-starvation-recovery/

Latest build562Java tests PASS,3Python populationtests PASS.
Log `/tmp/villagers-critical-supply-food-budget-tests.log`.
JAR `/tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar`.
Build command: source tools/java-env.sh; villagers_select_java; then
./gradlew --project-cache-dir /tmp/villagers-mineral-gradle-cache
-I /tmp/villagers-mineral-isolated-build.gradle build test (one command).
Very dirty shared tree: never reset/clean unrelated work. Do not edit src/main while primary live.

v214 `runs/native-eastern-worker-audit-v214` is a STOPPED frozen fullcopy of main213.
It copy-only adopted actual existing `cb79c23c-9623-45ac-9776-90c0f0a7813e`,
(-613.6656,64,-612.4455),24HP,unchangedemptybag/crossbow. Camp ledger allows second
additional worker slot. No actual adoption yet. Source main unchanged. Its saved
`NativeFourRegions` remains unchanged and is the proper source for further trials.
New worker begins16food (ordinary adoption mind), not17/18. No supplied food/body.

v215 default4HP guard stopped elapsed1 because Pip starts4HP.
v216 floor1 stopped246ticks, Pip1HP; worker3potatoes but redundant18food delivery gate.
v217 removedgate, stopped243ticks/Pip1HP; worker followed less urgent callers.
v218 criticalvoice14 picked Pip, stopped246ticks/Pip1HP before real handoff.
v219 criticaldelivery RUN1.1 stopped244ticks/Pip1HP, worker(-610,64,-614).
v220 copied prelethal1HP recovery grace stopped308ticks/Pip1HP/timer60, no meal.
v221 urgent food-supply RUN still stopped304ticks. No earlier pickup observed.
v222 retainedcriticalcall queue stopped302ticks, supplyRun receipt EMPTY. Workerinitial16
below17threshold blocked supplyrunning; queue fix alone did not improve pickup.

v223 `runs/native-critical-first-supply-run-v223`, session60200 COMPLETE exit1 tool
because recovery FAILED; server terminalexit0 is not success. Source frozen214 unchanged,
all192alive/no new/missing/dead. Stopped elapsed304, Pip1HP/0food/timer62.
SupplyRun receipt actually proves RUN1.1 at304554,food16,5nodes from
(-618.0784,64,-604.9222). Actual potato pickup304604 vs previous304649:45ticks sooner.
Atelapsed280 worker(-602,63,-621), following native handoff route; zero delivered meals,
zero hatch openings. No copied save promoted. Main remains4HP, not these copied1HP bodies.

## Changes after main213 — NOT yet deployed to primary

NativeCampMeals.reserveAvailable removed redundantfood>=18 restriction; still uses
FoodOffer.mayDeferOwnMeal(fullhealth,food>=13), actualration/contact/recipientcapacity.
Own eating runs first atfood<=16. Worker can gather real potatoes from native chests.
NativeCampCalls.volume14 only caller's ownfood0/health<=25%max; ordinaryinjuredfood0=12,
injuredwithfood=10. This is an actual public sound, never a remotehealth/stock query.
Bounded8-call queue retains louder uncommitted voices instead of evicting oldest
criticalvoice for laterordinarycall; regressiontest covers12latercalls/cap8.
NativeCampMeals deliveryRUN permitted forcriticalvoice withactualration/food>=17;
ordinary longdeliveries still require own10nutritionreserve. Every actualmovement
step remains charged; falls/physics unchanged, no NPCvelocity/position/attribute setters.
NativeCampWork can hurry on an existing safe dry food-storage path after hearing
criticalvoice,fullhealth/food>=14; dropsrunning below14. Uses same paidmovementcharge.
SupplyRun receipt proves actual start rather than merely canReach or activitylabel.

Copied observer options `--illager-min-health1..20`default4 and
`--last-health-recovery`valid onlyillagerrehearse+min1. Optional1HP recovery allows
normalAI whilefoodpositive or starvationtimer<60, then freezes before80tick lethalhit.
Pure ObservationHealthGuard helper tests timer59/60/79,food,dead/NaN. Does NOT suppress
anydamage or changehealth. All other health/air/firstcombat guards remain. Main observer
has NOT gained this option. Main default4HP remains. Record observation-guard.json.

## Next work

1. Solve PHYSICAL food access/delivery before further actual ticks. Latest worker gets
   a real potato sooner but normal route and feedinghatch work are still too slow for
   the remaining saved starvation interval. Do not claim a rescue from route labels.
2. Promising unimplemented option: captive helps prepare a one-block feeding hatch
   from inside its own three-high wooden fence cage while awaiting food. Existing
   CampFoodHatch already defines safe lower-block-only opening, retains upper2fences
   and native drops, actualcontact/tool/block work. A single lowerhole is not a full
   body exit; must prove geometry and population safety in copies. NativeCellEscape
   full escape staysFALSE because v128 vindicator killedcaptive after openingcell.
   This is only analysis: NO self-hatch feature or enablingconfig written yet.
3. FrozenPip geometry: fences(-598,64,-615),(-597,64,-615),(-599,64,-613),
   (-596,64,-613),(-598,64,-612),(-597,64,-612), all actualcontact. Nearbybarrels
   (-599,64,-621),(-604,63,-621) are NOT reachable fromcell; nohealththreshold tweak
   alone solves access. No operator coordinates may be taught to actors.
4. Once copied actualdelivery/survival proven, actual existing-worker adoption and
   ordinarycare continuation can proceed (no sourcecopypromotion). Main min1 recovery
   would need explicit equivalent prelethal observer behavior verified first if needed.
5. Earn actual armor, diamonds, Nether/End, actualvillagerdragonkill. Still required.
   No code past diamondpick progression yet; basegame player-only endgame mechanics
   suspected but not established runtimefact. Do not claim Nether/End supported.
6. After safety, consider bounded overland mineral scouting if localpondshores exhaust.
   No such landscoutfeature written. Currentboat ashore1200tickbudget may also be short;
   need traces before changing. Camera midpoint(-550,90,-500) covers camp+grassvillage.

## Verified boat and mine work — deployed in main213

Existing owned hull `02ee2e79-0b8f-4d23-8bbe-5e504d5cabc6` at v206
(-464.9293,62.5233,-369.1838), no newly supplied wood/boat.
BoatTravel actual range<3/LOS nativeboarding before exactbankwalk; replan actualdry
bankwithin3ofshiftedhull, max3fair nativequeries, physicalwalking. Actualdeparture/
landing docks retained. Parked dock and actuallyvisitedshore tags support bounded
shore search when hull notvisible. Single fairnavigation admission fixesv210 doublelease.
v208 physically boarded samehull,320paddleticks/1landing/0newboat recipes/deploys,
then MineStair.save NPE at reachedjunction withdirectionNULL. All192savedalive.
Codec now NONE onlypendingselect/root/depth0/part0, activepassagesrequirehorizontal
real direction; two regressiontests. v209 resumed savedcopy,4000ticks,walkedhome,
192alive. v211 another4000ticks,380paddleticks/1landing/dryreturn,192alive.
Finalsurveyreceipt empty: physicalboatcycleproof, not proof that memorysurvey ran.
v20728000/v21024000 preserved192 but noboat. v212 two-existingworker farm test4000
preserved192 but zero grass/logs/recipes/plants; primary farming remainsFALSE.

Mine exhaustedentry marker added ONLY after physicallyexiting exhaustedsite; excludes
immediatequarry/revisitedmine reopen. Existing retained floors/paidfoundations/waterseals,
24lateralstepcap and naturaltoolwear/drops unchanged. No actualarmor earned yet.
WoodBootstrap olderfallback reaches rememberedbell via bounded supportednativepath,
but v198 surveyed77logbases: zeroeligible naturalcrownlogs. Structuralwood must stay.

## Invariants and commands

No rollback/resurrection/copied-savepromotion/replacementadults/suppliedstock,
NPCposition/velocity/health/NoAI/invulnerability setters, syntheticbirths, clockskip,
cropage/moisture edits. Native80TPS executes everytick. Spectatorcamera moves onlyplayer.
Existingworker adoption body/stock unchanged and ledgerappend-only, max2additional
workersEVER percamp includingdead/missing. Founderhistoricaldeath retained.
NativeCellEscape MUSTfalse. NativeCampEntrance boundedowncamp opening≤2width/3height,
6lifetimeblocks (actual2removed+DONE). Primary CampFarming experimentalFALSE.
Firstcopy must reachnativePID before anotherstageonSAMEsource; complete sourcehash
checks before sourceadvances. Inspect terminal/PID; timeout is not completion.

Copy command pattern (space flags/values correctly):
source tools/java-env.sh
villagers_select_java
python3 tools/food-access-audit.py --source-world STOPPED_WORLD --runtime runs/FRESH
--server-home /home/cmkrist/Games/VanillaPerfectedCompanion
--production-jar /tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar
--illagers --rehearse --camp-meals --camp-passages --all-residents
--illager-ticks 2000 --illager-min-health 1 --last-health-recovery --tps 80 --port 25604
Boatcopy: --boats --rehearse --quarry --until-iron-armor --boat-ticks N --tps 80.
Ordinaryactual: tools/observe-native-play.py --runtime runs/FRESH --ticks N
--min-health 4 --tps 80 --simulation-distance 16 --observer -550 90 -500 --allow-quarry.
Do not run actual while starvation rescue unproved.


## Current goal continuation — v235–v238 STOPPED, main still actual213

Explicit user renewed the goal: grow villagers/illagers on the test map until a
villager beats the game. Goal remains ACTIVE, no endgame completion claimed.
No primary world advancement or copied-save promotion in this continuation.

Discovered prior unrecorded completed runs v235 native-known-gate-food-rendezvous
and v236 native-batched-two-captive-food. The latter uses the newer batch-food JAR;
v234 used the older single-item version. Do not infer batch behavior from v234.
v236 collected4 real potatoes, handed2 to Pip at304726; worker retained2 but spent
remaining time jumping at a stair while investigating Dara's heard call. Dara1HP
stop at584ticks; all192 alive. Current source already included batch/gate changes.

New route repair: RouteProgress.stalledAdvance tracks greatest native next-node
index so jumping or revisiting an earlier node cannot indefinitely reset progress.
NativeCampCalls uses80ticks, temporarily excludes a measured failed next step400ticks
via NativeBodyRoutes scoped path searches; existing native physics unchanged.
NativeBodyRoutes checks vertical body clearance for nearby ascent and reports
failedSteps. Call searches try3 bounded stand batches before optional masonry work.
New regression verifies repeated same/earlier indices stall, real advance/new path
reset. All568 Java tests PASS,3Python population tests PASS.
Build /tmp/villagers-low-ceiling-step-build.log, production JAR usual isolated path.

v237 runs/native-stalled-voice-step-v237 completed FAILED care,589ticks/Dara1HP;
all192alive, no new/missing/dead UUIDs, source frozen214 unchanged. It detected
stalled stair but then began native masonry entrance work too late for starvation.
v238 runs/native-clear-jump-food-route-v238 completed FAILED care,584ticks/Dara1HP;
all192alive, no new/missing/dead UUIDs, source frozen214 unchanged, terminalexit0.
Actual Pip handoff2potatoes at304721, worker retained2. Failed step observed
(-612,66,-603), worker around(-611,64,-605); selected masonry outer(-612,66,-605)
then worked too long. Do not call route detection wholecamp recovery or deploymain.
No live runtime remains. Logs /tmp/villagers-v237.log,/tmp/villagers-v238.log.

Next: inspect the actual ascending stair edge/ceiling around the measured failed
step and find an executable native detour sooner, or prove another conserved local
food access mechanism for Dara. Operator geometry must not be taught to residents.
Do not repeat blind long primary ticking; primary remains4HP Pip/8HP Dara.
