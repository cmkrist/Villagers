# Active goal — actual v342 at game time336121

Goal ACTIVE/unbounded: get an actual villager to defeat the Ender Dragon while
villagers/illagers grow. No new births, actual new beds, armor/diamonds, Nether/End
or victory verified. No subagents authorized/spawned. Preserve other thread edits.

## Current main and tested artifact

Authoritative `runs/native-four-regions-play/saves/NativeFourRegions` STOPPED after
actual342 at336121. Exact deployed source **v340**,639Java tests pass; JARSHA
 ebef41630b73df3826b9dbdafcf1ea03214479517b3fdaeb0360300fc5252b9d.
Snapshot `/tmp/villagers-source-snapshot-v340`; durable `dist/native-care-v340/`.
Actual342 PASSED8001ticks from328120 with all192current originals alive, no new/
missing/deadIDs, normal savedexit0, native80TPS/source/classbytes verified,
oldFourBiomesWar unchanged, no combat/wallstop. Previous actual338 advanced1371
before Dara-f13a healthstop. **Total actual this turn9372ticks** (326749→336121).
Main dawn now (clock336121mod24000=121). Do not roll back/promote copies.

Actual342 receipt proves Dara573a gifted OWN lastpotato1→0 to Dara-f13a at328121,
receiverbag0→1. F13a actually consumed that root (memory), then received REAL
Juniper40b3's3bread and later couriercb79's2bread. F13a recovered1→20HP,15food.
This emergency gift spent a real donor root, not granted food/health. Normal
sharing/normal meals still protect lastplantingroots; emergency ownrootconsumption
and this new UNDERSTOOD localcriticalrequest are the narrow exceptions.

Latest current bodies (exact bodies-after actual342):
- Hero Clover edc5:20HP/19food,sat0,exhaust1.8461, at(-466.5,64,-375.5), awake.
 Bag stonepick damage70,ironpick damage100,dirt44,bread2,carrot1,sticks4,rawiron1,
 wheatseed1; heldwoodhoe28/offhandwoodpick4. NO actualnewdirt/ore this turn yet.
 Active quarry(-475,63,-347)SOUTHstep9;114retainedminefloors/3entries/1exhausted.
- Birch67f3:1HP/17food,sat11,timer0, emptybag,
 (-588.3945867590968,64,-587.0266317210921). Fed, stillneedshealingfood>=18.
- Dara-f13a:20HP/15food,emptybag,(-633.5,64,-601.5), returnedhome naturally.
- Dara573a:10HP/7food,emptybag,(-632.2540497184691,65,-607.3066259175631).
 Her seedpotato spent to save F13a; futuremealneed, not immortal/restored.
- Linden8ea7:20HP/20food,sat3,(-680.5000231235362,64,-623.1243302569933),emptybag.
- Rowan8889:19.7667HP/20food,emptybag,(-596.5000207262668,64,-633.0815177455172).
- Distant homelessWillow1104 still19HP/9food; Iris240e18HP/8food;
 housedLinden6cdf20HP/9food. These do NOT normally tick at mainobserver, but
 allresidentcopies expose futurefoodrisks. Never conflate copied damage withmain.

Workers:
- Juniper40b3 at(-667.5019713377237,63,-627.3414093918119),24HP/16food,
 fatigue.3491, bag[wool1,string1], crossbow, otherhandempty.
- Couriercb79 at(-609.5013407248831,64.17675927506424,-626.2254238754903),24HP/
 20food,fatigue.6068,bag[string3],crossbow.
- FounderPip460a:24HP/8food,fatigue.5779, emptybag,crossbow; inspect saved position
 ifneeded. Founder lacks native_illager_worker; cannotuse frozen managed scope.
 ExactlyTWO lifetimeadditionalworkers already used; no more adoption/replacements.
 Camp-175646985019319 center(-640,73,-606)radius90. CellEscape/Farming false;
 quarry/meals/passages true. Defaultobserver(-555,90,-495),sim12. Do notsetbody,
 food/health/clock/cropgrowth/NoAI/invulnerable or supplyitems/adults. Historical
 drownedJuniper-e9b7 staysdead.

## Live work / next immediate checks

