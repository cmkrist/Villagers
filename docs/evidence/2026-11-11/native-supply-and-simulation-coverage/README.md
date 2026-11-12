# Recurring food, live coverage and a conserved emergency rescue

The goal remains active. Native births, earned armor, diamonds, Nether/End entry and dragon victory remain unverified.

## Authoritative result

The actual NativeFourRegions save is stopped normally at game time282171.
All187 saved humanoids are alive, including every original179. No new identities
were added in these primary runs. All56 managed villagers have nonzero food.
Several remain injured; minimum managed health is6. No health setter or rollback
was used. They need additional real food for natural healing.

Actual v106 completed1,000 ticks with nine real bread handoffs: Pip-460a four,
Juniper-e9b7 five. Their final health is24 each, food19/20. The four residents who
were starving before the rescue now have food15,10,10 and5. Last receipts record
actual donor stock loss, recipient gain, visible hand contact under3 blocks,
closed/restored gates where applicable, and emergency-reserve use. One copied
receipt also proves offhand stock1→0 with matching recipient0→1.

The server reported simulation distance16 and native80TPS. Every game tick ran.
80TPS is pacing, not a learning-speed claim. Source hashes stayed unchanged in
v106; the authored FourBiomesWar save hashes matched. Runtime development-source
and class provenance is recorded in the full primary run directory.

## What changed and why

- Empty harvested hay coordinates had no valid block-interaction stands. A
  worker can now walk to a safe point near its own recorded harvesting place,
  then use the normal local visible scan. Actual harvesting/contact, recipes,
  inventory costs, drops and tool wear remain unchanged. Frozen v93 proved one
  safe area route where the old interaction route was absent. Five other
  remembered places still had no admitted route in that bounded query window.
- A carried bed with no placement site previously ended the supply routine
  before food collection. Food work can now proceed when bed placement returns
  no job. Supply exploration uses the tested body-aware, bounded camp routing.
  Actual v97 crafted three loaves and placed an additional owned bed, but did
  not deliver food during that window.
- Ember-8a41's remembered beetroot patch was reachable by a32-node work route,
  while the ordinary follow-range search returned an incomplete31-node path.
  Native farm recall now uses the bounded work route. Long-term recovery for
  Ember remains unverified because the live trial stopped for another resident.
- Juniper ticked only570 of v97's4,000 ticks after wandering beyond the active
  simulation radius. A file-only radius change was reset by the client: v100
  still ran at12. The observer launcher now applies an explicit client option
  before world open, bounds it by existing render distance, records the actual
  server value and fails verification if they differ. v101/v102/v106 verified16.
  The observer returned to the previously used camp center. This covers the
  camp and mine; it does not establish that every distant region is ticking.
- Hungry voices repeat every200 ticks; food≤2 produces a louder native alarm
  every100 ticks. Sound range remains bounded by the actual native sound and
  the listener's hearing. Louder heard alarms attract attention, while only
  nearby understood words queue a meal. Existing urgent handoffs finish before
  another uninterpreted voice can interrupt them.
- Healthy workers with at least18 food can respond to an understood emergency
  using reserve or held food. Ordinary donations retain their previous reserve.
  Receipts include bag/offhand source and total owned item counts. This moves
  existing items; no food is created.
- Urgent carers can use the existing native running speed1.1 while sprint-capable.
  Extra exhaustion is charged from actual displacement on that running route.
  No movement attributes, positions, velocities or collision rules are changed.
- Camp handoffs now consistently use actual visible eye-to-body contact within
  three blocks. The old extra foot-to-foot check rejected legitimate contact
  beside raised floors. Gate/feeding-window planning and receipts use the same
  physical reach. No transfer bypasses a closed block ray or extends hand reach.

## Trials and limits

v91 mined24 blocks before native night rest. Its copied continuation v95 mined
150 more blocks on the next day and retained two raw iron, but no armor. These
are copied results, not actual primary stock. Actual v97 mined ten blocks; the
primary still owns one raw iron and its iron pickaxe has24 damage.

v94 stopped at7,920 ticks for Birch-764e starvation; v96 stopped at13,600 for
Ember-8a41 starvation after fifteen real meals. Both retained all187 identities.
v99 stopped after2,720 for Birch-67f3. Its wrapper also flagged a source-hash
change because the actual primary was advanced in v100 while the copied audit
was still running. This is not a clean wrapper pass. An independent stopped-copy
NBT audit verified all187 identities survived. Future copied audits must use an
immutable source or finish before the authoritative source is resumed.

Actual v101 stopped at18 health and v102 at8 while the starvation/delivery
problem was being exposed. Those actual outcomes were preserved. The primary
was held stopped at8 while the rescue changes were tested. v103 rescued Pip
but stopped for Birch-67f3; v104 verified reserve/offhand giving but still stopped
for Birch. v105 then completed the exact-state1,000-tick rescue with eight
handoffs, all187 identities and all four previously starving residents fed.
Only after that proof was v106 run on the actual primary; it completed normally
with nine handoffs and nonzero food for every managed villager.

No rehearsal was promoted. Failed saves remain intact. No actor setters, free
stock, replacement adults, synthetic births, health restoration or time skipping.
Latest build:532 Java tests pass, zero failures/errors; latest-build.json records
the JAR hash. New tests cover louder alarms versus understood consent and real
hand reach beside raised floors. Full native trials verify item conservation.

Next: natural healing and recurring provisions, native crop production rather
than indefinite reliance on finite stores, safe housing/family opportunities,
then earned armor and full Nether/End progression. The short rescue pass does
not prove sustained survival.
