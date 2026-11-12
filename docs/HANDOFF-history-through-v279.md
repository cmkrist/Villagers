# Active goal — ACTUAL v269, gameTime308509

Get a villager in the villager test map to beat Minecraft while villagers and
illagers develop through native conserved actions. Goal ACTIVE, unbounded.
No subagents authorized. Growth, diamond equipment, Nether/End/dragon UNPROVED.
Historical evidence: HANDOFF-history-through-v268.md, through-v249 and earlier.

## Authoritative state

Main `runs/native-four-regions-play/saves/NativeFourRegions` is STOPPED after
actual v269. GameTime308509, all192 original humanoids alive, no new/missing/dead
UUIDs. This is now authoritative, superseding v213/gameTime304508. Never restore
v213 or promote a copied save. Original FourBiomesWar remains unchanged.
Actual result: `runs/native-actual-frozen-care-v269/result.json`; budget-native.json,
bodies-after.json and native-care-startup.json. Session67276 ended0, no wall stop,
no combat stop, 4001 elapsed ticks at observed20TPS (nominal4000 budget).

Saved critical residents:
- Pip `4c3c0db4-75ea-42f5-8f91-b1c4814944d7`:1HP/7food, saturation5,timer0,
  one retained planting potato; fence cage near(-597,64,-614).
- Dara `573a309c-f0fc-4dd1-94cd-8246d352467c`:1HP/7food,saturation5,timer0,
  one planting potato; gated room near(-632,65,-607).
- Oak `64324351-28f6-4bc1-a8f8-949f3300763b`:2HP/6food,saturation4,timer0,
  one planting potato; same room(-631,65,-608).
- Birch `764e71da-6e8c-4016-b4cb-b9aee031c564`:17HP/0food,timer40,emptybag,
  (-596.5000001522084,64,-633.1687351243991). Unfed other fence cage.
- Rowan `88893f4d-fbba-4245-b563-089e8c0a5b52`:8HP/0food,timer10,emptybag,
  (-596.4999975087727,64,-633.9101508420496), same underserved cage.
Clover adventurer `edc5ea2a-f488-4aea-961c-456c3f7200f1`:20HP/20food, earned iron
pickaxe damage48, stonepickdamage70,3bread,3carrots,29dirt,4sticks,1rawiron; noarmor
or diamonds. At bed(-466.5,64.5625,-376.5). No equipment milestone this window.

Camp center(-640,73,-606),radius90,ID-175646985019319. FounderPip-460a alive.
FounderJuniper-e9b7 drowned historically and staysdead. Additional workers EVER
are now TWO, cap exhausted: Juniper40b3 and couriercb79. No replacement worker!
Courier UUID `cb79c23c-9623-45ac-9776-90c0f0a7813e`, activated in REAL main while
frozen at304508,24HP/emptybag/crossbow1, before==after exact physical signature.
End at(-598.815665084673,65,-623.4796333650917),24HP,emptybag,crossbow1, mindfood18.
Ledger main root `native-illager-worker-history.jsonl` has exactly two rows.
Founder death history preserved. No supplied items or created adults.

Invariants: no rollback/resurrection/copied-save promotion/replacementadults,
NPCposition/velocity/health/NoAI/invulnerability setters, suppliedstock,
syntheticbirths, clockskip or cropage/moisture edits. Native sprint+exhaustion,
real recipes/drops/transfers. NativeCellEscape remainsfalse; v128 killedcaptive.
Camp entrancework bounded6lifetimeactualblocks. Experimentalcampfarming false.
First actual combat hit stops ordinary observation; preserve all nativeinjuries.
Do not stop other independent task’s servers or overwrite its source changes.
Do not secretly unload vulnerable bodies to claim whole-world progress.

## Stable tested artifacts

Current movingroot has unrelated active language/belief/volunteer changes. Build
and run immutable snapshots. NativeMealVolunteers prototype is OFF unless explicit
JVM villagers.camp.volunteers; never credit its unexecuted behavior.

