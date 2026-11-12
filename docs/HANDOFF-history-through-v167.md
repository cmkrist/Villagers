# Goal active — authoritative primary after v167

Primary STOPPED at **gameTime293379**:
`runs/native-four-regions-play/saves/NativeFourRegions`.
**192 saved humanoids, all192 alive; all186 previously saved residents preserved.**
56 managed villagers; two atzerofood: Ember-c73f15HP and Juniper-e3de14HP.
Dara-f13a remains13HP/5food. Native80TPS/sim16, all source/classes/oldsavehashes
preserved; no copied world was promoted.

ACTUAL v166–v167 village rescue: Ember-8a41 reached own village chest and
collected/ate4REAL bread,0→20food/14saturation,18HP at(-438.50,64,-824.48).
Healing is not complete. Saved mind has4physical collection memories; no supplied
items, crop ages, body setters or inventory edits. v166600ticks only reached
(-431.93,63,-798.50),18HP/0food; v167600ticks moved spectator camera near village
(-440,90,-800) and finished the actual route. Preserve this distinction.

v167 saved6new unmanaged pillagers near(-300,71,-1068), including an ominous-banner
captain72HP. They have no family/managed tags. Newly recorded native bodies,
NOT verified births or replacements. Do not call them learned/developed families.
All original186 preserved, total192 currentliving. Check v167 bodies/result for IDs.

Historical loss remains: actual founder Juniper-e9b7
`e9b7b192-009b-4158-bc2d-1a63456a6f91` drowned in v108 at
(-603.47,47.75,-613.47). No rollback, resurrection, replacement body or copied-save
promotion. Historical camp registry retains her UUID. Earlier all187alive claims
apply only to older saves. Goal remains active.

Surviving founder Pip-460a `460ab4b6-1522-4504-9673-25fd503e0610`:
Last v148:24HP/20food/1saturation, at(-603.04,63,-635.48).
Current v167 Pip24HP at(-615.62,64,-615.48), real bag6wheat/2bread, offhand1bread.
A stocked rescue is now plausible; never grant him extra stock or move his body. Copied v149 first40tick
report had an empty bag; old v136 bag3wheat/1bread is no longer current.
Current injuries include Dara13HP, Juniper-e3de16HP, Birch-67f3 about17HP,
Iris18HP, Willow-110419HP. Inspect latest actual bodies for exact food values.
Clover-d10c healed9→20HP through real food/metabolism.

Adventurer Clover-edc5 `edc5ea2a-f488-4aea-961c-456c3f7200f1`:
20HP/20food at(-479.33,63,-378.30). Held wooden hoe damage18; offhand wooden
pick damage4. Bag stonepick70, earned ironpick24, dirt30,bread3,carrot4,
stick4,rawiron1. No armor. Daytime farm work is real; do not call held hoe an
iron pickaxe. Source has the original mine/furnace/footing history.

Old authored `runs/four-biomes-play/saves/FourBiomesWar` hashes still match.
Shared dirty tree; never reset/clean unrelated work or overwrite a live pinned
JAR. No NPC position/velocity/health/NoAI/invulnerability setters, free stock,
synthetic births, clock skipping or save promotion. Native80TPS runs every tick.

## Verified changes and actual outcome

NativeCampEntrance is NEW production code. A healthy adult managed native
pillager investigating a real heard camp voice may work ONE narrow entrance
in its own camp if normal listening routes fail. Maximum2horizontal columns,
3high,6cells/6lifetime removals. Existing air is allowed; editable materials
are a narrow masonry palette and natural dirt/grass fill. No wood harvesting.
Floor footings and the lintel above the work volume remain intact. Fluid,
falling blocks, protected cells, block entities and nearby beds/doors/fences/
iron bars reject the plan. Bodies pause digging. Construction is persistent
through entity tags; failed/partial work is retained.

The planner requires a complete real route to the outside work stand and a
hypothetical native route from the existing far-side stand into speaking range.
The hypothetical evaluator changes NO body or block. Actual destruction still
needs visible native hand reach3, ordinary tool-dependent work time, real drops
only with a suitable tool, native tool wear and inventory/overflow conservation.
No generic BREAK_BLOCK permission was enabled. Existing nativeCampPassages and
nativeCampMeals opt-ins gate this work; frozen learning/juveniles are excluded.
NativeCampDoors now initializes hand-openable door planning before camp jobs.

