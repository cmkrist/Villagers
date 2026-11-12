# Native mining fronts and camp rendezvous

The primary is stopped at game time205346, with all179 baseline humanoid IDs
retained, all33 saved managed villagers20HP, and the authored save unchanged.
Clover owns a stone pickaxe and placed furnace. There are no primary births or
iron ingots, and Nether/End/dragon work is incomplete.

`OBSERVATIONS.json` records the native receipts and failed outcomes:

- The old copied mine repeated57 turns over23 retained floors. Excluding already
  explored work floors made Clover leave that mine and survey new ground.
- A later soil quarry retired below the surface and forgot its entrance. The
  current change keeps the entrance and its protections until actual return.
  The v4 trial retained entrances but exposed an arrival test that could clear
  them one block below the surface. A shared height-aware arrival check and
  regression test now cover this; `native-surface-quarry-exit-v5` is testing it.
- Door alignment alone still ended in Dara starving. Matching the recipient's
  bounded wait to the guard's request lifetime, and allowing normal movement in
  a crowded cell, produced an actual bread transfer to Dara at221605, distance
  2.5337. The real lever was restored and the gate closed afterward.
- That comparison still FAILED sustainable camp care: Willow reached4HP at
  elapsed17600. Dara had13HP/15food after eating. These copies are not promoted.
- The inspected grassland actors had no free nearby beds or current nurseries.
  Actual paid bedding is required for ordinary villager population growth.

Build and tests passed:481Java and14Python tests. One overlapping local Gradle
run interfered with its own result file; a sequential rerun passed. Test success
is not evidence of sustainable populations or game completion.
