# Goal active — actual guarded continuation v116

Authoritative primary is STOPPED normally at **gameTime287173**:
`runs/native-four-regions-play/saves/NativeFourRegions`.
**186 saved humanoids remain, down from187.** Actual founder **Juniper-e9b7**
`e9b7b192-009b-4158-bc2d-1a63456a6f91` drowned at(-603.47,47.75,-613.47)
during actual v108. The client log explicitly confirms drowning; this is not
an unload or an audit ambiguity. No rollback, copied-save promotion, replacement
or resurrection. Historical founder records retain her UUID. Do NOT claim every
original body is alive. This outcome was reported to the user immediately.

All56 managed villagers remain alive with nonzero food. Pip-4c3c healed from6HP
to20HP through real meals/metabolism. Other current injuries:
Birch-67f3 7HP/18food, Clover-d10c9HP/15food, Juniper-e3de16HP/5food,
Iris-240e18HP/16food, Willow-110419HP/16food. No newborns, armor, diamonds,
Nether/End entry or dragon victory verified. The goal remains active.

Surviving founder Pip-460a `460ab4b6-1522-4504-9673-25fd503e0610`:24HP/17food,
bag1beetroot,6wheat,1bread; offhand1bread. Four actual gifts in v108,
two in v116. **Dara-f13a is20HP but only2food** at(-633.72,64.5625,-601.30);
she needs a real meal before longer progression.
Adventurer Clover-edc5 retains20HP/20food, earned iron pickaxe damage24,
rawiron1, dirt30 and prior tools/food; see archived v106 inventory for details.
Current old authored FourBiomesWar hashes still match. Shared dirty tree: never
reset/clean others' work or overwrite pinned runtime JARs.

## This turn's findings and code

The older background exit143 notice is historical (Nov9); later terminal files
show successful runs. No simulation was live when this continuation began.

NativeCampCalls now permits an actor's own recovery-food need to cause a loud
call even above6food. Less-than-half-health actors use the existing volume8
alarm. Only real sound is heard; understood words and real contact still govern
meals. Copied v107 ran7,640ticks:12real gifts, Pip/Birch/Clover fully healed,
then stopped at4HP for Dara-f13a. Earlier statement that Dara was in another
settlement was mistaken: she is a captive near(-632,64,-602) in the same camp.
She had0camp requests in this window despite hunger. Inspect communication and
home structure membership; do not infer generic stock shortage alone.

Actual v108 ran the copied safe4k recovery window, but native trajectories
varied: Juniper-e9b7 drowned. Actual health guard only checked managed villagers
and missed the illager. Population audit failed appropriately after normal
save. Source hashes unchanged; authored world unchanged. Actual v108 result is
NOT a population-preservation pass. It did record real recovery and4Pip gifts.

Root cause found in CreatureMobMixin: replacing all goal-selector ticks also
disabled the existing native FloatGoal. Managed actors could choose REST while
underwater. New code explicitly runs only the already-registered FloatGoal
when its native canUse holds, on each controlled serverAiStep. It requests native
JumpControl actions; no position/velocity/air/damage setters or new float goal.
CreatureAgents interrupts optional work/REST when in water and calls the new
NativeSwimming helper to choose visible physically supported dry shores using
native routes, a2block fall bound and loaded/local/border/lava checks. Native
floating continues even if no shore route is available. Water status reports
native float ticks, actual air, position and latest route receipt.

ObservationHealthGuard now covers health/depleted underwater air. Actual
LabClient checks ALL loaded villagers/illagers, including unmanaged bodies,
every tick. Copy NativeIllagerHabitatProbe uses the same guard every tick rather
than waiting40ticks. Instrumentation only freezes/saves; never alters bodies.
The probe records previously absent historical founders separately and checks
only the founders actually alive at this window's start. Never hide a new loss.

Current535Java tests pass; build log
`/tmp/villagers-native-listening-boundary-tests.log`.
Verified JAR `/tmp/villagers-mineral-verified-build/libs/villagers-0.4.1-dev.jar`,
SHAc5ed83c53dc71d015f57f1ba0e4486942ec27b3a3b48b2542f60e47f8823f376.
Diff check passed. New code was deployed through recorded development classes
in actual v116:full1k,186→186,no new/missing UUIDs,min managedHP7,source and
authored-save hashes unchanged,sim16 verified. Pip delivered2beetroot and Birch
began natural healing. No actor entered water in the successful copied/actual
windows (nativeFloatTicks0), so do NOT claim a live drowning rescue was proved.