v265 `/tmp/villagers-source-snapshot-v265`: stable606tests0failures, jarSHA
f88f8f26e4cfbc060efbbe3e84d7cb48e4464e5082e7dc19dbedb8ec670d20e6.
Fresh source214 copy passed4000ticks all192alive. Older controlled v258→v260
passes8000ticks in branch but not proof of current integrated indefinite care.
Current v266 reopened v265 and FAILEDafter1312ticks onBirch1HP/0food, beforelethal
starvation interval; all192stillalive, source265unchanged. Courier openedlower
fencehatch at(-596,64,-633), fed neighborRowan, neverfedBirch. Preserve failedcopy.

v267 `/tmp/villagers-source-snapshot-v267`: stable620tests0failures, jar
`build/libs/villagers-0.4.1-dev.jar`, SHA
da1a6ffb5e8c08b061933f5d7831baea444859df939ad95f44b5b1a58d9f6a89.
source-manifest.json verifies copiedsource before/after build; artifact.json records.
Durable testedJAR,sourceZIP,manifests also in `dist/native-care-v267/`.
All409 separately compiled client classes exactly equal testedJAR byte-for-byte.
Build log /tmp/villagers-snapshot-build-v267.log. Snapshot includes frozenstartup
helper; extra tests include identity/time/body validation. No diagnostictracecode.

## Frozen startup implementation and proof

NativeCareStartup opt-in JVM villagers.lab.careStartupScope=<schema1JSON> requires
DevMode,trialnative-regions,boundedplay,minHealth1,lastHealthRecoverytrue. Holds
serverfrozen BEFORE NPCticks, verifies reviewed gameTime,residentHP/position,
workerUUID/exactbodysignature. Recruits only expectedeligibleworker; check before
ledgerappend. Body/stock before==after required. Writesready receipt; remainsfrozen
until explicit `villagers native-care-resume`. Failedscope remainsfrozen.
LabClient observationguard waitsforstartup ratherthan terminatingpreflight.

`tools/observe-native-play.py --startup-worker-proof FROZENRECEIPT` requires
--recruit-native-worker REFERENCE + --min-health1 --last-health-recovery.
Builds exactscope from reviewed copiedworkerreceipt and actualbeforeaudit; verifies
readyreceipt, sends onebatch: play,spectator,camera,tpstickrate,resumeLAST.
The OLD proof214/gameTime304508 is NO LONGER REUSABLE on actualv269! It mustfail
clock/body validation. For nextactualresume, existingworkers are alreadyactivated;
do not appendanotherworker. A newcurrent frozen scope can use managedworker
idempotentbranch, but observer CLI currentlyonlybuilds scope from enrollmentproof.
Ordinary observerwithoutrecruit usesexistingworkers andboundedhealthguard.

Observer --source-snapshot PATH verifies manifest,pinnedJAR,testpass; generates
Gradle init overriding mainsource/resources/builddir. runClient.doFirst checks
compiledclassbytes againstpinnedJAR. It records correctsourceRoot/codeMode/classes.
Primary client .build-pinned contains409v267classes. Originalmovingroot untouched.
Build.gradle forwards VILLAGERS_CARE_STARTUP_SCOPE and validateslocalboundedrun.

food-access-audit.py --care-startup-scope JSON enables samehelper in COPY with
allresidentillagerrehearsal,min1,lasthealthrecovery. Probe waitsready,explicitlyresumes.
Failedhelper normalstopsserver frozen. Newprobe restates requestedTPS atresume;
v267 ran BEFORE thisharnessfix so observed20TPS despite requested80 (report20).

Native proofs:
- `runs/native-frozen-care-startup-v267`: fresh actualv213copy, froze304508,exact
  cb79adoption, passed4000ticks to308508,all192alive/sourceunchanged/exit0.
- `runs/native-frozen-care-reject-v268`: wrongworkerHPsignature, rejected304508,
  adoptionempty,all192 physicalbodies/tags/stock/food unchanged,exit0. Wrapper rejects
  expectedmissing successfulobservation; this is negativepreflightproof only.
- `runs/native-actual-frozen-care-v269`: realmainadvanced to308509; all192alive;
  10courierdeliveries,6nativebreadrecipesfromactualgatheredhay/wheat. ActualPip/Dara
  rootpairgifts thenrefills/bread. Gatesrestoredclosed,actualcontactreach<=3.
  Foodpositive, NOT healed or populationgrowth. Birch/Rowanremainunfedrisk.
