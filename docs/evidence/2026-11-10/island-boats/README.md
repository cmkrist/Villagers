# Native island boat verification

The playable primary save was NOT simulated during these checks. All 129 world
files (except the lock file) remain byte-identical. Selected old authored-save
hashes also match. The game-completion goal remains active and incomplete.

The final island rehearsal uses the actual seven saved grassland residents,
native terrain, supplies and UUIDs in a separate full-pack copy. No actor,
inventory, health, hunger or resource setters were used. Alder-c557 gathered
three actual logs and used an existing workbench: six native recipes produced
one boat and a wooden hoe, leaving three planks and two sticks. He launched,
paddled across the actual inlet and landed. A cold restart during his return
preserved the same passenger, boat and trip; he landed at his original launch.
No food was found on this shore. This proves this saved resident can make and
use a boat; it is NOT evidence of primary-map food sustainability or growth.

The independent declared crop fixture passed first-run and cold restart with
the same production jar. Empty initial bag, one natural tree and mature crops
are explicitly supplied fixture setup. Four gathered logs, eight native recipes,
one owned boat, two native landings and seven harvested carrots carried home.
The hoe was worn by actual harvesting. This IS a successful isolated food trip.

Fixes: discover nearby public workbenches behind doors while retaining reach/LOS
at use; remember an existing home workbench while fetching wood farther away;
search reachable trees to the ordinary home boundary, admitting interaction
reach at its edge; reject wood and work paths outside the world border; prepare
a real hoe for unknown shores, including restored preparation trips.

Validation: 424 Java tests and 14 Python tests passed. The ordinary native-world
client classes were staged under its launcher lock without starting Minecraft.
No test probe was put in that client. Use ./tools/play-native-regions.sh.

Intermediate island rehearsals v1/v2 stopped normally after bounded observations
without a launch. Their records remain under runs/ and are not success evidence.
The frozen food-passage planner remains a separate unfinished investigation:
Alder-5d26 has a plan, Fern-9bcb does not. No passage execution is yet verified;
primary hayForaging remains false. Preserve the primary's three prior deaths.
