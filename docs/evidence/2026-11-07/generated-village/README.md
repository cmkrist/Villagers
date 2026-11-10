# Generated-village and adventure capability iteration

Recorded November7,2026. Minecraft26.3, Fabric0.19.5/API0.162.0+26.3, the pinned Vanilla Perfected pack, JDK25, visible client throughout. The normal world seed is846206; the generated village is a native desert village centered around x=-240,z=768. Its structure templates/bounding boxes are recorded in baseline.json.

## Natural fixture

The frozen natural cohort contains **11 existing villagers and one native iron golem**. The resource scan found11 bed heads,15 wooden doors,8 farm blocks/crops and normal generated chests/workstations/hay. No actor, building, food or gear was spawned by baseline setup. UUIDs, positions, inventories, professions and existing home memories were retained. The golem keeps vanilla AI. Its identity is audited; it is not yet a personalized learning guardian.

The failed flat VillagerLab and its surviving identities were saved, closed normally and archived under ignored run/archives/. A complete natural village save before tool testing is also preserved there. The original human Modrinth world/profile and Buddy services were untouched. The development observer is creative/flying; villagers receive no invulnerability or player gamemode. Normal hostile-spawning eligibility remains with the usual observer-distance exclusion.

## Implemented capability slice

Real sword/hoe handoffs through sneak interaction; real inventory/equipment conservation; native defensive sword melee constrained by hostility, reach, sight, cooldown and durability. Doors open along reachable nearby paths and close after passage, including doors already open when adopted. Player food/planting gifts consume real inventory items.

Harvest/replant is a narrow exception: mature wheat/carrot/potato/beetroot on intact farmland, a possessed hoe, a corresponding existing planting item, tool reach and sight. The same crop is replanted at age0 with native loot and hoe wear. General BREAK_BLOCK and PLACE_BLOCK stay disabled. Farming/gift code preserves the last carrot or potato as planting material when equipped for farming. Wheat is collected but bread crafting is not implemented. This is a mob capability model, not an actual player GameType or complete crafting/food-physiology simulation.

Authored safety chooses covered reachable homes near dusk, home-range return, local golem protection when a house is unavailable, and checked escape paths. Home proximity alone does not count as shelter; roof and a valid unobstructed bed interaction are required. Cached missing-bed plans are invalidated. These capabilities are separate from learning.

## Evidence and limits

The final full build passed **21 unit tests**. Live game audits recorded actual door opening and closure. A recorded clock step to11800 exercised dusk behavior: five originals were observed sleeping inside generated houses, other actors were returning or seeking golem protection, and all11 originals plus the golem remained present in the sampled window. This is a partial shelter smoke test, **not** all-residents-protected acceptance or a multi-night survival pass. Some home/path assignments still need investigation.

The explicit harvest probe found no eligible resident/ripe-crop pair within its local reach in the attempted samples, and correctly changed no world/items. **Positive live harvesting/replanting, player handoff, native sword damage/durability and multi-night survival remain pending.** Their code and pure policy tests exist; compilation or a rejection is not a positive gameplay result. No artificial crop growth or manufactured eligible actor was used to obtain a pass.

Baseline.json and the clean saved-world backup keep the natural-supplies starting point. Any later successful testharvest creates an explicitly logged operator hoe/planting-item intervention, distinct from the natural baseline. The current marker has no successful resource handoff from the rejected probes. The visible generated-village trial remains running for inspection and further natural observations.

Current Java custody tuples are in owned-jobs.json. They are new development jobs; old Niko hash-only exceptions do not transfer. No model/hash execution or exclusive clearance is authorized here.
