# Active goal — actual v302 at game time 321513

Get a villager to beat Minecraft in the native test map while villagers and
illagers develop. Goal ACTIVE and unbounded. No subagents authorized. No births,
new population, diamond equipment, Nether/End or dragon victory proved yet.
This remains the full objective; care and mining are intermediate work.

## Authoritative world and artifact

`runs/native-four-regions-play/saves/NativeFourRegions` is STOPPED after actual
v302 at game time **321513**. Never restore an earlier checkpoint or promote a
copy. All 192 existing original-cohort humanoids remain alive, no new/missing/
newly dead IDs. Historical founder Juniper-e9b7 remains dead (never resurrect).
Original FourBiomesWar unchanged. No owned game processes remain live.
Actual result: `runs/native-actual-supply-and-prospect-v302/result.json`,
budget-native.json, bodies-after.json. All server/saved health, not client HUD.

Actual v302 used immutable v301, 630 passing Java tests, class bytes checked
against tested JAR. Snapshot `/tmp/villagers-source-snapshot-v301`; durable
source ZIP/JAR/manifests in `dist/native-care-v301/`. SHA256:
c963de9049f360215771614d8e6997336f385cdea050e75894f942aec44227b7.
It advanced 317512→321513 (4001 elapsed native ticks), native80TPS verified,
normal saved exit0, no wall/combat stop, source unchanged, zero enrollment.
Root is NOT the deployed artifact. Previous actualv292 used v291.

Clover `edc5ea2a-f488-4aea-961c-456c3f7200f1` is awake at
(-471.4283696712345,62.9375,-345.3348509141077),20HP/20food, mind age258053,
fatigue.28125. Earned iron pickaxe now in bag (damage100), stonepick damage70,
dirt35,bread3,carrot1,sticks4,rawiron1; heldwoodenhoe damage19, offhandwoodenpick4.
No new iron/armor/diamond claim. Actual13stoneblocks+5quarrydirt worked thisload.
Retired the wet mine after24lateralsteps, kept114floor supports and3entries,
1exhaustedentry. New quarry attempts correctly rejected water/unnatural farmland
and returned to surface; now following26-node survey route to(-474,63,-338).
Courtship active with Oak4387, family blocked on spare sheltered bed; no birth.
Keep observer **(-555,90,-495)** (script default). Camp-only observer left hero
loaded but not ticking. Simulation12 does not prove all192 ticking; actualbudget
reports35villageractors/3workers. Copied --all-residents forces56savedmanaged
villagers plus cohort. Client latest health can be wrong: use server/saved bodies.

Camp founder Pip460a24HP/20food, emptybag, position
(-684.4999611060571,63,-603.5448761489675). Check nativebudget exact food again
before interpreting own need; hands/crossbow unchanged by automation.
Maximum TWO additional workers EVER already used: Juniper40b3 and couriercb79.
Never enroll replacements or extra adults. Couriercb79 is24HP,emptybag/crossbow,
(-648.3725536898593,64,-616.6450824236389). Native receipts: founder3breadrecipes,
3deliveries; courier2deliveries. See actual-v302.json for conserved item details.
Juniper40b3 remains alive24HP, no gifts/recipes; survey/rest work needs diagnosis.
Camp ID -175646985019319, center(-640,73,-606), radius90.
Main quarry/meals/passages true, CellEscape and CampFarming false.

Current saved patients (reread bodies-after before each scope):
- Mossfad4:3HP/14food,sat10.4,timer0; was20HP/1food, fed before lethal boundary.
- Willow4ed3:6HP/5food,sat2,timer0; was16HP/0food,timer70, fed before boundary.
- Dara573a:6HP/15food (native recovery from1HP), retainedpotato1.
- Pip4c3c:1HP/5food,sat1.6,timer0; Oak6432:2HP/9food,sat0,timer0; eachpotato1.
- Tansye028:6HP/10food,sat4,timer0, retainedcarrot1.
- Birch67f3:11HP/1food,sat0,timer0, potato1, at(-588.35,64,-587.10).
Birch is next deadline: previous all-residentv301copy hit1HP after5239from317512,
so CURRENT321513 has different but potentially short care horizon. Never use the
failedcopy state as actual. Source actualworld must be STOPPED before a freshcopy.

