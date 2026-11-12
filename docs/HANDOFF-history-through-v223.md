# Active goal — primary v213, urgent eastern camp food recovery

Get a villager in the villager test map to beat Minecraft, while villagers and
illagers develop through conserved native actions and preserving populations/saves.
Goal ACTIVE, unbounded. No subagents authorized. Nether/End/dragon victory UNPROVED.

## Authoritative update: v213 STOPPED at starvation guard

Primary is now actual gameTime304508 (v213), all192 alive/no new/missing/dead,
old save unchanged, source unchanged. v213 stopped early after6194native ticks,
not the requested12000: Pip-4c3c 4c3c0db4-75ea-42f5-8f91-b1c4814944d7 starved
20→4HP/0food at(-597.0893,64,-613.8262). Dara-573a also20→8HP/0food,
Oak-6432 20→17HP/0food. Do NOT resume blindly; exact saved injuries retained.
Clover20HP/20food, sleeping at(-466.5,64.5625,-376.5),rawiron1,dirt29,
3bread/3carrot,ironpick48wear; no armor/diamond. Latest boat/mine code is now
actually deployed. No actual boat cycle observed yet; night stopped exploration.
Linden still8HP but12food; actual camp provisioning improved him.

v214 frozen COPY of actual213 is auditing/adopting one existing nearby eastern
worker for Pip-4c3c, runtime runs/native-eastern-worker-audit-v214,
session55048,/tmp/villagers-v214.log,port25604. Initial adoption receipt:
cb79c23c-9623-45ac-9776-90c0f0a7813e at(-613.6656,64,-612.4455),24HP,
unchanged body/emptybag/crossbow. This is COPY ONLY. No actual adoption yet.
Append-only camp worker limit2ever permits this second additional existingworker.
Frozen audit remains running; wait for terminal/source-hash checks before rehearsing
from its saved copy. Need actual food delivery/recovery proof before main continuation.

## Eastern recovery diagnostics v214–v216; next v217

v214 frozen audit COMPLETE, source unchanged; copy-only worker adoption succeeded.
v215 ordinary4HP guard stopped at elapsed1 because saved Pip already4HP.
v216 copied-only1HP guard stopped at elapsed246, Pip1HP, all bodies retained.
DO NOT promote/resume that copy. Replay from stopped frozenv214 for next test.
Worker cb79 actually collected3potatoes but still17food, full24HP, no deliveries.
NativeCampMeals.reserveAvailable requiredfood>=18 in addition to the existing
FoodOffer.mayDeferOwnMeal policy. This prevented any investigation/urgent delivery
while its ordinary supply search continued. Removed redundant18 requirement;
existing own-food consumption still runs first; FoodOffer requires fullhealth and
food>=13. Real owned ration/contact/recipient acceptance remain required.
Build/test completed PASS558,/tmp/villagers-emergency-owned-ration-tests.log.
Need copied recovery proof from v214 before actual adoption/continuation.
Copied tool now offers --illager-min-health1..20(default4), valid onlyillagerrehearsal,
records observation-guard.json; changes no damage/health. Defaultguard unchanged.

## v217 failed delivery, v218 critical voice replay RUNNING

v217 from frozenv214 with redundant reservefood18 gate removed stopped atelapsed243,
Pip1HP. All192alive/no new/missing/dead; sourceunchanged. Worker had actualpotato but
followed newlyinjured Oak/Dara instead of criticallyinjured Pip; both signaledvolume12.
NativeCampCalls.volume now returns14 ONLY caller's ownfood0 and health<=25%max;
regularinjuredfood0 still12, injurywithfood10. Listeners hear only actualpublicsound,
not remotehealth/stock. Added regressiontest criticalstarvationvoice interrupts newly
injuredcall through real calls.select. Latest build559Java testsPASS,
/tmp/villagers-critical-voice-tests.log. New ration/voice changes NOT primarydeployed.
v218 runs/native-critical-voice-recovery-v218 from same unchangedfrozenv214,
2000ticks80TPS,copymin1,allresidents,session12719,/tmp/villagers-v218.log.
Inspect result/progress/terminal/population; do not assume safe recovery or mainadoption.

## v219 / v220 final recovery timing