Actualsessionordinaryclient simulationdistance12; global simultaneoussimulation
NOT claimed. Before/after all192savedbodies audited. Copiedprobe --all-residents
adds persistentforcedchunkloads; do not conflate those with actualclientcoverage.
Mainquarrytrue (previouslyproven) retained; campfarming/CellEscape false.
Evidence `docs/evidence/2026-11-11/native-frozen-care-startup/` has selected receipts,
actual-v269.json,artifactmanifest and limitations. Previouscare evidence remains
`docs/evidence/2026-11-11/native-gate-meals-and-bread/`.

## Care implementation context

NativeCampMeals sprint onlygrounded andnext2nodeslevel±.25 with actualnextsegment
clear of bodies; .8walk onstairs/nearbody, nativesprint/exhaustion elsewhere.
RouteProgress observes native nodeindex; originalPath.advance ALWAYSexecutes.
Contactstands beforewiderleverstands. Narrowurgentfallback maywalkbesidepushable
ravager oncompleteblock/border/drop-safe route; normalcollisionsuntouched.
Rootpair gifts toEMPTYfarmers require2roots(seed+edible); lastplantingrootprotected.
Own-action pairtags permit investigating single-rootrefill; actualcontactstock
validation stillrequires2foremptyfarmer. No sampledsinglepotatorefillclaim.
Criticalcalls volume14 atHP<=25% andfood<=2. Heardcriticalrequest allows upto3
realbreadrecipes perreachedbench,3wheat/recipe,realinventory+exertion.
Earlycallerwait scopedto ownillager_barracks andbodyinside so ordinaryvillage
residents stillseekfood. Broadoldscope starvedSage infailedv259, fixedv260.
CurrentNativeCampWork scansvisiblecandidatecontainers thenactual3blockcontact
atpickup. Stalecompiled16blockcontactscoutingcausedv261failure; stablesnapshotsfix.

Read-only v266cagegeometry `/tmp/villagers-birch-cage-floor-v266.txt` (script same
prefixread-cage): openedlowergap(-596,64,-633); upperfencesretain. Insidefloor
(-596,63,-634) and(-597,63,-634)PLANKS; edgefloorsSLABS. Birchstalls x≈-596.5 while
neededcontactstandmight(-595.5,64,-633.5). This is geometryobservation, NOT confirmed
cause. feedingGapApproach currentlyonlyPLANKS, scansradius3/path<=6nodes. Need
inspectactualnativepath/contact plans before changingphysics. v266partial receipt
mealRendezvous empty, v269actualBirch gatewaitstand often“finishing rendezvous.”

## Next actions toward FULL goal

1. Copy CURRENT STOPPEDactualv269, samepinnedv267artifact, allresidents/min1+
prelethalguard, observe underservedBirch/Rowan/contactroutes and repeatfood. Their
8HP/0food and17HP/0food mean a blind longactualresume risksstarvation. Keep failures
preserved; do not restartoldsource214 toreplaceactual. No ownedruntime remainslive.
2. Implement/prove sustainablelocalfood (real gatheredseeds/crops/recipes) and
careforallcaptives. Copied --camp-farms isexperimental; don'tenablemainuntested.
Actually growvillager/illagerfamilies withrealpaidbreeding and verifynewchildUUIDs.
3. Actualearnedarmor/diamonds, Nether/End progression, villagerenderdragonkill.
No endgamecode yet. Goal staysACTIVE. Don't markcomplete for care/irononly.

Copy command (select Java with tools/java-env.sh first):
```sh
python3 tools/food-access-audit.py --source-world runs/native-four-regions-play/saves/NativeFourRegions --runtime runs/FRESH --server-home /home/cmkrist/Games/VanillaPerfectedCompanion --production-jar dist/native-care-v267/villagers-0.4.1-dev.jar --illagers --rehearse --camp-meals --camp-passages --all-residents --illager-ticks 4000 --illager-min-health 1 --last-health-recovery --tps 80 --port 25604
```
3PythonpopulationtestsPASS. No subagents permitted.
