# Goal active — authoritative state after v106

Primary STOPPED normally at game time **282171**:
`runs/native-four-regions-play/saves/NativeFourRegions`.
All187 saved humanoids are alive, including every original179. No identities
were added in this turn. All56 managed villagers have nonzero food after a real
emergency rescue. Several remain injured; do not describe them as fully healed.

No NPC position/velocity/health setters, free supplies, replacement adults,
rollback, copied-save promotion or synthetic births. The old authored
FourBiomesWar hashes still match. Goal remains active: no verified births,
armor, diamonds, Nether/End entry or dragon victory.

Latest evidence: docs/evidence/2026-11-11/native-supply-and-simulation-coverage/.
Archives through v44/v58/v70/v81/v89 retain earlier details. Dirty shared tree;
never reset/clean unrelated changes or overwrite a live runtime's pinned JAR.
All actual clients and copied servers are terminal; no live Minecraft process.

## Immediate care state

Actual v106 completed1,000 ticks with nine conserved bread gifts: Pip-460a four,
Juniper-e9b7 five. Both founders24HP; food19/20. Their UUIDs:
Pip `460ab4b6-1522-4504-9673-25fd503e0610`,
Juniper `e9b7b192-009b-4158-bc2d-1a63456a6f91`.
Pip has one held bread and no bag food. Juniper has four beetroots and an empty
offhand, and is collecting real supplies. Both have zero family attempts.
Pip's cumulative paid-bed placement count is2; these were existing owned items,
not evidence that raw bed materials were crafted this turn.

Actual injured managed villagers, all alive:

- Pip-4c3c `4c3c0db4-75ea-42f5-8f91-b1c4814944d7`:6HP/15food.
- Birch-67f3 `67f32c45-c10a-4f05-a74d-d1d31fdeac0e`:6HP/10food.
- Clover-d10c `d10cee59-b8c5-41fa-863a-1e455d73749b`:9HP/10food.
- Juniper-e3de `e3de735e-23c0-421b-a93d-34e386a06ff2`:16HP/5food.
- Iris-240e:18HP/16food; Willow-1104:19HP/16food (earlier native falls).

Birch-764e and Rowan-8889 each20HP/6food; Dara-f13a20HP/6food.
No managed villager is at zero food. The four starving captives were rescued,
but natural healing needs more real calories. A default health guard12/18 would
stop immediately because some actors are below it. The successful v106 rescue
used guard4 after an exact-state copied rescue proved feasible. Preserve actual
injuries; never restore a healthy snapshot. Future guards must account for the
current state, and lowering one requires actual safe evidence, not a blind run.

Potential next issue to inspect: NativeCampCalls.emit currently suppresses
voices when food>6, even for a badly injured actor needing recovery food.
Ordinary nearby spoken requests still occur, but may not attract a distant
provisioner. Use the actor's own recovery need and real audible cues if this is
confirmed; do not read remote private hunger or invent free healing.

## Actual mining state

Clover-edc5 adventurer `edc5ea2a-f488-4aea-961c-456c3f7200f1`:
20HP/20food, sleeping at(-466.5,64.5625,-376.5). Held earned iron pickaxe damage24;
offhand wooden pickaxe damage4. Bag stonepick70, woodenhoe17, dirt30, bread2,
wheat2, carrot5, stick4, rawiron1. No ingots/armor.75 retained footings, minimum54.

Actual v97 mined ten blocks before native night rest. Older actual v81/v88/v89
placed three paid dirt foundations (-469/-468/-467,55,-381), dirt33→32→31→30.
Do not merge copied miner stock or session counters into this save.

Copied v91 ran12k from the old primary and mined24 blocks before night rest.
Copied v95 continued that copy for16k, mined150 more blocks and ended with two
raw iron, pickaxe damage188,131 retained footings, minimum54,20HP/20food, no armor.
Night rest explains the late-run inactivity. Mining does work on the next day;
its remaining problem is shallow horizontal work/rare ore, not a proven broken
nighttime timer. Watch finite pickaxe wear and legitimate replacement supply.

## Changes deployed and tested

IllagerFood can return to a safe point near its own actual remembered harvest
area rather than demand an interaction stand for an already removed hay block.
Only the normal local visible scan can select new hay; real contact, work time,
drops, recipes and stock costs remain. Frozen v92 showed all six remembered
bales were air and old approaches failed. v93 proved one new safe area approach;
others remained unavailable in that bounded query. Work status records hayReturn.

NativeCampWork no longer lets a carried bed with no placement site prevent food
collection. Supply exploration now uses the common body-aware camp paths.
Actual v97 crafted three loaves, collected real supplies and placed another
owned bed, but produced no deliveries in that window.

Native FarmWorksite recall now uses WorkNavigation.pathTo's64-block native work
range with a two-block fall bound. Frozen v98 inspected failed-copy Ember-8a41:
ordinary follow-range route31 nodes/canReach=false; work route32 nodes/true to
its actually remembered beetroot at(-441,64,-755). This does not prove long-term
recovery; its live test stopped for another actor before that question resolved.

NativeCampCalls now uses louder native volume8 for own food<=2, volume6 otherwise;
repeat intervals100/200/1200 ticks by critical/urgent/ordinary hunger. With the
usual hearing12, real auditory ranges are96/72, bounded by native sound range.
Louder heard alarms attract attention. Only nearby decoded words queue a meal;
understood louder requests receive priority. Existing urgent handoffs finish
before another uninterpreted voice can preempt them. No private remote health
or inventory is used to pick a needy person.