v219 critical running test stopped atelapsed244/Pip1HP, all192alive/sourceunchanged.
Worker nowphysically(-610,64,-614),carrying1actualpotato, following Pip's public
criticalvoice. Walking/running fixes move it toward the correct recipient but the
1HPboundary stops before remaining80tick starvation interval can be tested.
Copied-only --last-health-recovery now optional with --illager-min-health1. At1HP
it permits normal AI only iffoodpositive or starvationtimer<60; stops20ticks before
next80tickstarvationhit. Other health/air/combat guards remain, defaultunchanged.
ObservationHealthGuard.lastHealthRecovery purehelper tested timer59/60/79,food,
dead/NaN. Latest561Java testsPASS,/tmp/villagers-prelethal-recovery-guard-tests.log.
Main does NOT yet expose/enable this option and remains stoppedactualv213.
v220 runs/native-prelethal-food-recovery-v220 from same frozen214,2000ticks80TPS,
allresidents,1HP+prelethalguard,session22981,/tmp/villagers-v220.log,port25604.
Need actual meal+wholecohort recovery, not path labels, before deploying to main.

## v220 failed safely; v221 supply-running replay RUNNING

v220 prelethal recovery test stopped elapsed308/Pip1HP, timer60. All192alive,
source unchanged. Worker approached Pip correctly but was still walking/running a
21nodehand-off detour from(-607.54,63,-616.72) tostand(-600,63,-614) atelapsed261.
No delivered meal or hatch yet; never call this a rescue.
Latest fix NativeCampWork calls NativeCampMeals.hurryForCriticalSupplies after
starting an existing safe dry food-storage path if it has heard an actualcritical
publicvoice and isfullhealth/food>=17. Native RUN1.1, same paidmovement charging,
run stops whenfood<17 or call expires; food contents remain unknownuntilcontact.
No newbody/food/health/position attributes.561Java testsPASS,
/tmp/villagers-critical-supply-running-tests.log. No main deployment.
v221 runs/native-critical-supply-recovery-v221 from unchangedfrozen214,
2000ticks80TPS,allresidents,1HP+prelethalguard,session98650,/tmp/villagers-v221.log.
Primary STILL actual213,4HP Pip,stopped. Need inspect result before acting.

## v221 failed; v222 retained-critical-call replay RUNNING

v221 stoppedelapsed304/Pip1HP/timer60, all192alive/sourceunchanged. Firstpotato
pickupstill304649exactly asv220; urgent supply speed had no observedbenefit.
NativeCampCalls.hear boundedqueue8 dropped firstnonpursuingvoice withoutconsidering
volume. Before firstmeal no activeinvestigation protected Pip's14call, so later
ordinaryvoices could evictit. Queue now dropsquietestuncommitted, refuses replacing
aloudercallwithquieter. Newtest criticalcall survives12laterordinarycalls,cap8.
NativeCampMeals.status now includes supplyRun receipt toprove actual fastsupplypath.
Latest562Java testsPASS +3PythonpopulationtestsPASS;
/tmp/villagers-critical-voice-retention-tests.log. Main stillactual213,stopped4HP.
v222 runs/native-retained-critical-call-recovery-v222 from unchangedfrozen214,
2000ticks80TPS,allresidents,1HP+prelethalguard,session51020,/tmp/villagers-v222.log.
Inspect terminal/progress/result/population. Neverpromote failed1HPcopies to main.

## Update v206–v213: owned boat cycle proved; actual continuation running

Evidence: docs/evidence/2026-11-11/native-owned-boat-and-mine/README.md.
Latest isolated build558Java tests PASS (two regression tests added), JAR at
/tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar.

Boat departure is now physically demonstrated in copies: v208 reused owned hull
02ee2e79-0b8f-4d23-8bbe-5e504d5cabc6,320paddle ticks,one landing; then crashed
on MineStair.save null direction at a reached old junction. All192 saved alive.
Fixed nullable pending direction with explicit NONE codec, valid only when selecting
at root/depth0/part0. v209 resumed that saved copy safely4000ticks, walked home.
v211 completed another4000ticks,380paddle ticks,one landing and dry return,
all192alive; no armor. It does not prove the shore survey helper itself ran.

