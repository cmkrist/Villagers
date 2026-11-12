# Native shared navigation, paid water seals and primary deployment

Goal remains active. No verified native births, armor, diamonds, Nether/End entry or dragon victory.

The production changes preserve native movement, gravity, collision, health, hunger, real block contact, inventory costs, recipes, loot and tool damage. No NPC setters, supplied stock, replacement founders, save restoration or copy promotion were used.

## Verified copied-world behavior

- v71/v73 frozen checks exercise native path cache bypass and navigation metadata preservation for both camp founders. Planning can inspect a fresh route without redirecting active movement; committing a chosen route sets its actual target. Common camp housing, supply, call and handoff paths share occupied-body checks.
- v74 stopped at 20,560 ticks with all 179 original identities retained and 18 actual food gifts. Fern-9bcb had 12 food, but a native husk inflicted 7 and 5 spear damage, followed by 7 fall damage during escape. This is a separate threat/route safety failure. The failed save is retained.
- v76 inspected all 199 known mine footings without advancing time: 25 fresh solid-supported faces could become mineable after sealing water, but only one placement preview had supported, dry, reachable contact. No survey coordinates were taught to the miner.
- v77 mined 18 actual blocks but placed no seals: retirement discarded entry knowledge. Production now retains previously recorded entrances and revisits unfinished known work through ordinary paths.
- v78 ran 16,000 native ticks, preserved all 179 identities, mined 18 blocks and placed one real paid seal. WATER at (-482,62,-380) became DIRT; carried dirt fell 64 to 63. Reach was 2.993789762354466 blocks from supported dry feet. Clover ended at 20 health/20 food with two ingots and no armor. Retained footings increased 174 to 213; reservations are included, so this is not a count of physically walked floors. Lowest known floor remains 54.

`v78-paid-seal-and-mining.json` contains the actual action receipt. `v74-fern-threatening-husk-and-fall.json` contains the native damage events.

## Actual primary deployment

v79 ran the actual NativeFourRegions save for 1,000 ticks at requested and observed 80 TPS. It stopped normally at game time 254966. All 179 original humanoid identities remain; 33 managed villagers ended at 20 health and none had zero food. Clover retained the undamaged earned iron pickaxe and 19 food. An explicit native command enrolled two existing locally resource-ready illagers, supplying zero items and creating zero founders.

Eight additional unmanaged adult villagers appeared in the saved-world audit, bringing the saved count to 187. They are not verified births and are not counted as family growth. All original identities and the old authored FourBiomesWar save were preserved. No rehearsal state was promoted.

Camp meals and camp passages are now enabled on the primary; the prior config and new config are recorded here. Cell escape stays disabled. Source hashes before launch and actual compiled development-class hashes after exit are recorded in the full run folder; the previous tool incorrectly described a separate built JAR as the actual client code source, which is now corrected. Latest tested build: 530 Java tests, zero failures/errors, recorded JAR SHA in deployment-preflight.json. Native 80 TPS is pacing, not a learning-speed measurement.

v80 completed 10,000 guarded ticks and saved at game time 264967. All 187 identities survived, with no new identities. Native adoption expanded management to 56 existing villagers, all at 20 health and nonzero food. These adoptions are not births. Both existing illager founders had 24 health/20 food and six bread each including held bread.

v81 completed 6,000 more guarded ticks and saved at game time 270968. All 187 identities survived, with no new identities; all 56 managed villagers remained at 20 health and nonzero food. Lowest food: Tansy-e028 5, Linden-8ea7 6, Moss-fad4 and Ember-c73f 7. Care remains incomplete.

Clover actually mined 12 blocks using the earned iron pickaxe (damage0 to12), ended with one raw iron, and placed one dirt foundation at (-469,55,-381). The receipt records actual supported contact, reach2.8303420629691995 and carried dirt33 to32. This is the first verified paid mine foundation, distinct from v78's copied-world water seal. Primary placed no water seals in v81. Clover ended at 20 health/20 food; minimum recorded floor remains54, no armor.

A read-only report during v81 records Juniper-e9b7 delivering two real bread gifts to captives. The last receipt records closed cell gates, restored lever state, recipient stock0 to1, donor4 to3 and native contact2.894338 blocks. Pip-460a had zero gifts at that observation. These are actual primary actions, not copied results. Both families still had zero birth attempts at that report.

All three primary runs are terminal with normal exits; no Minecraft processes remain. Source hashes were unchanged during each run and old authored-save hashes matched. The actual save is retained at270968, with no rollback or rehearsal promotion.

Full runs: `runs/native-common-route-cache-frozen-v71`, `runs/native-scoped-route-metadata-frozen-v73`, `runs/native-committed-route-full-care-v74`, `runs/native-all-junction-seal-survey-v76`, `runs/native-paid-water-seal-mining-v77`, `runs/native-remembered-entry-water-mining-v78`, `runs/native-primary-camps-and-paid-seals-v79`, and `runs/native-primary-camp-dawn-v80`, and `runs/native-primary-dawn-work-v81`.