The blocker was an actual disconnected perimeter. v119..v124 diagnosed native
start cells, door flags and connected graphs: about5,150dry physical nodes and
14,998ordinary native nodes exhausted without reaching within23blocks of Dara.
Raising diagnostic fall2→3 and enabling wood-door planning alone did not solve
it. The initial probe incorrectly demanded every clearance cell be solid;
allowing already-existing air and natural fill exposed viable small openings.
Frozen v132 proved useful native routes beyond several candidate openings.
These frozen results alone were never called completed physical work.

Copied v133 then ran4k: Pip actually removed2stone blocks with200ticks effort
each and no drops while using a crossbow, entered the courtyard, heard/decoded
Dara's words and gave bread through the actual lever-controlled gate. Lever
and gates were restored.7real gifts;186→186living; no combat. Source unchanged.

Actual v134 ran2k,186→186living,minHP13,no combat/source changes. Two real camp
blocks removed, one actual bread gift to Clover who fully healed. Dara was
still unfed13HP/0food at the end; actual trajectories differed from the copy.
This actual state was preserved while testing its rescue.

Copied v135 from that EXACT actual state ran1k PASS: Dara got real bread after
54ticks, retained13HP and ended5food.3gifts;186→186living;source unchanged.
Only AFTER the copied source hash check ended did actual v136 resume.
Actual v136 then ran1k PASS:3real gifts,Dara13HP/5food,all186living,56managed
nonzerofood,minHP13,source/authored-save hashes unchanged,sim16 verified.
No blocks were changed in the primary by an operator; actual work used AI
navigation, physical contact, normal effort and native block destruction.

## Combat and population audits

Copied v128 tested the EXISTING cell-escape feature. It was unsafe: Ember-c73f
broke its iron cell door, took13damage from a Vindicator (20→7), then died on
its next attack. Keep nativeCellEscape FALSE/absent in primary config. This
copied death was NOT promoted. The primary still has that resident alive.

This revealed two audit defects, now fixed:
1. tools/population_audit.py compares living identities as well as UUIDs.
   A corpse can remain saved at Health0. v128 had186UUIDs but only185living.
   New fields include before/afterLivingHumanoids,newlyDeadOrMissingIds and
   nonlivingAfterIds. Both native observer scripts now reject new deaths.
2. LabClient bounded observations and ProbeResidents copied rehearsals stop
   after the FIRST actual combat hit, even above the minimum-health floor.
   Normal damage is retained; no cancellation/healing/invulnerability. This
   does not prevent an immediately lethal first hit. Copies record
   native-combat-stop.json and halt normally; wrapper marks the stop failed.

Copied v130 verified the new guard: an arrow hit captive Pip-4c3c20→16; the
server froze and saved before further combat. All186living retained. Do not
confuse this captive UUID with founder Pip-460a24HP. Unit test also covers the
old13damage→7HP counterexample. Actual v134/v136 used the new guard and had no
combat stop. Earlier native float/air guard restoration remains; no successful
window encountered water, so a live drowning rescue is still unverified.

## Build and evidence

**538Java tests PASS**,0failures/errors, plus3Python population tests PASS.
Log `/tmp/villagers-native-camp-entrance-final-tests.log`.
JAR `/tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar`;
SHA `50deed412ae5fe7d4509124a53a371a8fb82e8768538ae4c8a39d4579ea1bf67`.
Actual clients use recorded Gradle development classes, not that JAR.
Python compile and git diff --check passed before actual v134/v136.
Evidence: `docs/evidence/2026-11-11/native-earned-camp-entry/`.
Archives throughv106 andv116 retain previous details and mining commands.

Copied mining v127 used --until-iron-armor and ran12k normally,20HP/20food,
more real excavation and5paid foundations,122retained floors, but still no
armor/ingots and rawiron1. It is COPIED progress only. v125 without the armor
milestone stopped after1tick on the already-owned stone pickaxe; no new mining
proof. The first v127 attempt failed before creating a runtime because another
copy held the source copy lock; retry succeeded after that copy stage ended.

## Current process and next work

Primary v167 stopped. All processes through v167 terminal; v168 copied rescue
RUNNING and its source hash check is pending; see latest section. v137/v138 frozen surveys both PASS; actual source unchanged.

v137:4,954sky-exposed natural soil sites in the bounded camp area,2,398with
native hydration; nearest wet sites about14blocks away across the west perimeter.
No locally visible irrigated soil within8. No owned seeds,hoe or wood recipe.
These global operator coordinates were NOT given to any actor.