BoatTravel now tries ordinary in-range/LOS native boarding before exact-bank walking,
replans to a reachable dry bank within3of the actual shifted hull, and records actual
landings/departures. Retains real owned-hull identity; no free boat or stock. Actual
parked dock and previously visited shore tags permit bounded native shore search
when hull is out of sight. One fair navigation admission per query batch; v210
revealed/fixed double admission. Trip away dock follows actual landing. Exhausted
mine entries remembered only after physically exiting and then excluded from retry.

v20728000ticks and v21024000ticks preserved192 but no boat. v2124000ticks with
2existingworkers and camp-farms enabled preserved192 but farm counters allzero.
Camp farming remains disabled/experimental in primary. No births claimed.

Actual continuation v213 currently started from v205 with latest source,12000tick
budget80TPS,sim16,camera(-550,90,-500),minhealth4/defaultfirst-hitguard.
Runtime runs/native-boat-mine-actual-v213,session77417,/tmp/villagers-v213.log.
Do not edit src/main while it runs. Inspect terminal/result before claiming outcome.
Main save is never replaced by a copy. All remaining Nether/End/dragon work persists.

## Historical primary v205 — superseded by v213 above

runs/native-four-regions-play/saves/NativeFourRegions, gameTime **298314**.
**All192saved humanoids alive**, no new/missing/dead UUIDs. Actual2000ticks80TPS,
sim16,camera(-640,90,-620),minhealth4+starvationguard. Completed PASS,
runs/native-reviewed-recovery-actual-v205,root session5640,/tmp/villagers-v205.log.
Old authored save unchanged; source unchanged during run; actual compiled13core
controller/survival classes EXACTLY match all3copied proof artifacts. No live clients.

Actual native golem4cbc8d37-298a-4ae5-b4e5-6fe240304039 KILLED husk
4b793243-b902-4336-86ea-82d6096cd184 at297166. Direct native death callback in
budget-native.json/result.json. Known3husk hits retained: Pip17→14 at296449,
then11 at296463; later healed and hit15.833 at297100. No waiveddamage/health/
invulnerability/cooldown/body/stock setters. Golem existed (sameUUID in sourcecopies).
Actual **Pip-3c29** saved15.833HP/15food/0saturation,2sandbag,emptyhands, at
(362.824,55,134.483). Threat resolved; further realfood/recovery stillneeded.
Do NOT call him actuallyfullhealth: only copied v202/v203/v204 ended20HP.
**Tansy-e028** actually healed8→20HP/20food,carrot1bag, at(-679.616,64,-623.535).
**Western Ember-c73f**12HP/7food/2sat,carrot1+potato1bag,at(-680.497,64,-622.488).
**Linden-8ea7**8HP/4food/2.4sat,emptybag (fed,needsmore recoveryfood).
**Dara-f13a**13HP/10food/4sat,emptybag. **Birch-67f3**17HP/8food,emptybag.
**Eastern Juniper-e3de**20HP/20food/2sat,emptybag at(-597.694,64,-613.154).

**Clover-edc5**20HP/18food, actual(-465.287,56,-377.507),bag stonepick70,
woodhoe18,dirt30,bread3,carrot4,stick4,rawiron1. Held earnedironpick42/offhandwoodpick4.
No actual armor/diamond/Nether/End/dragon milestone. Main now has all production
survival/startup/boat/mine changes throughv205; copied203 proofs and Labguard are
not a copied-save promotion. Actual nominal village progression advanced natively.
**Pip-460a founder**24HP,emptybag,(-684.647,63,-595.521).
**Additional existing worker40b3**24HP,emptybag,(-683.068,63,-631.403),crossbow.
Managed camp adults2, not births. Actual founderJuniper-e9b7 historical death retained.

## Reviewed combat mode — only by evidence gate; defaults remain first-hit

First-hit guard correctly detects contact but cannot verify post-contact escape.
COPY option --combat-recovery-from validates exactsavedbody matches actualfirsthit
receipt,>=16HP. Only knownactor/knownattacker continue above8HP; othercombatfirsthit.
All actual damage retained. Endtickhealth<=8 also stops. It exposed roof trapping.
Actual equivalent --reviewed-combat-from + three --recovery-proof paths +
--recovery-jar requires full unchangedworld filehashes, exact initialbody,3independent
passedcopies at least as long as actualbudget, retained population, finalactorhealth
at leastinitialhealth, and identicalcore13classhashes. Reviewedscope writtenout,
passed ONLY to thisprocess by VILLAGERS_REVIEWED_COMBAT_SCOPE env→Gradle property.
No persistentguard relaxation. **NEXT ordinary mainrun defaults first-hit again**;
husk is dead, do not pass obsolete v182 reviewreceipt now (world/body changed).
LabClient records every continuedhit plus nativeattackerdeath, knownactorfloor8.
Default globalminhealth4/air/starvation and unknowncombatfirsthit unchanged.
v205 result legacy combatGuard string saysfirsthit; actual reviewedCombat field
correctly documents3retainedhits. Future observerlabel fixed. Do not overwrite
forensicresult; reviewverification.json documents scope/classmatch.