Preserve injuries and native reserves. No supplied gifts/items/adults, health/
position/velocity setters, NoAI, immunity, clock skips or crop-age edits. First
actual combat hit stops ordinary trials. Failed copies remain rejected/preserved.
NativeCellEscape v128 killed a captive and stays disabled.

## Source and native evidence v285–v297

Evidence `docs/evidence/2026-11-11/native-parent-edge-and-supply/` contains compact
receipts, artifacts, actualv292 and mining diagnosis; prior details archived in
HANDOFF-history-through-v283.md and earlier histories.

v287 NativeBodyRoutes checks native parent→neighbor edges, instead of diagonal
rectangles from the original body. The legal west-then-north tent exit now works;
founder moved, gathered, delivered3 native meals. Mixin extends NodeEvaluator.
v286 was INVALID: inherited @Shadow failed, entity loading discarded176 IDs;
16/192 remained in rejected copy. Never deployed. Probe now preloads evaluator
before opening a copied world. Never claim every copied trial preserved192.

v288 NativeFoodStorage allows one supported shallow-water waypoint for healthy
trips, head clear. Critically injured paths remain dry. Sage crossed and ate3
stored meals,20HP/15food. LaterMoss stop6063ticks; all192 alive.
v291 request capacity16 and commitment to equal-ranked public requests; stronger
calls can preempt, expiry remains. Native copy reached9287 before Birch67f3
health stop. All192 alive; actual v292 then safely ran4001ticks using this source.

Candidate trials and deployment (v294–v300 not deployed):
- v294 recovery-sized handoffs at actual contact; actual stock/capacity/donor
  reserves and planting root preserved.628tests pass. FAIL1194Willow, no gifts.
- v295 retains hay-return route and releases idle cooldown to storage.628pass.
  FAIL1194Willow again; not a fix by itself.
- v296 retains advancing workbench path; three paid bread recipes allowed for
  any understood injured request.628pass. Native founder crafted3bread, gave1
  toWillow before deadline, removed2realbread from chest and gaveboth toPip.
  Receipt donor2→0/receiver0→2. Moss health stop2050ticks. All192 retained.
  This proves larger conserved handoff, not sustained survival.
- v297 FROM verifiedv296; only food-task continuation and retained storage route.
  A selected bounded NativeCampWork storage target takes precedence over old
  hay recollections when no active harvest/craft target. Retains matching actual
  path until stalled120ticks, then marks failedstep/replans.628tests pass,
  SHA0f9dd3b7d5eed6b5341162f4d9d423ffd89fee48264b0ac4fdef1b3c46fd4bb6.
  `/tmp/villagers-source-snapshot-v297`, durable `dist/native-care-v297/`.
  Copied v297 PASSED8000ticks, all192alive; Willow20HP/18food,Moss3HP/10food.
  v298 sameJAR with frozenexactworkerstartup FAILED1196Willow. Metleverstand
  but no actual handray, repeatedly chose same one-node path. All192alive.
- v299 from297: failed reached handoff-stand memory perrecipient,400tickexpiry,
  max64entries; triesanother native stand without changing lever/ray/reach.
  629testsPASS, SHAef0a167942c1490a1731a68dd06c6a3764ea88715551227f26888bc97d2049ab.
  Frozenstartup deliveredtoWillow at1051ticks, then FAILED2043Moss (westtrip late).
  All192alive. Runtime `runs/native-alternate-handoff-stands-v299`, stopped0.
- v300 from299 +ONLY NativeCampCalls earlier halfhealth/food<=2volume14 and
  boundarytest fromv282.630testsPASS, SHA
  4ccd2fbdc0b7c3bb6a8a3f660cae026552a108ade1109482197a3eed72a24693.
  `/tmp/villagers-source-snapshot-v300`, log `/tmp/villagers-snapshot-build-v300.log`.
  Copied v300 FAIL2045Moss; all192alive; earlier call alone insufficient.
