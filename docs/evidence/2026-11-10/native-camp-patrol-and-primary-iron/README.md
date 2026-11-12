# Primary iron pickaxe and camp food routes

The actual primary now owns its first earned iron pickaxe. This is no longer
only a copied-world result. `native-primary-paced-iron-v6` stopped after the
milestone and saved cleanly: Clover's real bag holds an undamaged iron pickaxe,
the stone pickaxe remains equipped (damage 70), and the backup wooden pickaxe
remains in the offhand. The native furnace loaded and collected three actual
ingots before crafting. All 179 original humanoid identities remain, all 33
saved managed villagers have 20 health, and the separate authored save is
unchanged. No diagnostic copy was promoted or spliced into the primary.

`PRIMARY-EARNED-IRON.json` records the actual saved body, recipe/furnace status,
milestone snapshot, terminal result, and pacing metadata. The milestone snapshot
is at game time 253831; the stopped primary's raw NBT clock is 253965.

The primary observer now accepts an explicit `--tps 20|40|80`, default 20. This
uses native tick pacing: every game tick, movement, hunger cost, crop tick and
recipe still runs. The startup play controller must initialize before the rate
is applied, or its later initialization resets the rate to 20. The observer
therefore initializes native PLAY when needed, restores the spectator observer
in the same command batch, requests the rate, and records actual observed rates.
The verified primary run observed startup at 20 and continued at 80. Neither
speed nor elapsed ticks are evidence of better learning.

Camp findings remain incomplete:

- The initial all-resident v4 copy had no enrolled provisioners. It was stopped
  and is not a complete food-cycle claim.
- The v5 and v6 full-day copies enrolled two *existing*, resource-ready native
  pillagers, without changing their health, inventories or equipment. Enrollment
  created no bodies and counts as no birth. Each run made eight actual bread
  deliveries through native lever/gate interactions.
- v5's probe completed its 24000-tick window, but the saved-body audit found one
  original unmanaged pillager missing: `3a0f306d-a099-461b-8ea9-b902f01203b5`.
  It fails the population-preservation requirement regardless of probe status.
- v6 retained all 179 original identities and all 33 managed villagers. Its saved
  minimum villager health was 19 (Rowan); several residents had little food left.
- v7 continued v6's actual saved state. It stopped at the guard when Dara-573a
  reached 7 health and no food. All 179 source identities remain. This disproves
  sustainable full-population care; the damaged copy is retained as evidence.

`FULL-POPULATION-OUTCOMES.json` retains the failed and successful observations.
The copied-world wrapper now always audits original saved identities before and
after a run, writes `population-preservation.json`, and fails if an original body
is absent. It does not restore, respawn or roll back missing bodies.

Two navigation changes address specific observed delays. A patrol can accept its
completed native stand up to three blocks from a public landmark, rather than
stall at that valid stand while requiring a two-block approach. Camp interaction
searches retain their per-call limit of eight normal searches and three larger
searches, but subsequent calls cover other valid stands. A normal search ending
at its starting node no longer permanently prevents the larger search. Caches
hold at most eight candidate sets per weakly referenced actor; all native path,
home, fluid and fall checks remain in force.

The frozen v9 inspection found three safe larger-budget paths to each western
handoff area that production search missed. The first v10 attempt only rotated
normal candidates and still failed; that failure is retained. Eastern captive
routes remain unproven even with the larger native query. These search changes
are not a claim that all camps can now feed themselves.

`CHECKS.json` pins the build, test results and changed sources. Armor, paid
housing and population growth, Nether/End progression, and a villager defeating
the dragon remain unfinished. The full goal is active.

The corrected v11 frozen test found actual safe native paths for both donors
to the western handoff stand on planning attempt four: 68 and 72 nodes. The
shared candidate-set cache then reused that valid stand for the neighboring
recipient on the first call. No actor moved during the frozen inspection.
Dara-573a's eastern cell still had no complete path after eight bounded calls.
All 179 source identities remain in the frozen saves. `FROZEN-CAMP-ROUTES.json`
records v9, the failed v10 change, and the successful limited v11 result.
