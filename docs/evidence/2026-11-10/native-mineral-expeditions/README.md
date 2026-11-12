# Native mineral expeditions: all-resident food guard

Dragon objective remains incomplete. No iron or later dimension milestone is
proved by this work. Primary was last verified stopped at205346; experiments
below are distinct copies, never promoted into the primary.

The existing boat mission previously ended mineral exploration at the first
stone pickaxe. `MINERALS` now continues the earned-equipment mission when iron
preparation still requires real mining. Departure requires at least16 actual
retained mine floors, no active shaft/quarry, and the existing food, health,
fatigue, daylight and threat admission. No ore coordinates are injected. The
same real boat recipes, native piloting,16-block landing work area and bounded
ashore/return behavior apply. Old FOOD/STONE trip records remain readable.

Visible owned boats can supply additional nearby launch banks. Discovery is
bounded to four owned boats within a48-block query, with line of sight required
beyond the old16-block neighborhood. A complete dry path, native valid bank and
water crossing are still required; boarding still requires actual physical reach.
This addresses repeated new-boat preparation despite a nearby owned boat.

480Java tests pass in the isolated build, plus14Python checks. The shared Gradle
test output had an EOF failure during concurrent builds; verification was rerun
with separate build/cache directories. The audit wrapper accepts `--production-jar`
to copy the exact tested artifact, with its hash retained in `artifact-pins.json`.

## Runtime records

- `native-fresh-mine-front-v3`: terminal exit0,24000tick window, zero iron. Actor
  leaves the exhausted old mine after fresh-floor avoidance. Subsequent shallow
  quarry retirement lost the exit in this older copied chain.
- `native-mineral-crossing-v1`: terminal exit0 at6139ticks after an explicit
  graceful stop. Older-chain Clover remained20HP/20food but could not find a
  complete exit. A nearest failed-site inference did not recover it and was
  removed. Current quarry retirement retains its own exact entrance instead.
- `native-retained-quarry-exit-v4`: terminal exit0, primary-derived copy24000ticks,
  all eight tracked actors20HP. Actual MINERALS plans were recorded, but boat
  preparation timed out seeking wood. No sailing or iron proof. The stopped save
  contains Clover's owned oak boat at(-464.929,62.523,-369.184); that is operator
  evidence only, not a coordinate installed into the controller.
- `native-owned-mineral-boat-v2`: TERMINAL copied experiment, source is the stopped
  v4 copy, all33 managed residents loaded. Exec session48087, PID1208237, port25602,
  24000tick budget at80TPS, stopped around15920ticks by native resident health checks.
  JAR SHA256 `330e5fbc16e568e7cd738217fc7baa47b1c021006a2a7b739b482b82af0b495a`.
  Log `/tmp/villagers-native-owned-mineral-boat-v2.log`. The server saved normally
  with exit0; the probe and wrapper correctly FAILED the food/health gate.
  Clover remained20HP but did not sail or acquire iron. All179 humanoid bodies
  remain in the copy; Willow-965a ended7HP/0food with9wheat and no home at
  (636.54,100,304.18), remembering a workbench at(644,99,296). Next diagnose
  Willow's actual bench route and meal preparation before a longer primary run.
  A fresh read-only primary audit retained179 humanoids and33 managed villagers,
  all20HP. That primary was not advanced or replaced during this experiment.
  See [saved result and starving resident](all-resident-mineral-result.json).
  The frozen production JAR, its sources and exact hashes remain in this runtime.

The independent `native-surface-quarry-exit-v5` copy was also observed live;
do not relaunch it or replace its save based on a stale handoff. Camp options
remain off in the actual primary. Gate/wait camp copies still stopped at health
guards and do not establish sustained care.
