# Active goal — main actual v213; stable v265 care proof before actual startup

Get a villager in the villager test map to beat Minecraft while villagers and
illagers develop through conserved native actions. Goal ACTIVE, unbounded.
No subagents authorized. Diamond equipment, Nether/End/dragon completion UNPROVED.
Historical details preserved in HANDOFF-history-through-v249.md and older archives.

## Authoritative world and invariants

Main: `runs/native-four-regions-play/saves/NativeFourRegions`, actual v213,
gameTime304508. STOPPED. All192 saved humanoids alive, no body differences from
v213 as verified by `/tmp/villagers-primary-current-v257.json`. Pip-4c3c4HP/0food,
Dara-573a8HP/0food; these are NOT the copied1HP bodies. Source main not advanced.
Original authored FourBiomesWar save remains separate and unchanged.

Critical residents:
- Pip `4c3c0db4-75ea-42f5-8f91-b1c4814944d7`, wooden fence cage near(-597,64,-614).
- Dara `573a309c-f0fc-4dd1-94cd-8246d352467c`, iron-gated room near(-635,64,-602).
- Oak `64324351-28f6-4bc1-a8f8-949f3300763b`, same gated room, initially17HP/0food.
Clover adventurer `edc5ea2a-f488-4aea-961c-456c3f7200f1`,20HP/20food, earned ironpick,
no actual armor/diamonds, near(-466,64,-376). Owned boat/mine history in archive.

No rollback/resurrection/copied-savepromotion/replacementadults/suppliedstock,
NPCposition/velocity/health/NoAI/invulnerability setters, syntheticbirths, clockskip,
cropage/moisture edits.80TPS executes everytick. NativeCellEscape MUSTfalse;
v128 opening killed captive. Camp entrance work bounded6lifetime actualblocks.
Primary CampFarming experimentalFALSE. First combat hit freezes ordinary trials.
Body-preserving existing-worker adoption max2additional EVER per camp; never
replace dead founders. Founder Juniper-e9b7 drowned and remains historically dead.

Existing western workers: founderPip-460a and adoptedJuniper-40b3(emptybags), same
large camp centered(-640,73,-606),radius90. Actual primary has only1additional
worker recorded. Copied v214 additionally adopted EXISTING cb79, NOT a birth:
`cb79c23c-9623-45ac-9776-90c0f0a7813e`, actual(-613.6656,64,-612.4455),24HP,
emptybag/crossbow unchanged. Source214 is stopped and unchanged. No actualcb79
adoption yet. Current newer qualification may select another body; don't assume
it will choosecb79 for main. Ledger records are append-only.

## Current source / artifacts / concurrent work

Very dirty shared tree: never reset/clean unrelated changes. Another task is
editing language/beliefs and worker/container access. Do not overwrite them.
Full integrated current source passed584Java tests in supply-loop build, but its
native first-supply path differed and FAILEDPip309ticks in v254. Thus whole-source
unit success is NOT a native rescue proof. New belief tests may raise total.
Latest stable snapshot v265 COMPLETEPASS:606Java tests,0failures,
source snapshot manifest unchanged during build. Earlier integrated
583/584 passes in `/tmp/villagers-goal-current-tests.log` and supply-loop log.
3Python populationtests PASS. No main client may run during source edits.

Isolated build command:
source tools/java-env.sh
villagers_select_java
./gradlew --project-cache-dir /tmp/villagers-goal-gradle-cache
-I /tmp/villagers-goal-current-build.gradle --max-workers=2 build test

Pinned OLD-language courier test chain, protected from concurrent source edits:
- v247 `/tmp/villagers-early-rendezvous-v246/villagers-0.4.1-dev.jar`
- v252 `/tmp/villagers-contact-first-v252/villagers-0.4.1-dev.jar`
- v255 `/tmp/villagers-isolated-supply-v255/villagers-0.4.1-dev.jar`
- v256 `/tmp/villagers-native-crowd-walk-v256/villagers-0.4.1-dev.jar`
- v257 `/tmp/villagers-crowd-segment-pace-v257/villagers-0.4.1-dev.jar`
- v258 `/tmp/villagers-root-handoff-memory-v258/villagers-0.4.1-dev.jar`
Latest v258 SHA c6cc26a5007027e750c170cfc8a984e449e4aa55098bbcaa40b0db58a758b08d.
Each has adjacent source snapshot+artifact.json with actual class/JAR/source hashes.
v255 replaced ONLY IllagerFood and NativeCampCalls main classes in v252; each later
patch replaces ONLY NativeCampMeals main class, old nested classes byte-identical.
No new nested state fields were added. `/tmp/villagers-pin-courier.py` implements
this controlled diagnostic pinning. Native source snapshots omit concurrent new
grammar API; never say isolated artifact equals whole current source.