**Copy343 running**: `runs/native-morning-mining-v343`, session70240,port25604,
 fromCURRENT336121, exactv340,12000ticks/allresidents/meals/passages/quarry,
 farmfalse. NEW read-only `foodWork` adventurerdiagnostics (activity/ownfoodgoal/
 familyfoodgoal/wheattarget/provisionstarted/nextprovision/farmjourney/nextFarmPath/
 croptarget) in toolsNativeIllagerHabitatProbe, no actor knowledge or mutations.
 Inspect progress/terminal before anyrestart or actualresume. Never mutate main
 while a copied run checks CURRENT source hashes.

Fresh scope `/tmp/villagers-care-resume-scope-v343.json` game336121 residentBirch67f3
 current1HP/currentposition, worker40b3 currentwhitewool1/string1 exact5slotnative
 signature. It is fresh ONLY until nextactualadvance. ALL earlier scopesstale.
 Noownedclientlive; only343server expected, inspectprocesses for otherthread.

## Source/test chain and evidence this turn

Evidence `docs/evidence/2026-11-11/native-homeless-search-and-next-day/`.
Previous handoff withfullv291..327/miningwaterfeasibility kept in
`docs/HANDOFF-history-through-v327.md`; earlier archives also retained.

- Copy328 current326749 exactv324 FAIL11520 on homelessWillow1104;all192retained.
- v329 FROM324, homeless-only earlier foodsearch atfood<=6, lowfoodhomeless may
 bypass between-replyRecoveryRest, useexistingdry/fall1exploration evennight.
 Housedcare behaviorretained. Added lastGiven/lastReceived reporting.637tests.
 Copy330 PASSED12000current326749→338749/all192. No growth/mining proof.
- v331 FROM329, VillageBedding.collect invokesexistingNativeWorkInventory.makeRoom
 if actualcontactstore haswantedbedding butbagfull; avoids falselyempty mark.
 637tests. Copy332 manually bounded4240assuperseded, PASS/all192; NOT12kproof.
- v333 FROM331 +5reviewedworkerfiles: shortcollision-checkedquarrydeparture,
 FarmExpansion no newtilling on nearby miner's realretainedsupport; dusk schedule
 yields atfood<=6, keepsreal danger.638tests. Distnative-care-v333 retained.
 Copy334 FROM326749 FAIL11187 on housedLinden6cdf1HP/0food. Saved all192,
 sourceunchanged. Hero DID clear5extraquarrydirt to49 andcompletedstep16 inCOPY,
 no actual claim. Frozen335 fromthisrejectedcopy showsreal chest1blockaway,
 native1..4noderoutes; no unseencontents supplied/actorcoordinates taught.
- v336 FROM333 +otherthread's NativeBodyRoutes closed-door actualfirstedge check
 in scoped searches evenwithoutbodyavoidance.638tests. Copy337 PASSED12000 from
 326749/all192, but Birch stayed1HP/7food, distantWillow2HP/3food; hero0newdirt.
 NOTDEPLOYED. Mere copiedpass notproof of sustainablecare/miningadvancement.
- Actual338 exact333:326749→328120(1371ticks), stoppedF13a1HP/0food/timer60/
 emptybag.192alive. Allcouriers beyondrealhandreach; baselinecopy339 FROMCURRENT
 stoppedafter1tick. No body/timer restoration/guardrelaxation permitted/included.
- v340 FROM333, 639tests, emergencyRootGiftSlot safeown donorfood>=14/HP>=50%,
 oneactualcarrot/potato,noCUSTOM_NAME; receiveralive/visibleHP<=25%, realmutualLOS/
 <=3block handreach andbagcapacity. Speech makesexception fornonGREEDYdonor only
 AFTERactualWANTFOODphrase understood; unhappy/distrusting people may savea life.
 Normalsharing selectorstaysprotected. Earlyownfood0/critical/noownedmeal request
 beforeeatOwnedRation/metabolize, soactualgiftcanbeconsumed beforestarvation.
 Acriticalrequest may loadnearbydonor'sexistingmind synchronously toavoid first
 replylost toasyncinitialization; no information orresourcesinvented.
 Copy341 FROM328120 actualguardunmodified: realDara573apotato1→0 at328121; F13a
 food0→1 atfirst40t, Juniperactual3bread at328393 thenfood6→11→16. Itreached10204
 before Rowan88891HP/0food, all192retained/sourceunchanged. NOT a full12kpass.