Three copied2000tick proofssourcev182 PASS,192alive,sourceunchanged,Pipended20HP:
v202 native-grounded-danger-recovery-v202,
v203 native-grounded-recovery-repeat-v203 (directgolemkill log),
v204 native-grounded-recovery-confirm-v204. v20420HP/20food at(360,59,136).
Copiedcontrols tested allresidents; actual v205 camera+spawnresidency and native
golemkill directlyverify scope in primary.556Java testsPASS,3PythonpopulationtestsPASS.
New ReviewedCombatRecoveryTest verifies exactpair,8floor,invalidhealth rejection.
Build /tmp/villagers-reviewed-actual-guard-tests.log, usual isolatedJAR.

## Production changes and diagnostic findings

VillageSurvival: supported/drylocalescape destinations±12,y-3..+5,>=6fromobserved
threat, max3nativepathqueries/plan, fall2 and threatcrossing checks. Rotate failed
candidates with200tickcooldown, retryfailures40, replancompletedroutespromptly.
Remember lastVISIBLEthreat position120ticks; controller gives rememberedalarm
priority overordinarywork, no hiddenposition updates. Stay sheltered whenphysically
insidecoveredhome evenwhilealarmremains. No unarmeddefense/attribute changes.
**Critical fix**: do NOT query/stopmovement whileairbornefromjump/hurtknockback;
retainphysics and replanIMMEDIATELY onground. Initialfrozenbodywasmidair andallpaths
null; treatingthatfailure as40tickcooldown causedrapidrepeatedhits. awaitLanding
reports state; native detailed refuge/floor route records now available.
Startup: nativeBrain continues while savedmindloads asynchronously, firstmanaged
think runs immediately whenready, thennormal10tickstagger. VillagerMixin uses
Villagers.tickIfReady; oldvoidtick API retained. Session.controllerStarted transient.

BoatPreparation retains safeactivejob control during null/paused woodsearch, yielding
tosafety. WoodBootstrap publicrememberedbell route uses bounded32multiplier/128range
fallback, fall1,dry,existingworkarea+128nodechecks, retainsactualwalk. v193 frozen
native85nodesdry/inbounds vsnormalnull; v196 actually walked51nodefromsurface tobell.
**Boat departure STILL UNSOLVED**. v19624k PASS192alive; ownedboatapproachstalled
>1000ticks, thennexttrip reachedbellandfoundnoeligiblewood. v20118k PASS192alive
but NO boatphase (ordinaryquarry/foodforchild/rest), not a boarding test.
v198 frozen rawtrunk77bases inbell±64, ZEROeligible, ZERO nonpersistentcrownleaves.
Remaining local logs structural; do not weaken woodeligibility orgrantmaterials.

Important unresolvedboarding inconsistency: v196elapsed18400..19400 body
(-462.9668204858612,63,-373.43571223442365), statuswalkingtolaunch,seaPREPAREtaghome
(-463,63,-374), water(-462,62,-374), away(-464,63,-360). Bodywithin.47ofhomecenter,
yetwalkingbranchlabel. Need active route.launch/boardingApproach diagnostic; v201
probe added it but neverexecutedPREPARE. No interpretation ofcanReach asphysical
movement. Existingownedboat mayblockexactdockcenter; consider actualnativeboard
reach3/lineofsight BEFORE walkingexactbank, but no suchboardingfix implemented yet.
Rawwood is absent; reusing actualownedboat preferred overinventingresources.