## Verified native diagnosis and first care

Prior turns proved own food-store memory, real sprint/exhaustion, actual rootpairs
(seed+edible), and existing cage-gap rendezvous. Single-root delivery to an EMPTY
farmer is not food recovery; lastroot remains protected plantingstock.

v239 frozen path and v243 waypoint trace disproved premature-node hypothesis:
worker actually landed the first65-high ledge then fell trying66-high at sprint.
Speculative waypoint suppression was REMOVED. NativeBodyRoutes now observes24
native waypoints only; original Path.advance always runs. RouteProgress detects
no node-index advance, so repeated jumping no longer hides stalls.

Native sprint only on ground with next2nodes level within.25; walking pace.8 on
stairs/airborne. v244/v245/v247/v248 passed wall, zero failed steps, but missed
Dara deadline. Next1lookahead notbetter; restored2. Current source additionally
requires actual next-segment body clearance for sprint; slow native physics near
bodies, normal sprint on clear level ground. No attribute/body setters.

v250/v251 frozen audits: valid block-safe contact stand(-633,65,-610),18nodes,
but ravager22b5e173-6501-4cb6-bb5b-71352d10f681 at(-638.28,64,-619.63) occupies
its short entrance route. Complete body avoidance often gives no path or a long
building detour. Nearby lever stands alone don't prove handoff reach. Understood
requests now try ACTUAL giving stands (including valid open-gate ray) first, then
wider lever meeting area. Hungry callers can prepare own safe inside-gate position
after their OWN public call, before listener reaches word-hearing range. No gate
is opened by this rendezvous, and existing gap approach still needs a listener.

v252 failed582ticks/Dara1HP, all192alive. v253 reached1971ticks beforePip's NEXT
starvation boundary; all192alive. REAL gifts in v253:
-3047872actualpotatoes toPip; ate1,kept1seed.
-3050372actualpotatoes toDara through native lever-opened iron gate; ate1,kept1seed.
-3057271bread toOak through same gate, made from actual gatheredhay/wheat; atebread.
Both gate receipts confirm leverRestored=true,gatesClosed=true. No healing claimed.
Experimental crowd fallback in v253 had no sampled proof of use; initially removed.
Evidence: docs/evidence/2026-11-11/native-gate-meals-and-bread/.

v254 integrated source failedPip309ticks: firststorage trip changed; obtained7beetroot
later. v255 isolates lowfoodcriticalcalls and3-recipe bread batching, but firstDara
route stillfailed585ticks. These failures preserve192 and sourceworlds unchanged.

v256 narrow crowd walking test: ONLY when urgent contact path has no complete
body-avoiding route, a complete block/border/drop-safe native path may traverse
pushable RAVAGER bodies. Refuses other blocking bodies or ravager targeting donor.
Native collisions/pushes untouched; no ravager setter/NoAI. This path ACTUALLY USED
(nativeCrowdWalk=true) and physically passed beast, but all-walking was late588ticks.
v257 resumes RUN on clearlevelsegments, slowsnearactualbodywalks: physically fedPip
andDara first time, gotmorepotatoes,fedOak, crafted3realbread. FailedPip'sNEXTfood
boundary1975ticks; all192alive. Thus initial rescue/crafting proved, NOT sustain.

## Repeat supply fix under test — v258

NativeCampCalls keeps criticalvolume14 for HP<=25% andfood<=2 (old onlyfood0).
One rawroot cannot heal a1HP body, so donor no longer treats that as reduced urgency.
Meaningful boundary regression tests food1/2 vs3, healthycaller. IllagerFood can
batch up to3 actual native bread recipes at a physically reached bench after a
critical heard request; consumes3wheat/output each, checks capacity eachrecipe,
charges exertion each. v257 actually crafted3bread from9gatheredwheat.

