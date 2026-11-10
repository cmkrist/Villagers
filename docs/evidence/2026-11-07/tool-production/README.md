# Real wooden-hoe production — unreleased0.4.1-dev

Dara autonomously made and equipped one wooden hoe from one real held oak log,
using the server's loaded plank, stick and hoe recipes at an existing table. The
seed and two spare sticks were retained. Clean restart preserved the tool and
materials without another recipe. This addresses tool production; the log was an
explicit operator supply, not autonomous forestry or renewable resource income.

## Inputs and controls

`prepare-copy.py` copied the stopped construction fixture exactly (202files) into
`runs/tool-production/play`. No villager, inventory, terrain, hunger, age or mind
setters during preparation. Previous fixture bread/hoe/table/nursery remain labelled
background. Source construction world stayed byte-identical to setup's full manifest.
Default play/family/household worlds were not replaced or merged with this copy.

`commands.txt` explicitly sets daylight, teleports only the observer into ordinary
handoff range, supplies1oak log+1wheat seed to Dara, then moves the observer away
and arms read-only observation. `WoodProductionProbe` labels these inputs and uses
the actual sneak-right-click gift code. It invokes no recipe, actor movement,
decision or work action. Dara's four held bread and empty hand were recorded before
the gift; both actual handoffs and resulting inventory are archived in supply JSON.
The existing operator-supplied table at(-226,71,744) was used, not constructed.

## Actual native sequence

The first handle20814/exit0 paused at `PauseScreen` at server tick67/game40209.
No completed production claimed. Its observation, source pins, process custody,
commands, normal save and exit are retained in `paused-attempt/`. The same world
was reopened without resetting or supplying anything again. A bounded-play-only,
explicit `resume` diagnostic command was added for future paused observations;
the successful reopen never needed it. Its positive paused-menu path remains
unverified. No other client's menu or desktop focus was changed.

Handle12719/exit0 ran1600ticks40210→41810/final save41814. The native status at41082
shows three wood recipes,one hoe,one seed,two sticks,four bread. Dara had native
age1364 (adult birth cooldown),selfFoodGoal=false,familyFoodGoal=false: production
did not depend on reproduction or a food-stock deficit. Final native equipment is
wooden_hoe,count1,damage0/59. One bread was eaten normally later,leaving3bread and
food20; it was not a recipe input. Inventory has no remaining log/planks.

Read-only persistence handle47797/exit0 ran400ticks41815→42215/final save42219.
The same hoe remains equipped at damage0/59,one seed/two sticks/three bread remain,
and this load's wood/hoe counters are0. No extra supply,clock,terrain or actor inputs.
Slot compaction on native inventory reload changes empty-slot positions; counts
and equipment persist. `verification.json` asserts these facts against retained
native reports,source pins,manifest and artifacts.

Both runs retain5live villagers (4original+the earlier child),7historical deaths,
0missing originals. Mean tick samples3.115093ms and4.76512ms at20TPS/five actors
are not population capacity or tail-latency evidence. `native-village.png` was
viewed and is a village overview; the tool claim rests on native inventory/equipment
and recipe counters, not an asserted close-up animation.

## Source and tests

`WoodCrafting` prepares detached exact grids, uses live server recipes, rejects
wrong result/count/byproducts and commits only unchanged inventory with full output
capacity. The next step needs enough held wood for the final hoe; insufficient
planks are not wasted on unusable intermediate sticks. Partial recipe success
retains real intermediate products if a later step fails. Current supported wood
families and limits are in [the design](../../../design/tool-production.md).

`ToolProduction` requires an adult needing a hoe and holding planting material,
safe daylight/body state,real loaded table,reach/sight/home checks,20workticks/recipe,
bounded400tick continuous trip,existing shared path admission and backoffs. It
interrupts elective learning and keeps survival/player/food priorities. No new
schema,thread,recurring timer or per-poll IO. Material gifts consume actual items.
Generic terrain break/place permissions remain false; production edits no blocks.

Build97403,final96062 and resume82511 exited0; latest suite244Java+10Python tests,
zero failures/errors/skips. Seven new regressions cover a full conserved chain,
mixed/split material stacks,insufficient inputs,stale/replayed plans,unexpected
outputs,capacity/free slots,retained intermediates and narrow permissions. The native
chain validates actual data-pack recipes/components and equipment. Source/deployed
0.4.0 artifact pin remains unchanged; no release or server restart.

## Custody

First chain836085/13737603,836194/13737737,836342/13738291 exited normally. Reopen
838248/13761855,838350/13761919,838480/13762323 and persistence839843/13776552,
839944/13776612,840087/13776886 were sampled live,exact tuples retained,and terminal.
The original capture helper refused to overwrite custody-start.json during reopen;
the distinct current chain was then recorded as custody-reopen.json,which the
collector uses. No stale tuples were attributed to the reopened process.

Historical adaptive601856/601946/602326 remain absent; no stop/pause/signal from
this lead and no replacement inferred. Frozen601857/601990/602372 retain their exact
identities at latest native metadata sample. All remaining user clients/services
preserved. No owned active transient job,automated build/release timer,new worker,
coordination hold,model grant or inherited PID exception.

Next dependencies: protected wood acquisition and station crafting/placement,real
reserve refill and sustainable farm capacity,parent care,rest fallback and scale
acceptance. A supplied-log success is not a self-sustaining village.