## New native checks and pending diagnosis

- v110 from actual v108: failed safely at3638ticks for Dara4HP; all186 retained.
  New every-tick health guard worked; this is not a long-term food pass.
- v111/v113/v115 frozen Dara: same-camp bounds valid, real urgent words can be
  formed and decoded by both saved founders. An early false decoding report
  came from checking an asynchronously unloaded listener mind; the final probe
  loads its saved vocabulary before the diagnostic. No forced vocabulary edits.
- NativeCampCalls now seeks a listening stand within actual word-hearing range,
  rather than demanding an interaction stand3blocks from an uninterpreted voice.
  It leaves a one-block margin around the public sound's block center and waits
  220ticks. Actual handoffs still need visible native reach3. Frozen v115 found
  no complete safe route in12bounded batches per donor; the listening fix alone
  does NOT prove Dara can receive a meal.
- v112 pre-loss DIAGNOSTIC source (v105) ran6k,187→187,nine real gifts. Both
  copied founders24HP; no water entry. This is not a restored primary.
- v114 exact actual-v108 copy:full1k PASS,186→186,three gifts. Completed source
  hash check BEFORE primary v116 resumed.
- v116 actual continuation:full1k PASS,186→186,two gifts,as described above.
- v117 frozen voice topology at failed-v107 state:525 physical listening stands
  near Dara. Even native queries WITHOUT body avoidance end after1–3nodes near
  donor start for eight nearest targets. This suggests a start/terrain/drop
  problem rather than proving that other bodies alone block the route.
- v118 `runs/native-dara-native-drop-inspection-v118`, source immutable v117
  copy, frozen. Compares native2block/3block search and actual floor drops for
  those same goals. PASS,source unchanged:3block searches ALSO fail with1–3nodes.
  The larger drop bound did not resolve this geometry; do not loosen production
  safety based on that rejected hypothesis. Actual inspected donors were
  Pip(-640.47,67,-576.50),Juniper(-610.56,65,-603.72) in the OLD diagnostic save.

All actual/copied Minecraft processes are terminal. No copy is pending.

Latest evidence `docs/evidence/2026-11-11/native-floating-and-survivor-care/`.
Older v106 handoff archived `docs/HANDOFF-history-through-v106.md`; it contains
all prior build/run commands, mining, camp care and navigation details. Earlier
all187alive statements apply to earlier worlds only.

## Continuation priorities

First verify restored native swimming and full-body health/air stops in copies,
including an existing pre-loss diagnostic save if needed. Do not advance the
primary merely because a copied trajectory survived; actual guards must cover
all bodies. Preserve the actual loss and surviving identities.

Resolve Dara's missing camp food calls and route/contact issues. Renewable camp
farming is PLANNED, NOT implemented: no nativeCampFarming config/class or seeds
were added. Use real planting material, crafted/owned hoe, natural hydration,
age0plants and ordinary growth; avoid endless finite hay/chest looting.
Do not claim promised farming exists. Native village FarmExpansion already
exists but is Villager-specific and excludes camp structures. Prior reads found
WoodCrafting.next(Container) can plan real log→plank→stick→hoe recipes; camp
storage currently does not collect seeds/hoes for this purpose.

Then continue actual food recovery, paid family housing/meetings, earned mining,
armor/diamond and real Nether/End/dragon progression. No genuine End victory or
native births in this primary have occurred.

Build:
```
source tools/java-env.sh
villagers_select_java
./gradlew --project-cache-dir /tmp/villagers-mineral-gradle-cache \
  -I /tmp/villagers-mineral-isolated-build.gradle build test
```
Copies: tools/food-access-audit.py --production-jar <verifiedJar>, fresh runtime,
--illagers --rehearse --camp-meals --camp-passages --all-residents --tps80
(use separate option/value, e.g. --tps 80), --illager-ticks1000..48000.
Actual: tools/observe-native-play.py, explicit --simulation-distance 16,
--observer -640 90 -606, --allow-quarry; account for existing6HP injuries.
No source edits during actual runs. Do not resume a source before copied audit
source-hash checks complete. Diagnostic copies never replace the actual save.
