# Active goal — actual v327 at game time 326749

Get a villager to beat Minecraft in the native test map while villagers and
illagers grow. Goal ACTIVE, unbounded. No subagents authorized or spawned.
No births, population growth, new armor/diamonds, Nether/End or victory proved.
Previous turn advanced actual4001ticks. Current turn advanced actual5236ticks,
proved supply/routing changes and a physically feasible mining construction.
Care and mining are intermediate work; the full victory objective stays intact.

## Current authoritative world

`runs/native-four-regions-play/saves/NativeFourRegions` is STOPPED after actual
v327, **game time326749**. Latest result `runs/native-actual-recovery-and-housing-v327/`:
result.json,budget-native.json,bodies-after.json,client.log. It PASSED4001native
ticks from322748 at80TPS, normal saved exit0, no combat/wall stop, source unchanged,
old FourBiomesWar unchanged. All192 existing original-cohort bodies alive, no new/
missing/newly-dead IDs. Historical founder Juniper-e9b7 remains dead. No copied save
was promoted. Earlier actualv310 advanced1235ticks then stopped on Birch; total
actual progress this turn5236ticks. No owned game/server processes remain live.

Deployed source **v324**, not root or rejected offer prototypes.634 Java tests
pass. Snapshot `/tmp/villagers-source-snapshot-v324`; durable JAR/sourceZIP/
manifests `dist/native-care-v324/`. SHA256:
c42751beb7cd401de9bea3fa4667782d219e621af59d696270b247ddda01604e.
Actual client class bytes matched this JAR. Frozen existing-worker preflight kept
exact body/stock, enrolled0. ALL old scope files through v312 are NOW STALE.
A fresh scope must use326749 and current saved resident/worker signatures.

Birch67f3 is1HP/5food,timer0,sat0, at(-588.4349840001227,64,-587.3339889343096),
emptybag. He survived by consuming his OWN last potato under new survival logic,
then received1real gift (native foodHelp.received1). Saved potato1→0,food0→5.
The copied native trace records the courier’s apple handoff; actual’s final report
has receipt count but not item identity (do not conflate copied item details with
actual proof). New root-only report fields lastGiven/lastReceived improve future
actual receipts but are NOT yet in the deployed artifact.

Other actual patients at326749:
- Willow4ed3:19.5HP/15food (was6HP/5food before327).
- Dara573a:10HP/16food (was6HP/15food); Oak6432:15.833HP/15food (was2HP/9food).
- Pip4c3c:1HP/17food,sat17,timer0, retainedpotato1.
- Linden8ea7:13HP/1food,sat0,exhaustion3.2907364,timer0, EMPTYbag, at
  (-680.5000182975444,64,-623.3035041275415). NEXT main care deadline: no root to
  eat if stranded. Fresh copy of CURRENT326749 required before another actual run.
- Distant homeless Willow1104:19HP/9food,emptybag,(-422.51,63,-816.05).
- Distant homeless Iris240e:18HP/8food,emptybag,(-412.50,63,-825.16).
  These distant bodies do not normally tick at the midpoint but all-resident copies
  expose their next-day starvation risk. Do not claim all192 tick in actual.
Read bodies-after for remaining exact health/food, never client HUD alone.

Current worker bodies:
- Courier cb79 `cb79c23c-9623-45ac-9776-90c0f0a7813e`:24HP/20food,
  (-657.6999999880791,64,-626.4517814946744), inventory[string3], crossbow mainhand,
  empty offhand. Native reloaded5slot bag should be[string3,air,air,air,air]; verify
  exact Java signature in a copied frozen startup. Latest actual delivered5,
  crafted3breadrecipes (real wheat).
- Juniper40b3:24HP/20food,string3,crossbow,
  (-619.8899234407583,63,-563.1105803601517), delivered1,crafted3breadrecipes.
- Founder Pip460a:24HP/16food,emptybag/crossbow,
  (-618.8818288200656,65.76636799395752,-603.9661487324279), delivered2.
Exactly TWO additional workers EVER already used (40b3,cb79); no more adoption or
replacement adults. Founder460a lacks native_illager_worker tag and cannot use
alreadyManaged-worker preflight. Camp ID-175646985019319, center(-640,73,-606),R90.
Main quarry/meals/passages true; CellEscape and CampFarming false. CellEscape v128
killed a captive and stays disabled. No body/health/position/velocity/NoAI setters,
supplied items/adults, clock skips or crop-age edits. Native meals/healing only.

