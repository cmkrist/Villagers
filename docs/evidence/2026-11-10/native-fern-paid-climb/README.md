# Fern's paid climb to a real meal

The copied all-resident food runs exposed Fern gathering wheat below a remembered
workbench without completing the climb. These changes address that physical route;
they do not demonstrate that a villager has completed Minecraft.

`NATIVE-CLIMB-RECEIPTS.json` records the terminal outcomes, saved starting state,
native block work and exact production JAR hashes. Server exit 0 is not a food
pass: the health guard stopped several otherwise orderly runs.

The passage planner now divides its existing visit budget between the original
search and a more directed retry. The excavation limit remains 64 blocks. An
actor who already owns wheat prioritizes the finite passage to a real workbench
instead of repeatedly gathering more grain or restarting unrelated table routes.

Native block contact uses an actually visible face. If a short adjacent sand
column must be cleared, the worker can cut its base from beside the column and
wait for native falling blocks to settle. The plan accounts for every original
falling block; it excludes standing beneath the column, overlapping bodies,
other nearby entities, unsupported bases, protected blocks and columns over four
falling blocks. Native hardness, tool requirements, drops and wear still apply.

Urgent food work can make one bounded native path search to a cleared next step
every 40 ticks. It stays within four blocks, limits search radius and path length
to 16, and checks dry terrain and the actor's home bounds. Larger searches retain
their shared planning budget.

Verified observations:

- `native-fern-directed-passage-frozen-v3`: found a 32-dig, 33-step route within
  the original search budget. Frozen inspection only; no movement claim.
- `native-fern-directed-passage-cycle-v4`: stopped at 8 health after 14 digs;
  an overhang blocked the upper block contact.
- `native-fern-side-sand-contact-v7`: broke the real adjacent sandstone base,
  waited for falling sand, then collected two native sand drops from that cell.
  The 240-tick diagnostic did not produce a meal.
- `native-fern-side-sand-cycle-v8`: performed 30 digs but stopped at 8 health
  while waiting between cleared steps.
- `native-fern-cleared-step-recovery-v9`: the already-starving actor climbed
  from Y94 to Y99 and cleared three sand blocks in 321 ticks. The unchanged
  4-health stop triggered before a meal; this is movement evidence, not recovery.
- `native-fern-cleared-step-cycle-v10`: mistakenly selected a damaged frozen
  checkpoint. The 8-health guard stopped after one tick. Excluded from care claims.

The next full-cycle test uses the independently audited, stopped
`native-earned-coal-prefuel-v12` checkpoint: 179 original humanoids, 33 managed
villagers all at 20 health; Fern starts at 20 health and 16 food. All copied runs
preserve their source files. No test supplies food, moves bodies, edits hunger,
restores health, or changes crop growth.

`native-fern-cleared-step-cycle-v11` completed the full 24,000-tick window.
Fern physically cleared 33 blocks, reached the remembered workbench, crafted
three bread from nine owned wheat, ate them, and finished at 20 health/20 food
with nine wheat remaining. The first craft appears at tick 16,360. Saved-body
audit retained all 179 source identities, no new bodies, and all 33 managed
villagers alive. The final probe snapshot reported 20 health for all 33; the
actual saved-body audit found one captive at 19 health after shutdown, with all
others at 20. `FULL-POPULATION-CYCLE.json` records both observations.
This is one food cycle, not indefinite self-sufficiency: several captive
residents finish with only one or two food points and require further care.

The enabled camp-meal continuation, `native-camp-after-fern-cycle-v1`, stopped
at tick 1,360 when already-depleted captive Dara-573a reached 4 health. Juniper
actually delivered one bread to Willow-4ed3 at distance 2.8485 through a real
lever/gate operation, restored the lever and closed the gate. This is one
transfer, not a sustainable-care pass. All 179 source bodies remain; the probe
adopted four existing native residents (37 managed total), with no births or
new identities. `CAMP-CONTINUATION.json` records the failed care outcome.