- v301 from300 compares complete native routes to hearing versus lever; takes
  the shorter route. Actual handoff still requires native lever/ray/contact.
  630testsPASS, SHAc963de9049f360215771614d8e6997336f385cdea050e75894f942aec44227b7.
  Snapshot `/tmp/villagers-source-snapshot-v301`; durable `dist/native-care-v301/`.
  Frozen copied run `runs/native-nearer-hearing-route-v301` reached5239ticks
  before Birch67f3prelethalstop. Willow andMoss bothfed; all192alive.
  This proves4000tickwindow, notindefinitecare orfull8k.
  Actual v302 PASSED4001ticks usingexactv301; source317512→321513, normal0.
  Exact317512scope `/tmp/villagers-care-resume-scope-v298.json` verified by
  copies298/299/300/301 andactual302, zeroenrollment,bodybefore==after.
  This scope is NOW STALE afteractual302saved321513. Never reuse it.

Root includes concurrent language/beliefs/NativeCampWork/MealVolunteers work.
Preserve unrelated edits. Use immutable snapshots derived from manifest, not the
moving root. Root half-health critical-call volume14 is experimental; snapshots
v291–v299 retain original25% critical threshold. v300/v301 deliberately combine50%
critical with supply/stand changes. Exactv301 is now DEPLOYED; root is not. Do not deploy root wholesale.

## Mining and next work

Frozen v293 survey shows immediate down-support cells Y54 are WATER. No
structural-cover bug. Do not relax floor/footing protections. Lateral search cap24
can retire a wet exhausted entry and seek another observed dry prospect.
Copiedv297 hero reached24lateralsteps,retiredwetentry,retained104floors and
returnedtobed by325512. No newore. New read-only probe includes quarry status
in future adventurer progress; no actor changes.
Actualv302 ALSO retired wetmine and began2newquarries/survey (see above). Continue toward
iron armor/diamonds/Nether/End; do not let care alone replace the victory task.
EnderDragon native damage accepts Players or ALWAYS_HURTS_ENDER_DRAGONS sources,
not ordinary villager arrows/melee. Bytecode `/tmp/villagers-dragon-native-bytecode.txt`.
Any eventual victory must be actual attributable native damage, no fake kill.

## Launch and provenance

`tools/observe-native-play.py --source-snapshot /tmp/... --runtime runs/fresh
--ticks N --min-health1 --last-health-recovery --allow-quarry` resumes actual.
Use fresh frozen alreadyManaged worker scope for low-health startup:
NativeCareStartup schema1 residentUUID,workerUUID,gameTime,workerBody exact native
signature,residentHealth,residentPosition[3]. OLD v292scope is313511 and STALE; v298scope317512alsoSTALE.
v298scope was317512 and is NOWSTALE. Nextscope mustmatch321513 and current
resident/worker bodies. cb79 currentlyemptybag/crossbow, exactposition above.
Validate fresh signature in a frozen copy before actual resume.
No new enrollment. Resume LAST after observer midpoint/TPS/guard setup.
`tools/food-access-audit.py` copies a STOPPED locked source, pins JAR, uses
--illagers --rehearse --all-residents --camp-meals --camp-passages --quarry
--illager-min-health1 --last-health-recovery --tps80 --port25604. Fresh runtimes.
Full cohort and adventurer progress receipts every40ticks/timeline400.
Check terminal.json and emergency-stop, not just process exit0. stop-observation
file requests safe next40tick shutdown. Never stop other task processes.

Build snapshot with Java env helper and original wrapper:
`./gradlew -p /tmp/villagers-source-snapshot-vNNN --project-cache-dir
/tmp/villagers-snapshot-gradle-vNNN --max-workers=2 build test`.
Verify source manifest/test XML/JAR SHA, write artifact.json before launch.

## Next action from current actualv302

Copy CURRENTSTOPPED321513 with pinnedv301 and all-residents/quarry/meals/passages.
Observe Birch67f3 and workers’ real supply routes; no new enrollment. Use native
progress JSON key **villagerFoodStores** (56entries) and timeline to see each
patient’s server health/food/navigation/gatewait, not only criticalVillagers[].
This was already recorded by the probe; no need another telemetry implementation.
Clover progressed faster in actual than copy: ironpick82→100, wetentryretired,
nowdry-survey north of home. Keep coverage, prove newore/armor thenNether/End.
Sustainable care still unsolved: finitechests runlow, farming actualfalse. Earned
seed/hoe/crop production and sparebeds/births remain needed, not just more care
fixtures. Preserve other task’s MealVolunteers/NativeCampWork/language edits.
This turn made actual/source progress; goal remains ACTIVE, notblocked/complete.