Clover `edc5ea2a-f488-4aea-961c-456c3f7200f1`:20HP/20food,sat6, mindage263278,
sleeping in owned bed(-466.5,64.5625,-376.5). Bag: stonepick(damage70),
ironpick(damage100),dirt44,bread2,carrot1,sticks4,rawiron1,wheatseed1. Heldwoodenhoe
(damage28),offhandwoodenpick(damage4). Worked9real quarrydirt total across310/327,
but no newiron/armor/diamond. Wetmine retired,114floor supports,3entries,1exhausted.
Spare-bed preparation now gets a turn before continuous adventure work, but latest
actual bedding counters all0; no growth claim. Family also needs more food (2bread
below3bread cost). Keep observer **(-555,90,-495)**. Actual sim12/budget35villagers
versus copied56managed villagers: report coverage honestly.

## Source/tests and native evidence this turn

Evidence `docs/evidence/2026-11-11/native-short-hay-and-reserve-search/`.
Older source chain/actualv292/v302: native-parent-edge-and-supply evidence and
HANDOFF-history-through-v302.md. Preserve other thread’s language/beliefs/work.
Use immutable source snapshots, never compile moving root for real play.

-303: fresh actual321513, pinnedv301, frozen worker. FAIL1241Birch, all192alive.
 Courier reconstructed3-node hay path every40ticks without escaping an oscillation.
-304: body movement across path replacements still counted oscillation as progress.
 630tests pass, FAIL1243Birch.
-305: closest-goal distance across SHORT<=8node hay approaches,120tick deadline;
 long paths retain native node progress.631tests pass. Native copy reached10030
 before Iris240e, all192alive. Courier really collected2apples and handedboth to
 Birch before old deadline, crafted3breadrecipes, fed Willow/Oak. This supported
 an8k actual observation, but actual310 diverged and hit Birch at1235. Do not
 present copied10k as actual survival. Main v305 remains at322748.
-306: v305 plus separate worker’s3file reserve-search.patch (AgentMind,
 VillagerController,AgentMindTest) in SNAPSHOTONLY.633tests pass. STILLFAIL10030Iris.
 Earlier RecoveryRest branch intercepted action choice while food>0/requestpending.
-308: v306 +food>6 recovery-rest wait to preserve travel reserve.633tests pass,
 but nativeFAIL1243Birch due different care/route outcome. Rejected, not deployed.
 Root has ONLY the recovery-rest threshold change, NOT the broader3file patch.
-311: FROMv305 +FoodInventory.survivalMealSlot and controller use; own reserved
 carrot/potato may be selected ONLY atfood0 andHP<=25%, after ordinary rations.
 Normal/sharing selectors still protect roots.633tests pass, SHA
 4527d1d25f4878fa890ec2487c1df15f19827c3d9a7a0a805d55185c43d40a21.
 Copy312 fromCURRENT322748 verified Birch ate his potato immediately (food1 at40t,
 itemgone, memory/native nutrition), survived1361ticks until food exhausted again.
 No donor reached him. All192alive. No world/body/health/timer grants or guard edits.
-313: FROM311 +NativeBodyRoutes partial-height floor correction.634tests pass,
 SHA7530cc6c1d8df3cbdbc314c9a3eac7ef640d5ccd2cba75da5331bcf9217b611c.
 Old edge planner usedY64 inside a light_gray_carpet whose actual top is64.0625.
 Every departure swept through the carpet; native starts could not move. Include
 partial own-block collision height (<1block), while vertical door/fence shapes
 remain native. Retain actual off-center body at first edge and all collisions.
 Copy314 fromCURRENT322748: courier moved, delivered5times (Birch actual apple
 at323137, before potato rescue window ended); Juniper40b3 crafted3breadrecipes.
 FAIL5148 on Linden8ea7 (1HP/0food/noitem). All192alive. This proves4k window,
 NOT8k or indefinite care. Its changes are now deployed through exactv324.
 Frozen315 confirms actual native start cell(-663,64,-614) islight_gray_carpet,
 raw floor64/bodyFitsfalse; cellabove65 floor64.0625/bodyFitstrue.0game ticks,
 source unchanged, all192alive. This is a real geometry diagnosis, not assumption.

