# Villagers Project Lead — focused continuation

Updated2026-11-07 after rainy-day wake correction, actual repeated beetroot harvest,
three new wheat grains, native bread crafting/eating and sunset/reopen. Audit done.
Read this, newest evidence and next-task files only. Previous checkpoint:
archives/HANDOFF-pre-weather-wake-2026-11-07.md. Do not repeat old audits/acceptance.

## Full continuing goal

Lead Villagers toward a performant Minecraft society that crafts its own things,
has children, protects its village and develops interesting histories, language,
relationships and adaptation. Coordinate threads and keep improving present behavior
and future ideas. Keep usage low with fresh focused contexts carrying this FULL
original goal and a sharper handoff. Do not mark the broad goal complete because
one bounded feature passed. Source audit is complete. Continue concrete progress.
No new workers/timers for resource coordination; finished audit workers stay finished.

## Workspace, validation and permissions

Canonical /home/niko/.internal/source/Villagers. Most source/docs/tools untracked,
README modified. NEVER reset/clean. Source0.4.1-dev UNRELEASED; no commit/PR/release/
deployment/service change by Lead. Preserve deployed0.4.0, user clients/worlds and
all actual deaths. MC26.3,Loader0.19.5,FabricAPI0.162.0+26.3,JDK25.0.5+8,Gradle9.8.1.
Mind schema7,work-sites schema5. Current286Java+10Python PASS,0failures/errors/skips;
last build60046exit0: evidence/2026-11-07/dawn-food/wake/build-fixed.log.
Initial56647exit1 was a diagnostic helper's undefined level variable, fixed to
v.level(); all successful native runs use the passing source. Normal check:
`tools/test.sh --no-daemon --max-workers=2`. No need to rerun unchanged suites.
Current devJAR SHA c730dddc2d378fd993ee4eaf15d5b0601ea4cb4fcb444090050fcf7f70132abe.
Source+installed0.4.0 SHA4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0.
Installed /home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar.

User chose limited autonomous building using real materials, protecting player
structures. Do not ask again. Read design/terrain-permission.md before terrain work.
No blanket logging/demolition/chest looting/free supplies/block-type ownership guesses.
Finite grants schema5,128KiB:8farms/81cells each,8stations,8forestry,32zones/4096blocks.
Protection wins; outside writes revoke; dirty-session marker revokes edits but keeps
protection. Unknown/bad metadata disables construction. Raw chunk editors bypass
native setBlock hooks: revoke before unsupported edits. No global crash atomicity.
Resource paths still require complete permitted routes; no partial-resource analogy.

## Current primary history — THREE adults, both deaths retained

Continue runs/dawn-food/play, stopped86062. It copied286files from farm-regrowth
at69646 AFTER Juniper's zombie death66151. The baseline then lost offspring
Fern-589b589b708d-b768-4a61-a59e-910db084b079 to starvation81487. It was already a native
adult. Fixed source and reopen continued this actual newer history, never restored
an older branch. Three adults now live: Fern5ad3,Darac7c3,Nelle358. Original-cohort
3alive/8dead/0missing excludes the additional offspring death. No new birth.

NEVER continue the older farm-regrowth four-live or pre-death five-live comparators
over this world to erase deaths, restore resources or merge same-UUID histories.
Old source worlds remain unchanged read-only references. Legacy selectedParentChildren1
is the deceased offspring's mind file, not a living child. Lifecycle terminal events
and current loaded entities override stale historical alive/health/disk-path fields.
Default user runs/play last independently4originalalive/7olddead/0missing/NOchild33104;
never merge fixture histories into that user world. All previous fixture supplies
remain labeled historical background, not generated-baseline survival proof.

## Latest correction and actual food acceptance

Read design/weather-refuge.md and evidence/2026-11-07/dawn-food/README.md.
55recorded-data/source assertions pass. Scope only VillageSurvival and FamilyProbe;
authority, recipes, metabolism, claims and partial-route policy are unchanged.

Old getSkyDarken>=2 treated rain as dusk. Actual rainy DAY73911/81647 left parents
asleep/SHELTER, food declining,0harvests/recipes; offspring starved81487. The fix
reads native environment adult/baby VILLAGER_ACTIVITY at actual position: REST owns
refuge. Native vanilla schedule beginsREST12000, preserving sunset lead without
weather confusion. Actual threats retain priority. Calm non-REST daytime wakes a
native sleeper even when newly loaded session intent starts NORMAL. Sleep persists
across load, intent does not: do not use old intent as the wake-up gate.