- Actual342 exact340 PASSED8001, asabove. Actor/read-onlyreceipts confirmthe
 rootgiftandlaterbread inACTUAL, not justcopy. No growth/ore/newactualbed yet.

Root now includes narrowhomeless search/beddingbagspace/emergencyrootsharing
 selectivehunks. Existingotherthreadcriticalall-home emergencysearch remains as
 OR withnewearlyhomelesscase (deployed340 itself has ONLYnarrowhomelessmask).
 Rootwholeartifact isNOTdeployed/testedruntime. FullROOT722testsPASS0failures,
 `/tmp/villagers-shared-emergency-root-tests.log`. Preserve roototherlanguage/
 closeddoor/farmwork. No furtherrootbaselinewholesalereplacement.

CampWork.beds2 onfounderPip460a isHISTORICALPERSISTENT count presentatfirst40t.
 It is NOTtwo newbeds thisturn. Commentarycorrected aninitialmisinterpretation.
 Useroptionalquestionpending: allowordinaryMinecraftdeaths vs preserveevery
 individual? Savingeverybodyhasstalledendgameforhours; this wasourconvention,
 notinitialuserrequirement. No replyyet, no guardchanges made. DoNOTaskagain.
 Continueusefulactualgrowth/equipment work; victory remainsfullgoal.

## Next work

Reviewcopy343morningprogress/stop; a full daylightwindow shouldfinallyletClover
 finishquarry andearnore while healthy. Somecopies hero approachesquarry forever
 withlastApproach{} andstandempty: may benavigationadmissionconsumed by anearlier
 food/fieldjob; newfoodWorkreadout shouldexposewhich. Source333 copy334 did5dirt,
 so donotassume allquarrypathsbroken or falselyclaim native5dirtasactual.

Bedding stillnorealproduction; hero2bread+1carrot unfunded (needs12breedingunits,
 reserve1plantingroot). He got5carrots/funded in334copy but nofiber/shears nearby.
 Prioritizeactual ownfood gathering/bedding beforefull-timeadventure whenfunded.
 Illagers target6bread already; care eatsmostsurplus beforefamilycanfund6each.
 Sustainablecampfarms needseparatecopyproof of realearnedhoe,seeds,planting and
 nativegrowth beforeactualenable. Existingfarmsetup runs BEFOREIllagerFood:
 shouldyield ifbread<3 ANDactualheardcriticalvoice/louderinjury, notreadprivate
 recipients. `NativeCampMeals.criticalVoice` packagevisible canreuse. No farmflag
 enabled inmain/current343. Earlier farmcopies only2seeds, nohoe/plantproof.

Miningwaterengineeringfeasibility/endgame audit/launchsyntax retained below in
archive327. Hero ownsNOsand/gravel. FrozenoldermineY56 fourwaterblocksunder
 pendingstepfootY55 haveNOdirtanchoratY54, but reachablefallingblockanchorY55.
 Realpaid4sand/gravelcolumn couldsettleintoY51..54 throughnativephysics. No
 productionconstructionjobyet; never broadlyignorefluidguards. Needearnmaterial.
 Enddragon_killed0;Nether/Endexist;overworldborder8192already. Dragonrejectsnormal
 nonPlayerdamage; earnedvillagerownedTNTpotential, nofakehealth/kill. Goalnotdone.

Tools: source tools/java-env.sh;villagers_select_java. Useoriginal./gradlew -p
 immutableSNAPSHOT --project-cache-dir dedicatedDIR --max-workers=2 build test.
 Actual tools/observe-native-play.py source-snapshot/ticks/min-health1/
 last-health-recovery/allow-quarry/care-resume-scope/tps80. Readbudget-native and
 bodiesafter, notHUD; renderedclientcanreportstaleHP. Copy food-access-audit.py
 pinnedJAR/allresidents/etc asarchive. Preserve everyfailedcopy, no main rollback.
