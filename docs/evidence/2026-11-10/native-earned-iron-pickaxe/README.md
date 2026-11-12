# Earned iron pickaxe in the actual primary world

`native-primary-paced-iron-v6` now proves the milestone in the actual primary:
normal exit0, milestone at253831, saved native time253965. Saved Clover owns
one undamaged iron pickaxe, retains the stone pick (damage70) and wooden backup
(damage4), and has20HP. All179baseline humanoid IDs remain, no new bodies;
all33saved managed villagers20HP. Only8grassland residents actively simulated.
The original authored save hashes are unchanged; no copied world was promoted.
`PRIMARY-EARNED-IRON-PICKAXE.json` records the actual process/JAR, final inventory,
identity audit and native furnace/wood receipts. Native rate80TPS was requested
and observed during this primary development run. Faction growth and dragon
victory remain unproved.

The following copied trials established and tested the resource chain first:


`native-earned-coal-prefuel-v12` ended normally at the actual iron-pickaxe
milestone:9769ticks,gameTime361524. Clover physically mined raw iron, deposited
owned coal, loaded the placed furnace, collected three native-smelted ingots,
and used three ingots plus two sticks in Minecraft's actual pickaxe recipe.
Stopped-world NBT confirms an undamaged iron pickaxe, the retained stone pick
(damage36), and the original wooden backup (damage3) in the offhand. Clover
ended20HP/20food. All179saved humanoid identities remain; all33saved managed
villagers are20HP. Only the grassland cohort was actively simulated in this test.

`COPIED-EARNED-IRON-PICKAXE.json` preserves exact runtime, artifact hashes,
raw inventories/equipment and ore/furnace receipts. This world was not promoted.
It does not prove population growth, Nether/End access or a dragon victory.

The first updated PRIMARY run subsequently completed12000normal20TPS ticks,
205346→217347:all179baseline bodies retained, no new bodies, all33saved managed
villagers20HP, original authored save unchanged. It had not obtained iron yet.
Its result is `runs/native-primary-earned-iron-loop-v2/result.json`.
A further primary daylight window is running, alongside a copied trial from
an immutable stopped217347snapshot. The copied trial cannot mutate primary.

New source also implements paid shears and native sheep shearing for renewable
bed wool, and routes boat preparation through the shared real wood survey,
gate and canopy logic. Those new growth/boat integrations still need gameplay
verification; neither is counted as a demonstrated birth or completed game.
