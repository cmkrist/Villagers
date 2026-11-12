# Earned native replacement-tool loop

Clover's copied native world began with one stone pickaxe at damage123, three
cobblestone, a wooden pickaxe, a hoe and food. There were no sticks or logs.

Actual gameplay opened a village fence gate beside an observed tree. A frozen
inspection then found zero valid standing spaces: low nonpersistent leaves
filled the headroom. The next normal-AI run cleared three actual leaf blocks,
harvested one native oak log, and committed three native recipes. The saved body
now owns a new undamaged stone pickaxe, retains the old damage123 pickaxe, and
carries the original wooden pickaxe (damage3) in the offhand. Two planks and two
sticks remain; the three cobblestone were consumed. Four clay balls were put in
an actual world item to make crafting space. All179saved humanoid UUIDs remain,
and all33saved managed villagers end20HP; only the grassland cohort was actively
simulated by this focused test.

`COPIED-NATIVE-REPLACEMENT.json` preserves stopped-world native before/after
inventories and the actual transfer/harvest/crafting counters. The run was
stopped early, at13667ticks, to diagnose its subsequent return-to-mine stall;
this is not full-cycle food or iron evidence. See its exact elapsed field.

Latest source routes a returning adventurer through its actual remembered mine
entrance before asking for a deep working-floor path. It also implements paid
village bedding from actual public stores and native recipes. Bedding now uses
funded family readiness; it does not require the unrelated hungry-family flag.
No native bed placement or primary population growth has been verified yet.

`native-restocked-mine-entrance-v10` reached the deep working floor but then
waited114failed scans: its100tick planning retry exceeded an80tick queue lease.
A10tick retry and competing-worker regression now cover that failure.
`native-fair-mine-frontiers-v11` is testing the fix from the healthy stopped
continuation.
The actual primary remains205346, without promotion. Camp care, earned iron,
Nether, End and a real villager dragon victory remain incomplete.

The fair-junction trial subsequently completed24kticks with Clover20HP/20food,
two actual backtracks,32mined blocks, and a real coal drop. Its native mining
receipt is in `COPIED-NATIVE-COAL.json`. No iron was obtained. The new continuation
stores actual coal in the owned furnace to free a bag slot, counts that paid
fuel against future needs, and abandons stale surface targets. That storage
change is under gameplay test in `native-earned-coal-prefuel-v12`.
