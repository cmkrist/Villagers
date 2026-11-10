# Efficient meals and useful forage affordances

## Behavior

The survival reflex already waited until a healthy body's missing food could use
a ration's full nutrition. Elective forage still bypassed that rule and could
eat bread at19food. FoodInventory now applies the same actual FOOD component and
MealPolicy to owned meals, loose-food selection and consumption after crafting or
harvest. Injury below18food and urgent hunger at6 or lower retain their exceptions;
full20 still cannot eat. Hunger rates, nutrition, saturation and real item costs
are unchanged. Farmer inventory retains its last carrot/potato planting item.

An oversized first slot no longer hides a fitting later slot: a one-point beetroot
can be selected without wasting an earlier bread. Selection reads the small native
inventory without mutation or detached snapshots. Actual post-craft/post-harvest
items are checked before consumption, including custom nutrition components.

Observation.foodAvailable now describes food the body can currently use, rather
than any stocked ration. A pending incomplete forage attempt whose affordance
disappears is abandoned without a failed-skill update, and the actor chooses among
its other allowed actions. It cannot override a false affordance with a learned
forage preference. An owned future ration suppresses elective raw collection;
explicit household stocking remains a separate authored goal. Fitting loose food
can still be used. Existing cooldown and no-authored-meal-learning rules remain.

Raw grain/crop work checks the predicted default food result; the cached nearest
crop is not replaced by a new global optimal-crop search. A farther fitting crop
may therefore wait. The actual result is checked again before eating, so an unusual
recipe/drop component can leave a future ration in inventory rather than cause an
oversized meal. This is not an optimal dietary/recipe planner or a throughput proof.

Household provisioning now permits food14..20, the existing breeding-body floor,
instead of requiring perfect20. Otherwise efficient waiting at19 could suspend
production indefinitely. Adult/healthy/rested/daytime/dry/awake/mood/safety gates,
real stock goals, physical journeys and native recipe/crop transactions remain.
Below14 work is suspended, and the existing meal reflex precedes work arbitration.
This does not supply food, relax birth cost or guarantee a sustainable work cycle.

No extra world scan, path budget, worker or timer was introduced. Existing cached
crop/table searches and bounded entity queries remain. Execution does not repeat
the full observation query just to recheck forage; it validates each actual source.
Default yield checks create small default item stacks; no population allocation or
tail-latency benchmark has been performed.

## Verification

Initial build56619 and final12383 exited0.215Java+10Python tests pass, zero failures,
errors or skips. [Final build](build-final.log). Six new actual ItemStack/container
tests cover reserve retention, fitting later slots, planting roots, injury/fullness,
nondefault nutrition and missing/unsupported food components. Test food components
are explicitly bound fixtures, not a running datapack load. Existing third-party
JOML Unsafe runtime warnings remain; no compiler warnings.

Native run95381 exited0 with unchanged source during the run. It used a fresh
independent `runs/efficient-meal-probe/play` copy, not either older fixture or the
original play world. All168 baseline world files matched before intervention.
Only two copied minds changed: Fern food15 and Nell food19, both saturation/
exhaustion/timer0 plus redundant hunger. Nell's copied learned FORAGE value was
set1 in every context to challenge affordance selection. No native health,
inventory, position, terrain, time or weather was set. [Exact setup](setup.json),
[preparation script](prepare-copy.py), [source hashes](source-before.json).

At the native boundary, Nell retained all6bread at19food/health20, actionEXPLORE.
Its FORAGE values remained1 in all contexts; no bread meal note or forage-learning
update occurred. Other learning progressed342→344, rather than repeatedly trying
to eat. Nell remains on the same canyon ledge: no escape or successful exploration
is claimed. Fern used one of its existing2bread, food15→20/saturation6/health20,
retained1bread and recorded one authored meal. Learning progressed499→502.
No bread was crafted or crop harvested in this short run; no recipe/harvest/native
injury or food14..19 work acceptance is claimed from these two cases.

The400tick observer requested normal quit33105→33505; final save33508 includes
three shutdown ticks.4alive/7dead/0missing unchanged in the copy. The original
world's complete file manifest still matches, including Fern2bread/food20 and
Nell6bread/food20. Fixture history must never merge back or count as ordinary
society survival. [Results](results.json), [native snapshot](budget-native.json),
[saved census](census.json), [launch](launch.log). Actual screenshot inspected:
[rendered village overview](native-village.png); not close-up consumption proof.

Native mean tick3.913932ms at20TPS/four survivors is a small snapshot only. Worlds
saved, exact idle Animatica four-worker executor drained and terminated. Observed
wrapper781945/start12925403, daemon782047/start12925462 and
game782208/start12925974 all absent at terminal checkpoint; original six exact
identities remain unchanged. Full UID/exe/cwd/cgroup custody in results/custody-start.
No active new client, build, worker, automated build/release timer, hold, hash/model
grant or inherited PID exception remains. Resourcepack/model warnings persist.

## Next society acceptance

Native food-sharing/understood refusal and ordinary birth remain open. Before
another birth attempt, inspect accessible potential parents' real readiness and
route rather than assuming Nell can leave the canyon. Juniper is near home, Fern
near the farm, and Dara needs real food/regeneration from12health. Any supplied
fixture food must be labelled and kept separate from autonomous production claims.
Verify actual parent inventory costs, local courtship, spare native home, child
lineage/inheritance and save/reopen. Continue measuring farm capacity/access and
conserved tool/station/field provision; seven local plots are not a proven surplus.