## Current turn work after v302 — inspect live handles

Main STILLSTOPPED321513; no newactualrun yet. Prior turn PROGRESS (actual4001ticks).
Fresh scope `/tmp/villagers-care-resume-scope-v303.json` matches321513/Pip4c3c1HP
andcb79emptybag/crossbow at(-648.3725536898593,64,-616.6450824236389). Verified
frozenalreadyManaged/enrolled0 copies303/304/305/306; notstale until mainadvances.

New evidence `docs/evidence/2026-11-11/native-short-hay-and-reserve-search/`:
-303 baselinev301 FAIL1241Birch67f3. cb79 loops3nodehayroute every40ticks without
 usefultravel. Mainunchanged, all192alive.
-304 body-progressacrossreplans FAIL1243Birch; bodyoscillationcountsasmovement.
-305 closest-goaldistance for SHORT<=8nodehayapproaches,120tickstall; longdetours
 retainnativecursorprogress.631testsPASS. SHAef71f96f0415559c6a1051ff6b2b80b46dfd4292da5d4ff6bd24a48d98283bc7.
 Snapshot `/tmp/villagers-source-snapshot-v305`. Native FAIL10030onIris240e,
 all192alive. COPIEDcourier escapedloop, took2apples andgaveboth toBirch at322312,
 beforeolddeadline; crafted3realbreadrecipes andgave3bread toWillow and3toOak.
 Thisprovesa10kcarewindow, notindefinitecare; notyetdeployed.
-306 from305 +separateworker’s EXISTING3file reserve-search.patch (AgentMind,
 VillagerController, AgentMindTest) isolated into SNAPSHOTONLY.633testsPASS.
 SHA62b7ec1baea819eb9c6eff40483ac63d35e116eae933816de5e4359d1127573b.
 Native STILLFAIL10030Iris. EarlierRecoveryRest branchinterceptsactionlayer
 whenfood>0andrequestpending. ThusnoIrisrescueclaim.
-308 from306 +ONLYfood>6 (was>0) in recovery-rest wait, retainingtravelreserve.
 633testsPASS, SHA641be689417d8c1ce9b5762cb7994d1184e8c8116a0ab077ee205d9da8cd2081.
 ACTIVE COPY `runs/native-reserve-search-unblocked-v308`, execsession33428,
 log `/tmp/villagers-v308.log`, port25604,14000ticks allresidents/quarry/meals/
 passages80TPS/frozenexistingworker. INSPECT confirmedprocess/terminalbefore
 restarting. Currentmainworldprovenancecheckedatend, don’tadvancemainwhilelive.
 ROOTdoesNOTcontainthe3filereservesearchpatch! OnlyrootRecoveryRestwaitthreshold
 modifiedsurgically; otherthreadownsbroaderfoodsearch. DoNOTdeployrootwholesale.

Beyondcare: frozen307 olderrejectedminecopy at322798, heroY56, waterfloorY54.
 Operatoronlyfutureexcavationraychecks showall4directions supportededgestance
 butZEROactualdirtplacementray/anchor. DoNOTimplementwetdirtfloorbasedonassumed
 reach. ConsideractualpaidfallingSAND/GRAVEL column afterrealexcavation instead,
 onlyifnativephysics/contact/earnedmaterialsprovepossible. Sourceunchanged0ticks,
 all192alive. Probe `ResidentMealProbe` now has wetFloorPlacementSurvey inINITIAL
 (finalactor usesinspectfalse). v309ACTIVE frozenexpandedcolumn/drop-contactsurvey
 `runs/native-gravity-floor-feasibility-v309`, exec42980, port25605, log
 `/tmp/villagers-v309.log`. Older sourcev293copy is322798, NOTcurrentmain.
 No productionminingconstructioncodewrittenyet; resolvephysicalfeasibilityfirst.
