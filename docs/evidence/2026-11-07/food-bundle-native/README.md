# Food bundles and reachable paid meals

Final frozen source passed **370 Java tests and 10 Python checks**. Three native
fixtures and all three clean restarts passed against that same source, with219
detached main classes matching each loaded mod JAR. Source drift was false at
collection. [results.json](results.json) records quantities, conservation checks,
artifact hashes, exact process custody and terminal receipts. Raw accepted
observations, probe sources and JARs are in `accepted/`.

Food quotes now deliver a bounded bundle starting at five gross nutrition,
rather than charging the same price separately for every low-nutrition item.
Each item and coin is explicitly spoken and decoded. At most eight items with
identical components can be combined from seller slots. Seller edible reserves,
planting roots, buyer usable-ration checks and both inventories' full receipt
capacity remain mandatory. Component-defined stack limits are respected by
explicitly splitting detached receipts; no partial transaction commits.

The native carrot buyer paid two marked emeralds once for two marked carrots at
food13, ate one and retained one planting root. It ended at food16, with one
purchase; the seller retained29 carrots and received the same two coins. The
cycle completed at1618ticks. The beetroot buyer paid once for five marked
beetroot at food13 and ate all five, ending at food18, with one purchase; the
seller retained23 beetroot. That cycle completed at1748ticks. Both fixtures
ended at full health with seller reserves. Every-tick assertions checked
the original food/coin components and total two-coin supply. Clean restarts
retained exact inventories, components and the carrot root without trade replay.
The retained root means gross bundle nutrition is not necessarily the buyer's
immediately edible nutrition.

An earlier production regression bought the meal only at food3 and **failed**
the existing requirement to purchase above food6. The farmer had saleable bread
while the maker repeatedly tried unreachable farm work. Its world, raw failed
report and exit0 receipt remain in `retained-failure/` and the original runtime;
server exit0 is not fixture acceptance. Both bodies remained alive at full
observed health. The earlier bundle-only passes are labelled `prior-source/`.

The controller now permits an already reachable paid food exchange before
starting another provisioning trip, after owned meals, survival arbitration,
delivery and social reactions. This opportunity requires the buyer's own unmet
hunger, no usable ration, actual money, mutual sight/hearing and three-block
contact. It checks at most16 local candidates every100ticks and shares the
existing600tick quote cooldown. It performs no movement or remote stock query.
A failed offer leaves existing provisioning alone. Ordinary negotiated payment,
seller willingness, language, safety and reserves still govern each exchange.

The strengthened production fixture exercised this new contact opportunity.
An understood request led the maker to pay one real log through three recipes
for a spare hoe while retaining its original used tool and two sticks. The
farmer paid two actual coins for that hoe, used it in eleven recorded paid
harvest/replants, and crafted four bread from actual wheat. All eleven harvest
receipts balance native drops, one planting seed, inventory/overflow and one
durability. These receipts do not claim exhaustive provenance for all native
world pickups. The farmer sold one produced bread and retained three, receiving
the same two coins back. The maker bought at food10 and ate it, ending at15;
minimum observed maker food was10 and both minimum health values were20HP.
The cycle completed at8089ticks and its clean restart passed. This is an observed
successful exercise, not a matched statistical estimate of the priority change.

All fixtures are fresh, supplied, isolated peaceful chambers with selected adult
traits, common initial language, beds and light. Bundle sellers start32 marked
food items and a working hoe; buyers start a working hoe and receive exactly
two marked coins through initial real player gifts. The productive fixture has
a real fence counter, tables, water, twelve initially mature wheat, maker log/
used hoe/three bread and farmer carrots/seeds. All run80TPS with400base exhaustion
per day. After setup there is no resupply, actor/body/health/clock/growth setter,
scripted trade/meal/recipe or fixture movement correction.

`source-v2.zip`, `source-pins-v2.json` and `build-v2.log` bind final production
source. The original source/build and an intermediate failing stack-capacity
unit-test log remain separate. All owned native/build processes are terminal
and their exact PID/start identities were gone at collection. Primary worlds,
existing clients, Lead construction files and `docs/HANDOFF.md` were not edited.
Owned source is `TradeMaterials`, `MarketGoods`, `LocalTrade`, `Session` and the
controller food priority, with corresponding unit/native probes and runner.

This establishes finite bundle purchases and one productive currency cycle.
Repeated renewable income, sustainable normal/hostile survival, crash/death
accounting, calibrated prices and larger-population performance remain open.
