# Native forestry income and paid planting

This historical checkpoint binds278tests. Later [earned-station work](../earned-station/README.md)
uses its actual logs for paid table construction. Recorded forestry evidence is
unchanged;current source pins differ.

Juniper collected one wild propagule, cleared three approved leaves and one
separately approved dry root, harvested two actual logs and planted the retained
propagule. The finite job retired. Two logs, one root item and the planted block
survived restart without another cut or planting cost. **Natural tree regrowth
and a second harvest cycle remain unverified.**

Current278Java+10Python PASS. Last build build-root.log,handle18683exit0.
verification.json records50data/source assertions,not50new unit tests.
See [rules](../../../design/forestry.md).

## Native cause and implementation

The original job had zero admitted stands for both selected logs despite clearing
three leaves. New ForestryProbe uses the exact same native checks as production,
without path creation/assignment or any actor/terrain setter. It inspects100
positions/target,records first rejection counts,at most3examples/reason and a
reference planting stand. Ray checks verify all crossed chunks already loaded.
The probe remains usable when hunger correctly prevents actual resource work.

15023exit0,400ticks,cutoff52710/final52713. A257-file exact copy of stopped
runs/forestry-refresh/play at runs/forestry-access/play. Inspection52395 proves
normal STANDING eye offset1.62,height1.95. For lowerlog(-271,74,722),100candidates
failed:52feet,18head,16support,13sight,1reach. For upperlog(-271,75,722):65feet,
18head,12support,4sight,1reach. Reference stand(-269,72,723) is AIR/AIR on MUD,
within native reach,but hits dry MANGROVE_ROOTS(-270,74,722) for both logs.
The plant target already had10admitted stands. No authority/resource changed.

Source now adds explicit `/villagers site forestry root UUID x y z`: one existing
loaded dry mangrove root,operator within32,fresh authority identity,same outstanding
log quota and planting duty. Work-sites schema5 migrates older jobs with empty root
lists; leaf authority never becomes root authority. The new root is part of the
watched/protected footprint and must be cleared after seed/leaves and before logs.
Ordinary progress cannot grow/replay root permission. Native root loot must be
exactly one actual root item,not planks/logs. Body/reach/sight/home/hunger/inventory
checks and global path/work budgets remain. Pure tests cover fresh identity,
quota1,protection,replay,old-schema migration,clean persistence and root material
conservation without invented wood recipes.

## Same-history food→wood trial

runs/forestry-root/play was an exact253-file copy of stopped runs/food-help/play.
This is the world where Juniper genuinely received and ate Dara's bread; it is not
a merge of the hungry forest copy and the fed social copy. Juniper starts food10,
health20,empty inventory. Earlier supplied bread/tool/nursery history remains a
fixture input. No new food,seed,wood,tool,actor movement,health,hunger/fatigue,age,
RNG or growth setter. Existing native resource blocks were selected via commands.

One labelled `time set day` provides a daylight work fixture; it changes daytime,
not gameTime/metabolic rates. Observer teleports affect only the player. Exact
native log,three-leaf and single-root grants were added in staged command batches;
all identities/inputs are retained in command files and launch logs. Copied
experiment.json retained an inherited contradictory NO-clock sentence; the actual
daylight command was always in commands.txt/setup.json. Raw manifest is retained
and manifest-note-erratum.json identifies that note error. No silent ledger repair.

74594exit0,1600ticks,cutoff52006/final52009. Native stage counters: seed1,leaf3,
root1,log2,planted1. Inventory: actual mangrove logs2,root1,propagule0. Food10,
health20,no further donated food in this run. Last recorded work path was native,
fully reachable and home-bounded; no resident teleport or excavation outside the
finite grants. Completion removed the entire forestry job.

Raw stopped chunks prove seed(-269,74,721),the three leaves(-270,73,723),
(-269,74,723),(-270,74,723),root(-270,74,722),and exact logs(-271,74/75,722) are AIR.
Plant(-269,72,723) is an actual nonhanging/nonwaterlogged mangrove propagule,
AGE4/STAGE0,on unchanged MUD(-269,71,723),with AIR headroom. Former upperlogs
(-270,76/77,722) and seed-support leaf remain. No native tree growth was forced.

12321exit0,same-world400tickrestart,cutoff52410/final52414. Two logs+one root remain,
no seed item; every resource/plant counter0,jobstillretired. Selected terrain and
planted block match exactly. Minecraft compacted inventory slots on load; evidence
compares real counts,not slot ordering. The plant remains stage0; this proves paid
planting/persistence,not adequate growth clearance or renewable output.

No bread was crafted and no crop harvested by any actor in the full root trial.
The society's food stock remains finite; wood income does not prove food supply.

## New root revocation check

70393exit0,200ticks,cutoff52914/final52918,continues the separate hungry inspection
copy at runs/forestry-access/play. It is not the completed root-trial world.
A fresh single-root permission was added to the existing paid-seed/quota2job,then
an actual direct unchanged native root write was attempted at52728. Before=true,
after=false,blockUnchanged=true,nativeSetBlockReturned=false. The entire job was
retired and the physical root remains. Native resource counters0; no inherited
leaf-refresh exception applies to roots. This budget ended before the15second
screenshot interval; no picture is fabricated.

## Evidence and custody

source-probe.zip binds275testdiagnostic;source-main.zip binds current278root source.
All source-before hashes match their respective archives. Root trial/restart/new
revocation use identical current source. All parent source-world file manifests
remained unchanged. Four runs retained4originalsurvivors+existingchild,7old deaths,
0missing. Historical family files are retained; budget-native is authoritative.

Exact captured launch chains,all terminal:
inspection913861/14772905,913964/14772963,914126/14773464;
root917330/14824135,917431/14824192,917585/14824693;
restart919803/14845466,919905/14845524,920051/14845785;
revocation921264/14856939,921365/14856997,921496/14857397.
Original frozen3identities remain exact;historical adaptive3remain absent. No
cause/replacement inferred,no original client/world/service stopped or restarted.
No pending command,new worker,timer,resource hold,model/inference/hash grant.
Existing005/006/007 evidence remains untouched.

Five-actor mean tick snapshots: inspection3.59535ms,root2.912546ms,restart3.946176ms,
short revocation5.547355ms. These are not scale/tail-latency acceptance. Existing
pack/model/JOML warnings remain. No code release/deployment/PR/commit/service change.
Source+installed0.4.0 JAR SHA remains
4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0.

Next: use this earned wood in actual crafting/building,measure renewable food
output,and validate natural tree growth/another paid cycle before claiming a
self-sustaining economy. Broader structures,neighbor physics and growth protection,
parental care,defense and population performance remain ongoing work.