Healthy workers with>=18food can use reserved bag or held meals after an
understood urgent request. Normal gifts keep their prior reserve. Source counts
are conserved: receipts identify bag/offhand, ownedItemBefore/After, receiver
stock changes and usedEmergencyReserve. Actual v106 used this reserve; copied
v104 proved offhand1→0 and recipient0→1.

Urgent navigation uses the existing running speed1.1 only while sprint-capable.
CreatureAgents charges extra exhaustion from actual displacement while that
running path is active. No movement attribute/position/velocity changes.
Handoffs consistently use visible eye-to-body contact within3 blocks rather
than an extra foot-distance test that rejected raised-floor contact. Prospective
stands and gate/window operations still require real reach/contact; closed rays
cannot transfer food. Receipts now record handReach separately from foot distance.

## Simulation coverage matters

The primary previously ran simulationDistance12/renderDistance16. Juniper's age
advanced only570 ticks in a4,000-tick run after she walked beyond the ticking
radius, although she could still receive heard-sound records. This was not
sustained autonomous care. A file-only options edit was reset: v100 still ran12.

The launcher now supports --simulation-distance16 (use separate option/value).
It applies the actual client option before world open, bounds it by existing
render distance, and records/verifies the server's actual value. LabClient
telemetry includes server_simulation_distance and server_view_distance.
build.gradle forwards VILLAGERS_SIMULATION_DISTANCE. Always verify the actual
server value. Current intended observer is original camp center(-640,90,-606),
with simulation16/render16: camp and mine fit. This is not proof that every
other region is ticking. Copies with --all-residents remain necessary for full
population survival checks. No clock ticks are skipped by native80TPS pacing.

## Trial outcomes and source discipline

- v94 (hay-area return only):health_stop7920,Birch-764e starvation; five gifts.
- v96 (bed fallback/survey fix):health_stop13600,Ember-8a41 starvation;15 gifts,
  all187 identities. Ember was in a separate village at(-434,64,-782), remembered
  beetroots and had only seeds. Failed save remains useful for frozen inspection.
- v97 actual:4k,normal278971,all187,56managed20HP/nonzero food, but no gifts and
  coverage incomplete. Primary stock and bed progress are real.
- v98 frozen Ember farm check:PASS,failed v96 source remains unchanged.
- v99 farm fix/live all-resident copy:health_stop2720,Birch-67f3. Its wrapper also
  failed the source-hash check because actual v100 legitimately resumed the
  primary while the copied audit was running. This is NOT a clean wrapper pass.
  Manual stopped-copy NBT audit proves all187 identities. Do not repeat this
  scheduling mistake: finish copied audits before resuming their source, or use
  a separately pinned immutable source. Never roll back actual progress.
- v100 actual:1k,normal279972,all187,minimum20HP,still simulation12.
- v101 actual:verified simulation16,health guard18 after347 ticks,time280320.
- v102 actual rescue:guard8 after850 ticks,time281170; one gift,all187 survived,
  four actors still at zero food. This actual state was preserved while testing.
- v103 copy:one critical actor fed,health_stop720 for Birch-67f3; all187.
- v104 copy:reserve/offhand giving verified,still health_stop720; all187.
- v105 exact-state final rescue copy:full1000 PASS,eight bread gifts,all187,
  all four starving actors fed5–10food,minimum5HP. No source changes or promotion.
- v106 actual final rescue:full1000 normal save282171,nine bread gifts,all187
  alive,56 managed/nonzero food,minimum6HP,simulation16 verified. Source hashes
  stayed unchanged during the run; old authored-save hashes matched.

## Build and continuation

Latest532 Java tests PASS,zero failures/errors:
`/tmp/villagers-native-actual-hand-reach-tests.log`.
JAR `/tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar`;
SHA `e721fdfc866271a11d53f5ad77e545508878851c51caf49fe6ef634a3b8c9901`.
Python observer compiles. Diff check clean. Client loads recorded development
sources/classes; do not describe the separate JAR as actual runClient code.

```
source tools/java-env.sh
villagers_select_java
./gradlew --project-cache-dir /tmp/villagers-mineral-gradle-cache \
  -I /tmp/villagers-mineral-isolated-build.gradle build test
```

Next prioritize actual natural healing and recurring food production, not an
endless finite-loot loop. Camp illagers need native crop production if their
stores cannot sustain dependents. Preserve existing fields, seeds, hoe wear,
recipes and physical contact. Family growth still needs paid housing, surplus
food and ordinary meetings; no births have occurred. Continue earned mining,
armor/diamond, then real Nether/End progression and villager dragon victory.

A bounded actual continuation uses tools/observe-native-play.py with a fresh
runtime, explicit --simulation-distance 16, --observer -640 90 -606, --allow-quarry
and a justified guard fitting the current injuries (v106 used4). Do not reenroll
or replace founders. Stop/audit rather than blindly continuing into starvation.
For copied care: --illagers --rehearse --camp-meals --camp-passages --all-residents;
mining: --boats --rehearse --quarry. Always pass the verified production JAR.
Frozen boat output is .initial, not .actors. On observation expiry inspect the
same actual process/terminal; never restart solely because a wait expired.
Goal remains active; no required End objective is verified.
