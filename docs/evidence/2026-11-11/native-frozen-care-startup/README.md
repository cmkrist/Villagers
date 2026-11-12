# Frozen startup and bounded native care

The active goal remains a villager completing Minecraft. None of these copied
worlds establishes game completion, population growth, or indefinite food recovery.

- v266: save/reopen continuation of the v265 care copy stopped after 1,312 ticks,
  at gameTime 309820, before Birch-764e’s next lethal starvation interval. All 192
  original bodies remain alive. The courier physically opened a lower fence hatch
  and fed the neighboring captive, but Birch received no meal. Repeat care fails.
- v267: fresh copy of the real, unadopted main checkpoint. Startup froze at
  gameTime 304508, verified Pip’s saved position and health and the exact reviewed
  worker body, then enrolled that existing worker without changing its physical
  body or stock. Explicit resume followed. Passed 4,000 native ticks at observed
  **20 TPS** (the startup controller reset the requested 80 TPS), gameTime 308508.
  All 192 original bodies remain alive. This is a bounded initial-care proof.
- v268: deliberately wrong worker-health signature. Rejected at gameTime 304508
  before enrollment, with empty adoption receipt. All 192 saved physical bodies,
  tags, inventory and food are identical before/after. Normal shutdown exit 0;
  the rehearsal wrapper expects a successful observation and therefore rejects
  this deliberately failed preflight. No survival window is claimed.

The immutable v267 snapshot passed 620 Java tests, zero failures. All 409 client
classes compiled separately from it were byte-identical to its tested JAR. The
observer now supports that snapshot and verifies the compiled classes before
client startup. Its worker proof enables explicit frozen enrollment and resume.

v269 is the completed **actual main-world** observation. Normal exit 0, no wall
or combat stop; source snapshot and original FourBiomesWar save unchanged.
Game time 304508 → 308509 (4,001 elapsed native ticks; nominal budget 4,000).
All 192 original bodies alive, no new, missing or newly dead UUIDs. The worker
history now contains exactly two additional workers for the camp, ever; this is
activation of one existing body, not population growth. Courier recorded 10 real
food deliveries and 6 native bread recipes from gathered supplies. Pip and Dara
end at 1 HP / 7 food each; Oak at 2 HP / 6 food. They retain their planting roots.
Birch-764e ends at 17 HP / 0 food and Rowan-8889 at 8 HP / 0 food in another cage.
Those two remain a repeat-care risk; there is no whole-camp healing claim.
Clover retains his earned iron pickaxe, 20 HP / 20 food, no earned armor/diamonds.
This actual session used the ordinary client loaded area (simulation distance12),
not the copied probe’s persistent all-resident chunk forcing. All 192 saved bodies
were audited before/after; global simultaneous simulation is not claimed.
No copied save is promoted into the main world.