Use native activity/day clock, NEVER gameTime%24000. Observed clock differs by94578.
Native26.3 timeline data under dawn-food/native-clock-data. First fixed frame81667:
clock176245,skyDarken3,rainingtrue,nightfalse,Activitywork,Fernawake. No weather/clock,
growth, inventory, health/fatigue/age/action/body/terrain/RNG setter. Weather later
cleared naturally. Only readonly watch/status/oneidleprobe commands were issued.

Before fix: Fernfood9,wheat1,seeds12,beetseeds4,no bread/beets,ironhoe10. After4000ticks:
4paidharvests (3WHEAT+1BEET),4stockreplants,hoe14,seeds19/beetseeds6,wheat1,bread0/beet0,
food15. Beet(-230,72,752) was earlier paidage0replant, naturally grew3, harvested again,
paidanotherseed,eaten and nowage1. Actual renewable beetroot cycle, not suppliedripe.

First2newgrain+1heldgrain became3. Receipt81937(-230,72,751): actualwheat1/seeds3,
wheat2→3. Native crafted1breadfrom3wheat at actualtable(-233,72,746), then ate it.
Laterthirdnewgrain(-226,72,753)restored buffer:1+3−3=1. Do NOT claim the bread initially
used3newgrains; distinguish capture times. day-observation.json preserves notes/earlier
receipt before overwrite. Finalstock and shortmemory also bound. One cycle is NOT
adequate long-run yield or offspring survival. Native sunsetclock180230,REST and
sleepingparents while nightfalse; shortreopen clock180635/nighttrue remainsasleep.

Handlesdirectexit0:10159baseline12000ticks69647→81647/save81651,mean3.253553ms;
59205fixed4000ticks81652→85652/save85656,mean3.201894;
10552reopen400ticks85657→86057/save86062,mean4.148681.
Three-adult snapshots, NOT speedup/scale/tail proof. Reopen seeds19/beetseeds6,wheat1,
food15,hoe14/cropspersist,0newharvest/recipe, bothdeathsremain. All8farmlandmoisture7.
Crops: wheat(-230,72,751),(-228,72,752),(-226,72,753)defaultage0; beet(-230,72,752)age1,
otherbeet0; remainingwheatages6,6,5. Saved Name-only vanilla crop means defaultage0.
Current source-wake.zip SHA
c5812f85950b348724c21cc476ed8e2da312dbdd8f0c2e787f11b76e98aeaa1c.
Older pinned source differs intentionally; never rerun old verifiers against new main.

## Next focused work — adequate real farm capacity

Do not repeat the weather, bed-floor, first bread/beet, old wood/table or audit cases.
Read FarmExpansion,ConstructionSites,PlantingMaterials and terrain rules. The bounded
17×17,Y70..72 saved farm survey found oneDIRT/GRASS cell already crop-occupied and
no exposed eligible bare soil. terrain-before/report.json is a survey, NOT ownership
or native path approval; it does not rule out wider terrain. Current8plots cannot
be claimed sufficient from one recipe. Three adults still lack held ration reserves.

Find a real authorized way to add productive farm capacity: first inspect actual
available ground/water and costs. If soil must be gathered/placed, use genuine
materials and finite explicit edit authority with all existing protection/revocation/
dirty-session safeguards. No free dirt, supplied seeds/food, crop-age/time edits,
road/landscape conversion by block-type ownership assumption or silent home widening.
Use earned seedbank and native till/plant/tool costs. Target useful productive output,
not a test-only generic selector or already-passed gift acceptance. New schema/types
must not let old writers reinterpret future grants. Keep work/path/state bounds.

Nell's separate rescue issue is now grounded: native idleprobe70121 at
(-256.828629,68,779.387396),home(-236,72,766),STANDING/onGround/collisionFree, no body
intersection. Homepath1nodefalse; four localcardinal routesfalse. West/south snap to
Y58/57 drops; east/north toY70/74 terrain. terrain-before retains columns. This is
NOT clipped-start geometry: FloorCentering correctly does not activate. No other
route universally ruled out, no movement/permission fix yet justified. A rescue may
need safe real construction/material delivery; never teleport or blindly accept drops.
Do not interrupt navigation just to run an idle probe. Keep broader rescue/defense goal.

Full society work stays adequate food/wood, paid construction/material logistics,
parent care, defense, language/history/adaptation and population performance. Keep
original goal active and pass it with this sharper actual scope to the next context.

## Resource custody — no active hold/worker/timer