MineStair prioritizes observeddescending/deeperrecordedjunctions, persisted
villagers:mine_lateral counter caps24newstepswithoutnewlowestfloor, thenactualexit/
retire/boatprospecting. Currentvaliddescent allowed, deepestfloor resetscounter.
v188 aquifer: allfreshlowdescents blocked78WATER+3AIR,noactualfreshsturdyfloor.
v19118k retiredandclimbedout, plannedmineralboat; laterrevisitedoldmine because
boatpreptimeout. May need honor exhausted-entryhistory/boatpriority. Preserveactual
floors/exit/supports, physicalcontact/drops/toolwear, paidseals/foundations.
v191 copiedrawiron1+ingot1 vs v187ingot1/no raw; receipt sampled40ticks cannot
attribute precise neworeevent yet. Do NOT claimnoadditionaliron basedstaleinventory.

## Invariants and identities

Clover UUID edc5ea2a-f488-4aea-961c-456c3f7200f1; Pip-3c29 UUID
3c29e5bd-14b7-435c-8737-d49ac9cb7fdb. Western Ember UUID
c73f2812-0861-417f-8180-38f671532381. Main always actual existing world; no rollback,
resurrection, replacementadults, copied-savepromotion, suppliedstock, NPCposition/
velocity/health/NoAI/invulnerability setters, syntheticbirths, clockskip or cropage/
moisture edits. Observerplayerspectatorcamera only. Native80TPS executes everytick.
Historical founderJuniper-e9b7 e9b7b192-009b-4158-bc2d-1a63456a6f91 drownedactualv108;
registryretainsher; neverreplace.192currentliving includes6nativepatrolbodiesfirst
recordedv167, NOT verifiedfamilybirths. Existingworker40b3 adoptionrecord unchanged,
atmost2additionalworkersEVER percamp includingdead/missing, append-onlyhistory.

NativeCellEscape MUST stayfalse: copiedv128 openedcell andvindicator killedcaptive.
NativeCampEntrance one owncamp passage,width<=2,height3,6lifetimeblocks,already2
removed+DONE. No furniture/ironbar/fence/door/fluid/falling-structure shortcuts.
NativeCampFarming stilldisabled/experimental: real grassloot/logs/hoes/seeds/table,
age0plant/drysoil/ordinarygrowth, protectlastseed,max32plots. Priorcopies16grass
NO seeds; repeatedsame sourceRNGdraws notindependent. No actualcampfarmproofyet.
Lastcarrot/potato areprotectedseed; gifts ofone root neednotbe usablemeals. Do not
change these tests/foodpolicy merely toforcehealing. Copiedstatsnotactualmilestones.
Sharedworktree verydirty; do notreset/clean/delete unrelated files orforensics.

## Next work

Main can resume ORDINARY boundedguardedcare (knownhuskdead) while solvingboat
boarding/resourceexpedition. Primarymedicalneeds: westernEmber,Linden,Dara,Birch,
Pip3c29stillneedrealfood; nobodyat0food in listed injuredcohort. Readfullsavedbodies
beforelongrun. Frozenboatdiagnostic should start fromCURRENT primary or a stopped
copywithsamephysicalownedboats; no copied-savepromotion. If continuing copied
mining, keep sourceclock/reserves incontext: v201actor wentordinaryfarm/childfood,
so no boardingproof. Goal still requires actualearnedarmor,diamonds,Nether,End,
actualVILLAGERdragonkill, plus conserved continued village/illagerdevelopment.
No subagents. Keep userupdates≤60sec; inspect livehandles before restart.

## Build and tools

source tools/java-env.sh; villagers_select_java (use newline commands).
./gradlew --project-cache-dir /tmp/villagers-mineral-gradle-cache \
 -I /tmp/villagers-mineral-isolated-build.gradle build test
Artifact /tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar.
Do not overwrite runtimepinnedJAR. No src/main editswhileactualclientlive.
Actual ordinary: tools/observe-native-play.py --runtime runs/FRESH --ticks1000
--min-health4 --tps80 --simulation-distance16 --observer X Y Z --allow-quarry.
CLIactual spacesbetweenflag/value required. Readscript for optionalreviewedcombat
mode; do NOT use oldreceipt againstnewworld. All current clients/copies terminal.
food-access-audit.py copiedrehearsals/frozenroutes preserve sourcehashes. Firstcopy
mustreachnativePID before anothercopy-stage on SAMEsource. Finishsourcehashchecks
beforeadvancingit. Sources191/196/201 are copiedworlds, not actualmilestones.
Historical constraints/evidence archived HANDOFF-history-through-v204.md andv191.