## Tested supply/growth changes and rejected prototypes

v316/318 physical-offer prototypes passed634/635unit tests but native317/319 still
missed Linden at5160/5166ticks. They were NEVER deployed and are now removed from
root, preserving the other worker’s door-start fixes and unrelated tests. Initial
language diagnosis was incomplete: closest cb79 had not understood Linden, but
40b3 and founder460a HAD understood his hurt/food request and were gathering food.
Do not carry the rejected physical-offer protocol forward as a verified fix.

**v322 FROMv313**, no offers: critical supply movement recognized distant calls
only. Once actual words were understood, the public alarm left calls.heard, so
supply urgency vanished. Use existing criticalVoice (understood requests) as well
as distant injury calls in hurryForCriticalSupplies AND chargeRunning. Apply that
existing native hurry action to retained/new actual workbench and hay-return
routes in IllagerFood, not just storage. Bodyfood>=14, actual running exhaustion,
dry-path and stair walking checks remain.634tests pass, SHA
3447b745e1b597a5900dc508c75a8f7789c817fb3f9f8360602c46a5325e97e7.
Copy323 FROM322748 PASSED12000ticks, all192alive. Linden recovered20HP/17food,
Birch1HP/14food; Iris18HP/17food. This was the useful supply fix.

**v324 FROM322** +only existing VillageBedding.work before AdventurePreparation.
An eligible/funded parent can now attempt existing paid housing work before the
continuous adventure job. It yields normally when no task/material/space exists.
634tests pass. Copy325 FROM322748 PASSED12000ticks,all192alive; no births or bed
production proved. Actual327 used EXACTv324 and PASSED4001ticks, all192alive.
Pinned artifact/sourceZIP in dist/native-care-v324. Earlierv320 had same priority
idea based on rejected offer prototype; never natively tested/deployed. Ignore it.

Copy326 continued ONLY passed copy325 from334748 withv324; NO main rollback or
promotion. It stopped1969ticks later on homeless Willow1104,1HP/0food, beetroot
seeds2 (inedible), no home. Source334748 already4HP/1food; CURRENT actual Willow
is still19HP/9food, so this is a future-risk diagnosis, not current main state.
All192retained, source unchanged. Hero acquired5carrots via ordinary farming but
bedding counters stayed0, family stillsavingfood, no newore/birth.

Next targeted food search should address HOMELESS low-reserve actors. Rejected
v306/308 broader action-mask policy changed every household’s behavior and one
candidate still failed Birch. Start with worker reserve-search.patch isolated into
v324, then limit emergencyFoodSearch to !observation.homeAvailable; controller’s
lowfood dry/fall1 exploration to mind.home==null; recovery-rest waits may block
lowfood ONLY when a home exists (orfood>6). This preserves camp rendezvous behavior
while giving homeless Iris/Willow time to find food before injury/night. Danger
stillFLEE; no operator coordinates or private stock scans. Test from CURRENT main
326749 (healthy distant residents), not the failed nearly-starving copy.

Root includes own-root survival, partial-floor, hay-progress, supply hurry and
bedding-priority changes plus the UNSTABLE generic recovery-rest threshold from
v308 and other thread’s language/closed-door work. Root lacks the broad3file worker
night-search patch. New root VillageFamilyProbe adds lastGiven/lastReceived to the
existing foodHelp report (read-only, not yet in deployed snapshot). Do not deploy
moving root. Use immutable snapshots and preserve unrelated edits.

## Mining feasibility toward equipment/endgame

