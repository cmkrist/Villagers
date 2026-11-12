# Releasing failed native storage routes

NativeCampWork abandons an unsuccessful storage job, but IllagerFood.approachNearby previously left any older path active when its next path could not be found or started. Failed camp supply surveys likewise left old navigation active. Both failure paths now stop native navigation before returning. This changes goal ownership only; native movement, collision, gravity, hunger and damage remain.

NativeIllagerHabitatProbe now includes actual feet, ground flag, velocity, path target, next node and completion in its timeline. The full Java build/test passed519tests (`/tmp/villagers-native-stale-storage-route-tests.log`). Normal copied run v32 independently continues healthy v19 for up to8,000ticks with all residents loaded and meals/passages enabled. Do not infer outcome before terminal audit.

Longer mining v31 remains separately pinned to its earlier JAR and was confirmed live; it is not restarted or replaced by this camp change. Primary save remains stopped. No diagnostic copy promoted.

Normal v32 TERMINAL health_stop at5440ticks, all179original IDs retained/no new bodies. Fern delivered3meals; Oak and Willow each reached12HP/5food, while another distant hungry resident Rowan-8889 reached4HP. Juniper stayed stuck at(-632.3,65,-611.5), despite an active route toward a storage stand. Failure release improves the real run but is insufficient.

Read-only stopped-source NBT identifies top-half oak stair counters atx=-632,y65,z=-613..-611, with white carpet at(-632,66,-612), and full birch ceiling aty68. Pillager body height1.95 plus carpet0.0625 cannot fit in that2block gap. New native-region safe-path validation uses the actual support/obstacle shape height and the mob AABB to reject blocked body space. Hand-openable wooden-door nodes remain plannable for NativeCampDoors. No collision override, terrain edit or body setter. Full519Java tests pass `/tmp/villagers-native-path-clearance-tests.log`. Independent normal v33 tests this from healthyv19; outcome pending.
