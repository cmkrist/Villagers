# Native clipped-start recovery — unreleased0.4.1-dev

Juniper escaped the original roof/stair trap through ordinary movement and gravity,
then followed native partial paths toward the unchanged home. The native home
distance fell from47.80blocks to25.01after the fix and16.86after restart. No fixture
movement,terrain edit,clock change,resource supply,pose/collision/velocity setter
or path assignment was used. Reaching the bed and harvesting forest resources
are not claimed.

## Diagnosis

The stopped rest-recovery world was copied exactly (229files) into
`runs/home-route/play`. Read-only nearby block inspection found a small house,
stairs and low top-slab ceiling. `inspection/native-inspection.json` records:

- Position(-234.206902,73,766.7),standing height1.95,width0.6,onGround=true.
- Actual box top74.95 intersects two smooth_sandstone top slabs atY74.5..75.
- Native collisionFree=false,door opening enabled.
- Home path has1node at(-235,73,766),canReach=false. Four local cardinal diagnostic
  paths also have only that node. They were never assigned to navigation.

This explained why retrying distant targets and resting could not restore movement.
`FloorCentering` now checks a clear lower standing box centred in the SAME current
cell. In this case it targets(-234.5,72,766.5),away from the stair edge. Normal
MoveControl/gravity performs the step; no teleport or excavation.

## Actual sequence and preserved failures

| Phase | Handle/exit | Cutoff/final save | Result |
| --- | --- | --- | --- |
| Initial observation |88305/0|46377/46381|800ticks,position unchanged. The explicit probe was queued too late,never executed; exact unused batch archived and removed under lock. |
| Idle inspection |15058/0|47182/47186|Same world,no reset. One-shot probe captured46535while live. Position remained unchanged. |
| Centre and return |75151/0|48387/48391|1200ticks,1attempt/1success/0failures,3partial home routes. Body at(-245.083396,72,741.562533),inside28home range. |
| Read-only reopen |55186/0|48792/48796|400ticks,no repeat centring attempt. Continued movement to(-253.337298,73,740.427937),home distance16.86. |

Initial home distance is measured to the same bed-block centre. Home stays
(-269,75,733). `centering/movement-observed.json` already showed one successful
centering and two partial routes mid-run. Success is counted only after native
noCollision,onGround and lower-destination checks pass. The successful run ends
with1success/3partial routes; restart needs0centering attempts. Later idle path
probes were refused while Juniper was actively navigating,so no post-fix path was
assigned or disturbed merely to inspect it.

All phases keep5live villagers,7historical deaths,0missing originals. Juniper's
identity/home/history are retained. Mean ticks5.175131/4.863564/4.902593/5.300245ms
are five-actor snapshots,not scale/tail-latency capacity evidence. Earlier supplied
food/tools/station/nursery remain labelled background. This turn adds no resources
or drive/action setters. Source rest-recovery229-file world stayed byte-identical;
copied histories never replace default play or their source.

## Validation and source bindings

262Java+10Python checks pass,zero failures/errors/skips. Four new geometry tests
cover the actual negative-coordinate case,current-cell bounds,drop/width limits,
upward/other-cell rejection and invalid values. Runtime guards additionally require
actual clear collision/support and finite duration/backoff. Native accepted the
centering step and the previously unverified calm partial-return branch.

`build-probe.log` preserves a diagnostic getter compilation failure; corrected
`build-probe-fixed.log` passes258checks. `build-centering.log` passes262. The first
two native phases share probe-source.zip/pins; final two share source-main.zip/pins.
The older survival file was recovered from the prior pinned source archive and
verified against its original hash. `verify.py` checks both archives,actual native
roof intersections,unchanged home,substantial movement,home-range recovery,no
repeat after restart,population,source manifest,terminal jobs and artifacts.

Source/deployed0.4.0 JARs retain their prior hash.0.4.1-dev is unreleased; no server
deployment,restart or service change. Full-pack warnings remain. See
[the production rules](../../../design/floor-centering.md).

## Exact process custody

All four chains were sampled live,then exited normally with world save and the
known idle Animatica executor drain. Metadata is in each custody/results pair:

- Initial873285/14112751,873387/14112816,873557/14113452.
- Inspection875315/14128627,875427/14128698,875556/14129033.
- Centering877266/14163747,877370/14163811,877490/14164259.
- Persistence878977/14186394,879080/14186468,879197/14186792.

Historical adaptive601856/601946/602326 remain absent,no cause/replacement inferred.
Frozen601857/601990/602372 retain exact identities at latest sample. No stop,pause
or signal to those clients; other user clients/worlds/services preserved. No active
new transient job,pending command,new worker,automated build/release timer,hold,
model grant or inherited exception. Diagnostic path queries only run for idle,
awake,nontrading residents; they may warm native navigation caches but never
assign the resulting paths.

Next: finite protected mangrove propagule/log/planting jobs. Mobility restoration
is not ownership permission,resource reachability,loot income or renewable growth.