v257 exposed donor carrying1potato then3bread whilePip starved. Global rootpair
eligibility rejected that singlepotato despite own previous seed+meal delivery.
Current NativeCampMeals stores bounded16own-action tags
villagers:given_root_pair:<recipientUUID>:<itemid> ONLY after actual pairedrootgift.
Fresh heard caller plus this own history permits investigating a single-root
refill. Default unknown-recipient single-root availability remainsFALSE.
Actual give first checks contact/range, then reads recipientstock. Empty farmer
STILL requires2roots; bundlePlan rejects insufficientsource/fullcapacity unchanged.
Donor's remembered pair never bypasses that contact stock/capacity validation.
Eligible delivery selection favors higher actual nutrition. Stable offeredcopy
captured before shrinking source records pairtag reliably. No new nested fields.

v258 COMPLETEPASS `runs/native-root-handoff-memory-v258`, session90426 closed,
/tmp/villagers-v258.log,4000ticks80TPS/allresidents/min1+prelethalguard, source214,
pinned v258 JAR. Completed4000ticks/gameTime308508, all192alive, no new/missing/dead
UUIDs, source214unchanged, terminalexit0, realresultstatuspassed.12recorded gifts:
Pip rootpair304779 +beetroot305889,306659; Dara rootpair305039 +beetroot305409,
306289 +bread307289; Oak beetroot305449,306249 +bread307229 +apple307579;
otherrecipient67f3 apple308119. Donor actuallycrafted3bread from gatheredwheat.
Saved pairtags forPip/Dara potato survive. NO sampled single-potato refill yet;
do not attribute fullpass specifically to that eligibility change. EndPip1HP/3food,
Dara1HP/8food,Oak5HP/11food; timers0. Positivefood/retainedlife, NO healing claimed.

v259 COMPLETEFAILED `runs/native-care-save-reopen-v259`, session73047 closed,/tmp/villagers-v259.log,
source STOPPEDv258(savedactualstocks/bodies), samepinnedv258artifact,8000ticks80TPS/
allresidents/min1+prelethalguard. Stopped873ticks/gameTime309381 on Sage-2a52,
`2a526eee-ec74-45bf-aac8-04c2323a1755`,1HP/0food/water near(-821,62,-767).
All192alive/source258unchanged; Pip3food,Dara8,Oak11 retained. Sageinitial214 was
20HP/4food at(-820.437,65,-756.723), home(-819,64,-781),emptybag. End25812HP/0food
atSAMEposition; copy259 thenmovedbutstarved. Trace shows ordinaryVILLAGE resident
labeled waitingforcellgate evenoutsidecamp! Earlycall readiness was overbroad.

v260 fixes this sourcebug: new NativeCampMeals.captiveHome requires actor's OWN
loadedhome in a native illager_barracks AND bodyinside thatstructure. Bothrequest
andwaitAtGate enforceit; ordinaryvillagefoodwork can proceed withoutcampgatewait.
Existing captive care remainsenabled. No unrelatedsource rolledback.
v260 COMPLETEPASS `runs/native-captive-call-scope-v260`, session46562 closed,
/tmp/villagers-v260.log,source258(saved12HPSage/positivefoodPipDaraOak),4000ticks,
sameallresidents/prelethalguards, isolatedscopefixartifact
`/tmp/villagers-captive-call-scope-v260/villagers-0.4.1-dev.jar`,sha
08798f11097e9899d939b1d3eb3825d8e48bca416872c4de685f45209c37c092.
Completed4000ticks/gameTime312508; all192alive, no new/missing/dead IDs, source258
unchanged, exit0. Sageactually collected4storemeals at(-844,64,-781),ate,waterfalse,
gateWaituntil0, health12→14/food15. Copied successful branch258→260 spans8000ticks.
EndPip1HP/7food,Dara1HP/8food,Oak5HP/11food. No fullhealing/popgrowth claim.
Integrated scopebuild COMPLETEPASS606Java tests,0failures,
`/tmp/villagers-captive-scope-tests.log`.3Python populationtestsPASS.
Never promote copiedsave or call8k care indefinite recovery.

v261 integratedcurrent606-test artifact `/tmp/villagers-integrated-care-v261/`:
FAILPip303ticks; firstsupply(-616,64,-617)7beetroot at304668 instead of oldfirststore
(-617,64,-603)4potato at304565..304580. First40/80activitylookingrealcampsupplies,
no target. All192alive/source214unchanged. Scopefix alone doesn'tresolveintegration.
v262 controlled ONLY newNativeCampWork mainclass inside successful oldchain:
PASS1000ticks/all192alive; initialpotatostore304565. Thus the3NativeCampWork changes
(facecontact/readpredicateorder) ALONE do NOT cause slowfirsttrip; do not rollback
correct current storageaccess. Controlledartifact /tmp/villagers-storage-face-control-v262.

