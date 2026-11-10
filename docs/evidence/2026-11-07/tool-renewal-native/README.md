# Paid tool replacement and resumable forestry

The native test reproduced a production interruption bug: a villager gathered
one granted log, crafted a replacement hoe, then could no longer finish its
bare-hand forestry job. One log and the retained-seed planting obligation remained.

Forestry now stores a held hoe in a real empty inventory slot before continuing
an actual valid job. Idle tool equipping defers while that job is eligible.
Actual food harvesting can still equip the stored tool. Weapons and full
inventories fail without equipment loss. Native loot/planting commits retain
their bare-hand, permission, material, visibility and protection checks.

The final frozen source build passed **345 Java tests and 10 Python checks**.
All 211 detached main class files matched the native mod JAR in each accepted
runtime. Six native phases passed:

- `replacement/`: an actual harvest broke the initially near-broken iron hoe;
  native granted seed/log gathering funded three recipes and a wooden hoe;
  farming resumed, remaining forestry and paid planting completed, then restart
  preserved items and tool wear without replay.
- `partial-resume/`: a clean save stopped after the replacement was already used,
  with one log and planting still outstanding. The same saved actor finished
  only that work on reload. A further restart showed no recipe/resource replay.
- `full-storage/`: eight occupied inventory slots prevented stowing the hoe.
  Equipment, inventory, resource cells and the finite grant stayed unchanged.

Each accepted process exited zero and its exact PID/start identity was gone at
collection. Reports, logs, identities, pinned mod/probe JARs and probe source are
in the case directories. [results.json](results.json) records source bindings,
resource checks and retained earlier failures. `source-v2.zip` and
`source-pins-v2.json` bind the final production build; current source matched at
collection. Builds used separate output directories and did not replace shared
build output or running clients. Lead construction files and HANDOFF were untouched.

This is a new supplied peaceful flat fixture, not primary history: one adult,
two mature wheat crops, eight seeds, two bread, an iron hoe with one use left,
two mangrove logs, a mature hanging propagule, planting soil, water, bed and table.
The no-space variant instead declares six bread and 384 emeralds solely to fill
storage. Native operator commands grant only the exact forestry cells.
After setup there are no supplies, body/clock/growth/position setters or fixture
terrain edits. The controller performs actual paid work at 80TPS with normal
80 base exhaustion/day.

This proves finite replacement and clean resumability. Natural tree regrowth,
repeated forestry authority, indefinite tools, sustainable food income, hostile
survival and a complete local economy remain unverified. Small-fixture tick times
do not establish population capacity.
