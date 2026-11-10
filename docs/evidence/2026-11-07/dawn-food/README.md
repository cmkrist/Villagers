# Rainy-day wake, repeated harvest and native bread meal

Rainy daylight was treated as dusk, leaving sheltered residents asleep. Fern's
offspring starved during the baseline. Current source uses the native REST schedule
and wakes sleepers after daytime load. The same continued world then produced four
paid harvests, one native bread recipe and actual meals. Both deaths remain retained.
286Java+10Python pass;55recorded-data/source assertions pass.0.4.1-dev is unreleased.

## History and actual inputs

runs/dawn-food/play copied286stopped files from farm-regrowth/play69646, preserving
Juniper's zombie death66151 and four then-living residents. The parent is unchanged.
The baseline lost Fern-589b at81487: native server log and lifecycle identify starvation.
That offspring was already a native adult. Fixed and persistence cohorts continue
the newer three-adult history without restoring an earlier save or genealogy.

Only read-only watch/status and one idle route probe were issued. The first probe
batch was rejected while watch remained pending; deferred-probe.txt records that it
was not queued, then it was queued after consumption. No supplies, terrain, growth,
weather/clock, actor/body/health/fatigue/action or RNG setter. Earlier supplied
fixture resources remain background, not generated-baseline survival proof.

## Failure and correction

At73911 and baseline endpoint81647 it was raining and not night. Fern was asleep,
intent SHELTER/dusk=true; food fell to9, with zero harvests/recipes. The offspring
had been asleep at food6 in the earlier frame. Sky darkness includes weather.

Current VillageSurvival reads native adult/baby activity at the actual position.
REST defines refuge; vanilla's loaded schedule begins at12000, preserving sunset
return. Calm daytime waking no longer depends on a prior session-local SHELTER
intent, which resets to NORMAL on load. Actual threats retain priority. Only this
timing/wake behavior and diagnostic fields changed; authority, recipes and metabolism
are unchanged. Initial build failed on a diagnostic helper's undefined `level`;
corrected to `v.level()` and passed the full suite before any fixed native run.

First fixed frame81667: rain=true,skyDarken3,night=false,nativeActivity=work,Fern
awake. No weather/time change. Native clock176245 differs from gameTime81667 by94578;
gameTime%24000 is not the day phase. Weather later naturally cleared. Loaded26.3
timeline resources are retained in native-clock-data; code uses the environment
attribute, honoring native timelines rather than hardcoding these JSON values.

## Actual food cycle and conservation

Fern began with wheat1,wheatseeds12,beetseeds4,zero bread/beets,ironhoe10/250. Final:
three wheat harvests,one beetroot harvest,four stock-funded replants,hoe14/250;
wheat1,wheatseeds19,beetseeds6,zero held bread/beets. No new material supplied.

Beetroot(-230,72,752) was the earlier paid age-zero replant. It naturally matured
in the baseline, was harvested again, paid another seed and was eaten. It is now
replanted and naturally age1: a real repeated beetroot cycle, not another supplied
ripe plant. The other beetroot remains0; eight farmland cells remain moisture7.

The first two new grains joined one previously held grain. Direct receipt81937 at
(-230,72,751): wheat2→3, actual drops wheat1/seeds3 and paid age-zero replant.
Native notes record one bread crafted from three wheat at table(-233,72,746), then
eaten. Thus this bread initially used two new grains plus one held grain. A later
third new wheat replenished the buffer:1+3harvested−3recipe=1. The final last receipt
is that later crop(-226,72,753). The short memory ring/last receipt can overwrite;
day-observation.json retains the earlier sequence at its actual capture time.

Food9→15 follows an actual bread and beetroot meal. All three harvested wheat crops
are saved at defaultage0. At fixed endpoint clock180230, native REST and sleeping
parents coexist with night=false: proactive sunset return remains. Short reopen
retains stock,crops,hoe14,food15 and sleep, with zero new harvest/recipe.

Juniper66151 and Fern-589b81487 remain dead. Legacy selectedParentChildren1 refers
to a deceased offspring mind file, not a living child or new birth. Three adults
remain. Original-cohort totals3alive/8dead/0missing exclude the additional descendant
death; use terminal events and actual loaded actors rather than stale alive fields.

## Nell and expansion constraints

Native idle probe70121: Nell grounded,STANDING,collisionFree=true,no intersecting
blocks,home path1node/unreachable. Four local cardinal paths also fail; two snap
toward deep drops and two toward higher terrain. The bounded stopped-parent survey
retains geometry. This is not the old clipped-start case; it does not justify a
centering change, unsafe partial route, teleport or excavation. Other routes are
not universally ruled out; any material-based rescue needs actual geometry/stock.

The bounded17×17,Y70..72 farm survey found one dirt/grass cell already occupied by
a crop, zero exposed eligible bare dirt/grass. This is not whole-village ownership
or permission to till roads. Useful expansion may need more surveyed ground/water
or real soil gathering/placement. Eight crops and one grain-buffer refill do not
establish adequate long-run household yield or future offspring survival.

## Runs, custody and source

| Phase | Handle/exit | Native cutoff/final save | Mean tick ms |
| --- | --- | --- | --- |
| Retained-history baseline |10159/0|81647/81651|3.253553|
| Native REST/daytime wake |59205/0|85652/85656|3.201894|
| Sunset persistence |10552/0|86057/86062|4.148681|

All original handles directly exited0. Captured exact tuples, all terminal:
995550/15713068,995655/15713129,995845/15713654;
1004275/15795171,1004376/15795230,1004583/15795609;
1008656/15833412,1008766/15833471,1009001/15833737.
Means are snapshots, not speedup/capacity/tail-latency proof. No active owned job,
pending command, automated build/release timer or new worker. Original user clients,
worlds/services and0.4.0 deployment unchanged. Adaptive3/frozenwrapper stay absent;
frozen601990/10936325 and602372/10937054 remain exact. No cause/replacement or shared
scope ownership inferred. No hold, model/hash/unit/grant activation;005/006/007 untouched.

Baseline binds prior refuge-floor/source-backoff.zip. Current source-wake.zip SHA
c5812f85950b348724c21cc476ed8e2da312dbdd8f0c2e787f11b76e98aeaa1c binds fixed+reopen.
DevJAR SHA c730dddc2d378fd993ee4eaf15d5b0601ea4cb4fcb444090050fcf7f70132abe.
No PR/commit/release/deployment. Continue this three-adult history; never restore
an earlier four/five-resident branch to erase deaths. Full society goal stays active.
