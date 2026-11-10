# Saved farm capacity and owned meals

Read-only inspection of the stopped isolated VillageTrial after the remembered-farm
run. No game launched, native ticks advanced, inventories changed or terrain edited
in this pass. The original clients/services remain running and untouched.

## Physical evidence

[Normalized block report](farm-blocks.json) retains nine selected raw chunk NBT files
and their SHA256/coordinates/data versions. The bounded volume is 33 by 33 by 9
(9,801 blocks), centered on Fern's native composter (-226,72,752). It is not a
whole-village or world census. Both that composter and the previously used table
(-226,71,744) match independent known anchors.

There are seven farmland blocks, all moisture7, supporting five wheat and two
beetroots. Wheat ages are3,4 and three omitted default-age states; both beetroots
are age3. Fern has12 wheat seeds and no beetroot seeds in the last native inventory.
The existing harvest rule requires held planting stock before harvesting, so those
ripe beetroots are currently unavailable to Fern. Native seed loot is not rerolled
or invented. Only one equipped farmer was present in the last four-survivor census.

The volume also contains14 water blocks, two dirt blocks,25 hay bales, one table
and one composter. Dirt presence alone does not establish a free tillable surface;
one is beneath the composter. Hay gathering remains disabled by the existing
runtime's missing/default-false opt-in. These counts do not establish food per day,
sustainable population, crop growth rates or a renewable production surplus.

Current baseExhaustionPerDay80 costs20 saturation-or-food points per neutral loaded
day before individual appetite and activity/regeneration costs. Seven crop plots
and one successful harvest do not establish capacity for four adults plus children.
Actual throughput, access and distribution still need measurement. No hunger rate,
growth speed, terrain permission, food supply or birth prerequisite was changed.

[Inspection script](inspect-saved-farm.py) is a retained one-off evidence tool,
not a general NBT library. It takes the existing play-runtime lock, reads only the
stopped save, bounds chunk decompression/NBT traversal and writes reports here.
It supports the observed MC26.3 palette string/id/properties encoding plus legacy
Name/Properties, validating packed lengths and both anchors. Unsupported records
fail rather than being counted as air. The retained raw chunks permit later replay
without reopening a game. Running it again reads the current save and replaces
these reports, so preserve this snapshot first.

## Source improvement: owned-food reflex

Previously, eating owned food depended on the elective forage action, which can be
superseded by shelter or danger. The controller now checks a real existing ration
before starvation processing and survival/action arbitration. It uses the actual
food component, removes one real item and applies ordinary metabolism nutrition;
it neither supplies items nor directly heals. The last farmer carrot/potato remains
planting stock. Trading and food-consumption permission gates remain respected.

For a healthy body, the reflex waits until that ration's nutrition fits. Injury can
justify a meal below the18-food regeneration threshold; urgent hunger at6 or lower
does not wait for a large ration to fit. A full body still cannot eat. A32-tick
cooldown is checked on the existing staggered10-tick thinker; elective forage cannot
eat another item during it. Explicit forage can still eat earlier than the reflex;
this is not a universal optimal-meal policy. Sleeping actors wake normally to eat.

Movement/navigation continues. A completed previous elective attempt is settled
before the meal, and an incomplete attempt's learning observation is cleared so
the authored nutrition cannot improve exploration/rest values. The meal itself
does not mark an elective action successful. No new timer, thread, world search
or IO was added to the thinker; the existing small inventory is checked.

## Verification and next acceptance

[Build log](build.log) reports BUILD SUCCESSFUL;203 Java tests and10 Python checks
passed, no failures/errors/skips. [Captured verification](verification.json).
Five added tests cover efficient/urgent/injured/invalid meal decisions and the
real pure metabolism boundary where possessed nutrition prevents a final scheduled
starvation hit. Existing third-party JOML Unsafe runtime warnings remain.
The tagged build processes are absent at the terminal checkpoint.

Native meal acceptance is **pending**: observe actual inventory debit/food and
normal regeneration, waking/continued escape, planting-stock retention, cooldown,
and absence of unrelated learning credit. Pure tests cannot prove that adapter
behavior. No existing resident was starved, healed, refilled or moved to make a
test trigger. Preserve four living/seven dead histories and the canyon obstacle.
Future field/building permission preference was requested; no response is approval,
and current terrain rules remain in force. Next work should validate meals without
unbounded cohort exposure, then address measured production/access/distribution.