**v138 changes the next implementation:** native dry farming is valid. The actual
full-pack tag/API fixture confirms MOISTURE0farmland is retained under wheat,
and dry crop growth speed is positive (2versus4for the isolated hydrated fixture).
This fixture is in-memory ONLY, not completed planting or crop growth. Shipped
FarmlandBlock.randomTick keeps dry farmland when MAINTAINS_FARMLAND covers its
crop; wheat has that tag. CropBlock.randomTick can grow dry crops under normal
light/random ticks. Do not make nearby water an unnecessary planting gate or
set moisture by hand. Actual crop age progression still requires a real test.
Three natural soil sites are currently visible within8of Pip; seeds and a real
hoe are the immediate missing resources. Their coordinates are diagnostic only.
Use local perception in production, not operator targets. Native code/API
bytecode and survey details are retained in the evidence folder.

**Experimental native camp farming now exists, defaultfalse; NOT deployed in primary.**
`NativeCampFarms`, config `nativeCampFarming`, capability gates, and copied CLI
`--camp-farms` implement local short-grass seed drops, one natural log plus
native plank/stick/hoe recipes, real table contact, and consumed wheat seeds
with an owned hoe for age0/defaultMoisture0 crops. Preserve the last seed.
Up to32 own tagged plots; actual crop ages reported, no age/moisture setters.
Existing meals/families take priority; generic block breaking/placing stays denied.
NativeCampWork can collect actual seeds/hoe ingredients from real contacted stores.
IllagerFood now releases a remembered field when the crop has actually vanished.

v139 4k care COPY stopped at3986, Ember-c73f starving4HP; no farm material actions.
v140 added bounded public-call commitment (equal loudness cannot redirect detours,
1200tick trip,400retry; louder urgency can preempt; understood words still first).
One real meal, but stopped at3074 on distant **Ember-8a41**
`8a41fdfa-cbb9-4807-8a33-9373867ccba3` starving4HP at(-436,63,-777).
His actual starting stock was20HP/3food, no inventory. Future primary care must
address this independent village food failure before long windows.
v141 isolated1k COPY without camp meals PASS, all186alive, but no grass/logs/
recipes/plants. This is not farming proof. v139/v140 also retained186living.

Harvest bug fixed afterv140: short grass has no COLLIDER, so use native OUTLINE
interaction contact for actual seed gathering. Build544Java tests PASS after fix.
v142 corrected outline COPY full care/farming3000ticks PASS, all186living,
source hashes unchanged. Pip harvested12real short-grass blocks; every actual drop
was empty in that window, so seeds0/logs0/recipes0/plots0. One real meal delivered.
This proves harvesting contact only, NOT hoe crafting/planting/growth.
v143–v145 frozen focus surveys PASS, all186living, no source change. Ember-8a41
has nearby visible reachable hay, no table within48, nearby immature crops age1–6.
No ripe food or owned grain in the initial snapshot. Operator coordinates NOT taught.

NEW experimental SelfProvisioning fix: locally visible exposed hay may trigger
ordinary WoodBootstrap TABLE preparation before owning grain. Previously gathering
wanted a table but table preparation wanted already-owned wheat: circular dependency.
All real wood/time/recipe/placement checks remain. Build544 tests PASS.
v146 isolated Ember2000tick20TPS COPY (camp meals/farms disabled) PASS,
all186living, primary unchanged. TABLE goal started but natural wood unavailable;
logs/recipes0. By1852 real ripe potatoes grew and FoodAccess found a41step/0dig
passage. At2000 Ember20HP/1food, still walking, NO meal completed. Diagnostic
success status is a bounded cycle, NOT proof of successful food access.
v147 full care/farm4k COPY stopped at3072 Ember-8a414HP, all186living retained;
Pip grass15, actual native drops empty. Target grass(-639,64,-616) was occupied
by native pillager99c34bb9 at(-638.95,65.66,-615.02), causing persistent work wait.

v148 ACTUAL1k PASS, all186living, minmanaged13HP, nozerofood, native80TPS/sim16,
source/classes/oldsavehashes preserved. Pip delivered2meals and opened one real
food hatch. Clover now3bread, still earned ironpick24/rawiron1, no armor or
later milestones. Ember-8a41 now20HP/2food at(-429.68,63,-778.50), emptybag;
he remains the next village hunger risk. Primary STOPPED normally.

