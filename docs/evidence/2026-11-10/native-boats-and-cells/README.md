# Native boat returns and captive food access

All initial observations here used copies; none were promoted over the primary.
The separate authored save remained pinned and unchanged. No body, health, food,
item, time or loot-table grants were used.

`return-before.json`: Birch resumed the actual saved ASHORE trip, but failed to
plan a return for 2400 ticks. `return-after.json`: directed native water-cell
search found the home bank; the same actual boat used 870 native paddle ticks,
and Birch landed at (-511.58,63,-325.51), game time60891. All eight loaded island
villagers remained healthy. Birch retained the original one bread, three planks
and two sticks; the tool's stage `returned_with_food` does NOT prove a new food
harvest. This is proof of a native physical return, not resource production.

`cell-interrupted.json`: hungry Tansy reached an iron door but pantry work kept
interrupting the break. `cell-escape-and-attack.json`: prioritizing an active job
allowed actual native destruction at78738 after150ticks with the empty hand,
followed by three physically reached meal withdrawals and eating. Tansy was
18HP/food4 at79017, but the open cell also exposed
other villagers to native attackers. Ember reached4HP; the health guard stopped
the copy after1240ticks. This is a FAILED safety rehearsal, not sustainable food
access. The experimental nativeCellEscape config now defaults false, including
for old configs, and diagnostic use requires explicit --cell-escape.

All illager housing attempts still lack proof of a collected/placed bed or child.
The game-completion goal remains active.

The primary subsequently applied the verified return fix in an1800tick guarded
normal20TPS run,60017→61818. It exited normally, retained all179 original native
humanoids, and all33managed villagers ended at20HP. Birch is home with no sea-trip
tag; the original owned boat is retained at shore. Sage completed natural recovery
to20HP/food20. The experimental cell-escape setting stayed disabled. Primary files
are included with `primary-` prefixes. New resource-voyage code was added only
after this primary run and remains under copied-world testing.