All current owned native chains captured live and exact identities terminal:
995550/15713068,995655/15713129,995845/15713654;
1004275/15795171,1004376/15795230,1004583/15795609;
1008656/15833412,1008766/15833471,1009001/15833737.
No active owned build/game/test/local-model job,pendingcommand,automatedrelease/build
Timer,newworker or service change. Earlier audit_domain,audit_tools_trials,
visible_current_source finished; do not wake them just for status.005closed/spent,
006expiredunused,007earlyreleased09:54:54. No model/hash/unit/engine/successor/grant001/
inference approval. Historical resource evidence immutable. No inherited PID exception.
Short old refuge-backoff36106 missed exact census after terminal0; explicit gap remains
in refuge-floor/backoff/custody-gap.json, never repaired with fabricated tuples/relaunch.
All future admission requires fresh independent exact data; no vacuous empty-chain proof.

Original adaptive601856/10936249,601946/10936303,602326/10937021 and frozenwrapper
601857/10936249 remain ABSENT. No Lead stop/signal or cause/replacement inferred.
Frozen daemon601990/10936325 and game602372/10937054 remain exact at latest capture.
Preserve all original clients/worlds,Modrinth,Buddy,Niko. No attribution of unrelated
Java from shared scopes. New coordination requires fresh exact custody and explicit
finite agreement; no periodic resource polling or implicit deploy/restart permission.
Head <t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/2a6b199a-0bbd-4884-887d-119d63e29b97>.
Lead e551a259-e755-4223-9927-474c3c52b7ca. Recruitment completed0.4.0; coordinate before
changing its home/bed guards:
<t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/9d5b9bc5-09e6-4ff5-99c1-a09d3982e712>.
Read a named thread before answering about it; do not reread completed audit/status.

## Earlier verified behavior — keep, do not repeat

HarvestMaterials: owned plantingitem first or actual native loot once atreach,
fullslotCAS/oneuse beforewrites incl unchangedfullbag+overflow,40attempt/200unfunded,
realoverflow/hoewear,loaded/sight/protection/tool/crop/soil rechecks,age0samecrop.
Old grain-only earlyreturn diagnosis was WRONG: defaultsearchincludesall4crops;
VillagerController.stockCropTarget unchanged. Source receipts bounded last only.

RefugeRoutes: existingbedpath first, otherwise complete coveredstandinggoalwithin2.5,
75cells/3homequeries,exact1..128nodes,loaded/fullstandingclearance/safefooting/nativeLOS,
cachedrevalidation,200failedsearchbackoff samecell,ordinarybed/threat/escapeunchanged.
Actual34node and final6node floorpaths followedbyarrival/sleep; diagnosticbaseline also
sheltered, no causal survival-rate claim. Partialpolicy/TraderRecruitment unchanged.
Groundrest80quietticks; clipped-start FloorCentering currentcell/drop<=1.25/MoveControl/
gravity40ticks restoredJuniper fromoldrooftrap but hasno applicationto Nell's clearledge.

Nativewildseed→3approvedleaves→1separateapproveddryroot→2logs→paidpropagule, then
oneearnedlog→native4planks→tableitem→paidtable(-281,75,745),protection/restart/no replay
passed. Juniper's remaininglog/root were carried BEFOREdeath: never restore them.
Natural treegrowth/secondwoodcycle still unproven. SourcehasPurpose fed>6 optionalwood,
existing hungryfood exception,no-jobscan fastpath. See forestry-access/earned-station
only if scope needs it. No blanket forestry from old leaf permission.

FoodHelp real voluntary food sharing via actual understood WANT FOOD,16visiblepeers/
100ticks,600requestbudget,400trip/80stall,home/reach/sight/donor guards. Native bread
donation+eating/reopen and laterparent→offspring donation passed, not free nutrition.
Ordinary offspring born36379 toFern5ad3+Dara c7c3: consent,actual3bread/12unitsEACH,
spare roofednativeHOME,genes+27words,freshlearning,reopen no duplicate; nowDEAD81487.
Minddeadline60379 andnativeage6000cooldown separate. Preserve all family guards.
Paid farm plot(-228,71/72,752) actualseed/hoe/protection/revocation/restart passed;
its wheat now yielded realgrain. Hay25 defaultdisabled. FoodSupply15heldnutrition,
rawgrain<=9,independentfamily readiness; MealPolicy32ticks/efficientrealfood unchanged.
RecoveryCheckpoint EMPTY-only,Animatica idle4workergracefuldrain,boundedplay normalquit
pass; other branches remain open. No auto-dismiss of user menus.

Native boundedplay100..12000ticks; quit adds a few shutdown ticks. Capture exact
owned chain immediately in same tool orchestration with /tmp/villagers-capture-owned.py
TAG OUT(O_EXCL). Recover confirmed live handles; NEVER relaunch solely on lost
observation. Keep this full goal active; use a fresh focused context with this handoff.