Afterv148 new experimental NativeCampFarms `occupied` filter skips living/nonitem
bodies at grass/log target selection and releases targets when bodies obstruct.
Actual gathering still rechecks occupancy. Build544Java+3Python tests PASS.
v149 full care/farm1k COPY of CURRENTv148 PASS, all186living, primary unchanged;
Pip harvested13real short-grass blocks, actual empty drops; seeds/logs/recipes/plots0.
At(-634,64,-619), empty bag,20food. Occupancy-aware selection did not wait forever
on the earlier body. v150 is a FURTHER1k COPY OFv149 (not a primary continuation),
full care/farms/allresidents, port25604/session67953, /tmp/villagers-v150.log.
v150 PASS, all186living, source v149 unchanged. Pip harvested3additional short
grass (16total across v149/v150), still no seed drops. Actual ordinary forage
gave6wheat/1bread midwindow, then2bread at end; camp farm logs/recipes/plots0.
End activity hay-route retry, fatigue.0084. No source copies promoted.
All processes TERMINAL, all source hash checks completed. Primary stays v148.
Native shortgrass shipped
loot is chance.125 and uses persisted random_sequence minecraft:blocks/short_grass;
repeated runs from the same snapshot repeat loot draws, not independent trials.
Evidence: `docs/evidence/2026-11-11/native-camp-farm-bootstrap/`.
No real seed drops, hoe recipes, planting or age progression proven yet.

## Current turn — clear native meal routes, through v153

Prior turn was PROGRESS (actual v148 and real harvesting evidence).
Primary still STOPPED v148/gameTime291176,186living. No later actual advancement.
v151 focus2k COPY PASS for cycle only; Ember reached vicinity slowly and ended
9HP/0food, no meal. v152 focus1k diagnostic COPY PASS,186living: target carrot
(-442,64,-752) was harvested/replanted to age0 by another body before Ember
arrived. FoodAccess.route active=false, lastFailure now records actual vanished
ripe target. No terrain/body mystery: local one-step native path cadence was too
slow for a finite public crop. The first12 plan steps were ordinary dry native
walking, including actual grass/stairs/path floor62.9375.

NEW FoodAccess fix (not actual-deployed yet): reuse the already-admitted preview
batch to start a full native path across the longest already-clear plan stretch.
Bounded native path query64, fall budget2, home/work authority,128nodes maximum,
dry path only; never cross pending excavation. Retain a running long leg rather
than overwrite it with one short step every40ticks. Global replan batch now
precedes local fallback. No position/velocity, supplies, resource or crop setters.
Observability adds actual route current/target/step/walkEnd and target-loss cause.
ResidentMealProbe additionally reports native navigation and operator-only initial
route geometry; it does not issue a path or teach coordinates during inspection.

v153 COPY1k20TPS PASS, all186living, source primary unchanged: Ember actually
reached target, harvested with normal replanting cost and ate bytick200,
2→5food at20HP. Still20HP/5food and1carrot after1000ticks. This is actual meal
proof, stronger than the cycle status. All544Java tests PASS.
v154 integrated4k COPY STOPPED at2999, all186living, source unchanged. Ember-8a41
20HP/7food (village meal fix validated), but western captive Ember-c73f starved
20→4HP. Pip carrying1bread offhand, near(-677,64,-635), serving another healthy
hungry resident. Grass10, no seeds/logs/recipes/planting. Existing prioritization
could ignore an uninterpreted injured voice while completing understood HUNGRY.

v155 first HURT/distress prototype COPY stopped at2992 on the same Ember-c73f,
all186living retained, source unchanged. Voice10 only after half health was too
late; a closer starving injured Juniper also needed real food. Village Ember-8a41
20HP/16food by2960, stronger evidence of ordinary repeated meals. Grass16 with
empty native drops; no seeds/hoe/crops. Never promote these diagnostic copies.