v263 COMPLETEFAILED `runs/native-first-scan-trace-v263`, session99628 closed,
/tmp/villagers-v263.log,source214,1000ticks/allusualguards. Based onfullintegrated261
with ONLY NativeCampWork mainclass instrumented: console CAMP_SCAN_TRACE records
firstscan position,target,home,food/housing and operator-only oldstore visibility/
accessibility flags. Temp source `/tmp/villagers-first-scan-trace-v263.java`, artifact
folder sameprefix. Trace coordinates/time are OBSERVATIONS ONLY, never actorinputs;
no trace code added to main source. Firstscan304511 correctlytargetedoldpotatostore,
visible/local/storagealltrue, native firstpickup fast. StillfailedDara584ticks,
all192alive. Never deploy diagnostic traceJAR.

Bytecode comparison /tmp/villagers-work-integrated.javap vs work-control.javap
found v261JAR STILL scanned with BlockContact.interactionPoint(...16), whereas
CURRENTsource used visible(...). This was stale/concurrently changed source,
not an unexplained native scan inconsistency. CandidateScout visibility is current;
actual pickup stillrequiresBlockContact.interactionPoint(...3).
v264 buildtestspassed BUT sourcechanged duringbuild(NativeCampWork,
NativeMealVolunteers), so its artifact was REJECTED, not pinned/deployed.

v265 STABLE integrated snapshot `/tmp/villagers-source-snapshot-v265/` contains
346copiedsource/resource/build files; source-manifest.json verified beforecopy and
afterbuild. Build inside snapshotwith originalwrapper -p SNAPSHOT avoids root
source races.606Java tests0failures; /tmp/villagers-snapshot-build-v265.log.
JAR `build/libs/villagers-0.4.1-dev.jar`,SHA
f88f8f26e4cfbc060efbbe3e84d7cb48e4464e5082e7dc19dbedb8ec670d20e6.
Native `runs/native-snapshot-care-v265` COMPLETEPASS4000ticks/gameTime308508,
all192alive/no newmissingdead UUIDs, source214unchanged, terminalexit0. Firstfull
current integrated source snapshot carepass; no diagnostic traceclasses.
Main re-audit `/tmp/villagers-primary-current-v265.json` exactlymatches all192
v213body rows; main not advanced. No owned goal runtime remainslive now.

Concurrent source includes NativeMealVolunteers prototype under Systemproperty
villagers.camp.volunteers; it was OFF in these trials. Don't claim volunteered
stock or growth from unexecuted prototype behavior.

## Next actions / full objective

1.Inspect v258 and latest integrated tests; keep milestone claims tied to actual
native receipts and food/health, not labels or unused fallback code.
2.Prove sustained care, repeatfood and eventual healing in copied current bodies.
v265 resolves firststorage integration at a stable source point. Verify longer
save/reopen care/reproducibility with this exact606-test snapshot before deployment.
Use immutable snapshot sources/artifact, not concurrently movingroot classes.
No copying trialsave into main. Actual startup must freeze before ticking and
adopt exact verified existing worker before unfreezing. Current observe-native-play
NOW has --last-health-recovery with --min-health1 (added concurrently); build.gradle
andLabClient use it, so old claims this optionis missing areobsolete. BUT worker
adoption stillwaits camera+3seconds; safe frozen preflight adoption isNOTimplemented.
Do not blindly resume main or spend its starvation interval waitingfor enrollment.
3.Then actual population growth and earned armor/diamonds, Nether/End, actual
villager dragon kill. All remain required. No code yet supports endgame progression.
Equipment-only copy was considered but NOT RUN: persisted forced chunk loads keep
starving cohort active. Do not silently unload them to claim wholeworld progress.

Copied care command:
python3 tools/food-access-audit.py --source-world runs/native-eastern-worker-audit-v214/NativeFourRegions
--runtime runs/FRESH --server-home /home/cmkrist/Games/VanillaPerfectedCompanion
--production-jar PINNED_JAR --illagers --rehearse --camp-meals --camp-passages
--all-residents --illager-ticks 4000 --illager-min-health 1 --last-health-recovery
--tps 80 --port 25604
Use actual spaces between option/value. Timeout isn't completion; inspect PID/terminal.
