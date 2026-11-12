# Earned armor preparation after the iron pickaxe

The adventurer previously stopped resource preparation after obtaining an iron
pickaxe. `AdventureArmor` now plans the next missing iron armor piece, checks the
actual native recipe at a reachable workbench, atomically pays possessed ingots,
and transfers the crafted item into its native equipment slot. Displaced gear
uses the vacated inventory slot. Existing iron, diamond and netherite armor is
retained. No item, health or protection is granted independently of an item.

Mining and village supplies request the next piece's actual metal cost rather
than the original three-ingot pickaxe quota. Furnace fuel can be replenished for
successive real ore loads. Mineral-expedition demand also continues after the
pickaxe when armor metal is still needed. The four recipes cost 8, 7, 5 and 4
ingots, respectively. Food, recovery and ordinary navigation protections remain
ahead of adventure preparation.

`AdventureArmorTest` verifies a 24-ingot full-set transaction, preserved food,
stale-plan/wrong-output rejection and atomic failure when a full bag cannot hold
the real result. `CHECKS.json` pins source hashes and the latest test logs.

`native-earned-iron-armor-v1` completed 24,000 native ticks with the existing eight
grassland residents actively simulated. All 179 original saved identities remain,
no new bodies appeared, and all 33 saved managed villagers have 20 health.
Clover retained the earned iron pickaxe and two raw iron. **No armor was crafted
or equipped.** The report's `bounded_iron_armor_observation` stage is an incomplete
milestone, even though the bounded process has status `passed`.

The frozen `native-iron-armor-faces-frozen-v2` inspection verified all four plans
against the loaded server's native recipes using temporary in-memory diagnostic
inputs. Those inputs never entered a villager, world container or item entity.
This proves recipe compatibility, not possession or crafting in gameplay.

Its actual observed mining faces explain the shallow route: every immediately
descending direction had water where a supported floor was required. The actor
correctly preserved the aquifer; north and west still offered supported level
faces. `NATIVE-ARMOR-PREPARATION.json` retains those exact block/contact findings.
Further earned ore and native equipment proof are needed before Nether work.

Follow-up source fixes inspect and collect deposited native furnace batches
before looking for more ore, yield to real fuel gathering when an input batch
is stalled, and process carried raw iron while the owned furnace has known
fuel. Decorative andesite/diorite/granite/tuff are left as real world drops
during excavation; an existing bag stack can also be put down as a real item
when it blocks working space. Recipe stone, food, tools and planting reserves
are preserved. The additional inventory test covers that distinction.

`native-armor-fuel-and-space-v3` completed all 8,000 ticks and saved cleanly.
The two possessed raw iron became two native-smelted ingots. Clover also mined
seven actual coal, put down the obstructing 16-andesite stack as ItemEntity
`1fda2bf3-17c5-4b30-bc25-33b344481403`, and retained both actual pickaxes. Native
saved inventory and equipment confirm the two ingots, seven coal and 20 health/
20 food. All 179 source identities remain, no new bodies, and all 33 saved
managed villagers have 20 health (eight grassland residents actively simulated).
`NATIVE-FUEL-AND-INVENTORY.json` records the receipts and actual saved body.
No armor has yet been crafted: the chestplate still requires six more ingots.