NEW current distress code, NOT actual-deployed yet: NativeCampCalls emits an
actual audible volume6normal,8critical hunger,10injury with food,12starving injury
at FIRST actual damage. No hidden health transmitted: emitter chooses its own
public sound. NativeCampMeals investigates an eligible louder injured voice even
before a lesser understood request. Real nearby Speech includes HURT for actual
injury; the donor must decode the exact four-word WANT FOOD HUNGRY HURT phrase
(or three-word WANT FOOD HURT). Mere sound never creates an understood request.
Known HURT requests prioritize actual remembered public volume, bounded4queue;
stronger calls can replace weaker recovery requests. First newly decoded injury
allows one retry; repeated words cannot erase the stalled-route cooldown. Serving
removes injury priority; the recipient decides whether further need remains.
New status includes pursuingUUID, injuredRequests and lastApproach.recipientId.

Build549Java tests PASS, including public-cue/no-language separation, exact HURT
vocabulary, bounded queue/priority, cooldown and stronger-starvation replacement.
v156 CURRENT4k fullcare/farms/allresident COPY RUNNING port25604/session9310?
(check /tmp/villagers-v156.log/process.json; root tool session new, not v155).
Keep actual primary stopped while its source hash check is pending.

Evidence: `docs/evidence/2026-11-11/native-clear-meal-routes/`.

## Latest authoritative state — v157 and village search

v156 early-distress fullcare/farm COPY stopped at2266 on village Ember-8a41 HP4,
all186living retained. Public starvation-injury calls were earlier, but a competing
villager harvested the only nearest ripe root before Ember reached it. No source
promotion. Distress behavior remains unproven as a rescue of Ember-c73f.

v157 ACTUAL1k PASS, native80TPS/sim16, all186alive, no managed zerofood, minHP13,
source/classes/oldsavehashes preserved. New clear meal routes/distress code ran,
but Ember-8a41 did NOT eat in this actual window:20HP/1food, emptybag, at
(-437.27,63,-777.23). Western Ember-c73f20HP/1food, emptybag at(-680.50,64,-622.49).
Clover20HP/20food at(-479.33,63,-378.30), bag3bread/4carrot/30dirt/4sticks/
rawiron1/stonepick70/ironpick24; heldwoodhoe18/offhandwoodpick4. No later milestone.
Actual primary STOPPED gameTime292177; all186living. Do not call copied v153
meal an actual v157 meal. The village shortage still limits long whole-world runs.

v158 frozen operator focus survey COPY PASS, source unchanged. No natural trunks
or table within48 of Ember; one bell at(-430,64,-820), ~43blocks away, NOT visible
and native direct route cannot finish. Ember's Brain has no MEETING_POINT. No
bell coordinate taught to actor. Another native root field lies about50–56blocks
away, outside FoodAccess's existing48radius and ordinary home28radius. Read
actual food survey file for ripe states; do not assume all physical crops are ripe.
Birch-3518 nearby field20HP/20food owns2carrots (one is a protected planting root).
Moss-fad4 adjacent to western Ember20HP/20food but emptybag. No private stock data
was put into any actor. Food sharing preserves donor/recipient planting roots;
do not erase original last-root tests or grant emergency meals as a shortcut.

v159 expanded FoodAccess RANGE64 COPY found no safe ripe-food route and stopped
on an unrelated first combat hit: native husk4b793243 hit Pip-3c29
`3c29e5bd-14b7-435c-8737-d49ac9cb7fdb`20→17HP at gameTime293128/elapsed951,
all186living. That resident is at(+378.81,74,+102.02), outside actual camp camera
simulation, but is forced loaded in allresident diagnostic copies. No primary
hit occurred. Crop search reverted to48; no broader range result claimed.

CURRENT NativeFoodStorage EXPERIMENT (not deployed primary): urgent native VILLAGE
residents may survey physical containers across own actual settlement up to64,
rather than12; ordinary/captive searches stay12, unavailable body3. Contents remain
unread until real three-block contact. Actual native loot and one actual item
transfer only. Village target lifetime1200 permits fair planning and real walking.
v160 focus600 COPY PASS,186living, but NO meal/transfer; Ember16HP/0food, still
near home; native long chest route incomplete. v161 added scoped fresh native
route search/fall2/32node-budget multiplier, dry own-settlement node predicate,
128node max. Native physical movement unchanged. Its600tick COPY also PASS with
NO transfer; native route to chest(-439,64,-827) stops at(-436,63,-820),47nodes,
canReach=false. lastPath report added to NativeFoodStorage status. Scope cannot
solve an actually disconnected house by granting movement or free food.