Frozen operator surveys307/309 inspect OLDER rejected mine copy322798 (v293),
never restored to main. HeroY56, descending footY55, prospective floorY54 WATER.
There are FOUR water blocks Y54..51 above solid natural rockY50 in all4directions.
Hypothetically clearing3stepcells leaves a supported edge stance but no hand ray
for placing ordinary DIRT atY54 (neighbor anchors there are water). Do not weaken
footing checks or claim dirt can be placed through rock/liquid.
Native frozen rays DO reach an anchor for placing a falling block in the cleared
foot cellY55 (reach2.1–2.6, supported stand), so a real paid column of4SAND/GRAVEL
could fall by native physics intoY51,52,53,54 and support the next step. Hero must
first EARN4actual falling blocks; currently owns none. NO production construction
code written yet. Next feature: physically gather exposed natural sand/gravel,
excavate only while remaining on retained old floor, place each paid block atreal
contact, wait for native falling/settling, recheck solid/dry support before descent.
Keep all unrelated fluid/falling/structural/exit guards. Native copied proof needed.
Only the floor under a pending owned construction job could permit a foot-cell
excavation; never broadly ignore water. Can reuse ordinary mining drops/tool wear.

Growth: v324 gives eligible/funded bedding work priority, but no material transfer,
craft, newbed or birth observed yet. Hero ends with2bread, below family cost3bread;
next day must gather another meal before housing. A longer copied run into morning
was cut short by homeless Willow; fix early self-food search without disturbing
well-supplied households. Do not claim the priority edit alone proves growth.

## Launch / next actions

Main STOPPED326749, no owned servers live. First copy CURRENT world with pinned
v324 and fresh326749 scope, because Linden now13HP/1food and all3providers have
little/no carried food. Do not reuse old proof of a different checkpoint as a
blanket survival guarantee. scopes throughv312 are STALE. Preserve current injuries.

Then targeted HOMELESS early-food search and sustainable production, while Clover
continues earning gear/construction materials and families acquire real beds.
All-resident twelve-thousand-tick proofs now pass at the previous checkpoint;
actual327 rescued Birch and advanced4k, not full12k. Victory remains unproved.

Endgame state verified from current26.3 saved data: overworld border8192 (initial
native-four-regions.json1024 is historical); Nether/End files exist. End fight has
 dragon_killed0,previously_killed0,needs_state_scanning1. No victory. World borders
are in dimensions/minecraft/DIM/data/minecraft/world_border.dat, not level.dat.
No further border expansion/dimension creation is currently indicated. Native
EnderDragon damage still requiresPlayer or ALWAYS_HURTS_ENDER_DRAGONS source; real
villager-owned TNT may qualify after earned resources, never fake health/kill.

`tools/food-access-audit.py --source-world STOPPED --runtime runs/fresh
--production-jar PINNED --illagers --rehearse --all-residents --camp-meals
--camp-passages --quarry --care-startup-scope SCOPE --illager-ticks N
--illager-min-health1 --last-health-recovery --tps80 --port25604`.
Read native progress key **villagerFoodStores** (56entries), including each server
health/food/navigation/gatewait; criticalVillagers[] does not describe all patients.
Probe `ResidentMealProbe` inspect fields are underINITIAL in resident-meal.json;
finalactor usesinspectfalse. Frozen315 geometry is under campCommunication.listeners.

`tools/observe-native-play.py --source-snapshot /tmp/villagers-source-snapshot-vNNN
--runtime runs/fresh --ticks N --min-health1 --last-health-recovery --allow-quarry
--care-resume-scope SCOPE --tps80` opens actual; preserve source hashes/class proof.
Both scripts require a fresh runtime. Never copy/advance main concurrently. Correct
scope worker must have native_illager_worker tag (founder460a is NOT eligible for
alreadyManaged-worker preflight). NativeCareStartup freezes before entity ticks;
ready receipt preserves exact body/stock, then resumeLAST after observer/TPS setup.

Source build: source tools/java-env.sh; villagers_select_java; original ./gradlew
-p /tmp/villagers-source-snapshot-vNNN --project-cache-dir
/tmp/villagers-snapshot-gradle-vNNN --max-workers=2 build test.
Verify manifest/JAR SHA/testXML then artifact.json before launcher. Preserve owned
sourceZIP/JAR/manifests in dist before actual deployment. No subagents or messages.

Latest verification: actual327 and copies323/325 normalterminal0; copy326 prelethal
stop1969,all192alive. No owned process remains live. Evidence README and compact
JSONs updated throughv327; actual receiveditem identity is not directly sampled,
only giftcounter1/food5/rootconsumption. Native report root-only lastReceived field
will make subsequent item receipts explicit. Keep full goal ACTIVE.