v162 FROZEN operator-only geometry COPY RUNNING port25604/session94354,
/tmp/villagers-v162.log. ResidentMealProbe surveys actual physically valid chest
interaction stands (native floor/body/outline reach), more native approach paths,
and the existing FoodAccess paid natural-soil passage planner to that chest.
It never reads container contents, issues a walking path, alters blocks or
teaches coordinates. Check this evidence before choosing another fix; don't
assume a door flag or extra search budget is the cause. Primary remains v157,
STOPPED292177,186alive, village Ember20HP/1food and no actual meal yet.
NativeFoodStorage latest build549Java PASS; latest production jar is isolated
/tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar. Source hash
check of primary is pending for v162. No actual source edits while primary live.

## Latest — actual stored meals and stocked camp rescue, v162–v168

v162 frozen geometry PASS but store survey was empty: the tool searched48 while
chest was one block outside that box. Corrected diagnostic to64 (production crop
search remains48). v163 frozen PASS proves a complete dry native route52–57nodes
and existing FoodAccess passage53steps/0digs. Stands include door(-439,64,-824)
and real room(-439,64,-825), etc. No chest contents read or coordinates taught.
v164 tested NativeFoodStorage walking-distance budget128 (geographic storage
search64 remains). All route nodes within native home settlement bounds, no wet
nodes; direct native route53nodes to(-439,64,-825).600tick COPY PASS,186alive,
2actual bread transfers, Ember19HP/10food. v165 allowed --camp-meals/--camp-passages
for focused resident rehearsal,600COPY PASS,186alive,3real bread/19HP/15food.
All549Java tests PASS before actual deployment; source unchanged in copies.

v166 ACTUAL600 PASS,186alive, Ember18HP/0food still partway to chest. v167
ACTUAL600 PASS with closer camera, actual4bread and20food; current primary above.
NativeFoodStorage range/route changes are now actually deployed. Strict camp12
and injured/unavailable3block searches remain; only urgent native villagers may
survey own real settlement containers to64, with1200target life. Contents/loot
are touched only at real reach; native dry/fall2/own-bounds/128nodes checks remain.
No nativeCampFarming enablement in primary; no camp crop proof yet.

NEXT v168 fullcare COPY of CURRENTv167 is RUNNING:
`runs/native-stocked-camp-rescue-v168`,1000ticks80TPS,allresidents, camp meals and
passages true, farming false. Port25604, /tmp/villagers-v168.log (tool session13221?
see actual process.json). Primary must remain stopped until hash checks complete.
Pip now really owns3bread+6wheat and can attempt both starving injured captives.
Check actual handoffs and Ember-c73f food/HP, not just trial status. Allresident
copies can also stop on the distant Pip-3c29/husk encounter from v159; distinguish
that event from a food-route failure. Preserve all192 identities/living count.
Evidence: `docs/evidence/2026-11-11/native-village-stored-meals/`.

Recovery requests currently become urgent only atfood<=4 orhealth<half. Injured
actors above half health can call for recovery food but may have difficulty
getting enough real calories to cross18food for healing. Consider actual spoken
HURT requests if needed; do not bypass decoded words or read remote private needs.
This is an idea, not an implemented change. Farm production and extra actual
workers matter more than endlessly draining finite hay/chest loot.

Only one managed illager founder survives; no births yet. Any additional workers
must be existing native bodies with unchanged UUID/inventory, activated openly;
never replace the deceased founder or spawn substitute adults. Preserve death
history while developing ordinary paid families and renewable supplies.
Continue real mining/equipment and Nether/End/dragon progression; goal active.

Build:
```
source tools/java-env.sh
villagers_select_java
./gradlew --project-cache-dir /tmp/villagers-mineral-gradle-cache \
  -I /tmp/villagers-mineral-isolated-build.gradle build test
python3 -m unittest discover -s tools/tests -p test_population_audit.py
```
Care COPY: tools/food-access-audit.py --production-jar <verifiedJar> --illagers
--rehearse --camp-meals --camp-passages --all-residents --illager-ticks 1000
--tps 80; fresh runtime. Do NOT use --cell-escape in primary.
Mining COPY: --boats --rehearse --quarry --until-iron-armor --boat-ticks 12000.
Actual: tools/observe-native-play.py --runtime runs/FRESH --ticks 1000
--min-health 4 --tps 80 --simulation-distance 16 --observer -640 90 -606
--allow-quarry. Existing injuries require a justified guard; don't blindly lower
it. No source edits during actual runs. No primary advancement while a source
hash-checking copy is running. Diagnostic copies never replace primary.
