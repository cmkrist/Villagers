<img src="assets/icon.svg" width="64" height="64" alt="">

# Villagers

**Native Four Regions:** run `./tools/play-native-regions.sh` for the separate
initial64×64-chunk map with Grassland, Forest, Frozen Peaks and Desert quarters. It uses
Minecraft's native Overworld noise terrain, caves, features and structure/village
generation, with standard Nether and End dimensions. Village placement follows
vanilla biome rules. The original Four Biomes War save is preserved separately.
Once the recorded adventurer owns a pickaxe, the dev command
`/villagers native-exploration` opens an8192-block exploration border for the
native world. The four regions, existing residents and native terrain are retained.

Managed villagers can gather a small natural-tree wood quota, craft a workbench
and a real boat, and row to a visible shore when food needs a water crossing.
Crop expeditions prepare a real wooden hoe. Matching planks, native recipes,
boat ownership, safe dry-bank landings and return trips are enforced; no boats
or food are supplied. At-sea save/reopen and a return with harvested food have
passed isolated native tests. Routes currently cover loaded, level source water
within96 blocks; waterfalls, ice and long unobserved ocean voyages are excluded.
In the native development map, loaded villagers inside the border join learning
after the initial trial activates, without replacing its original cohort.

Native-map adventurers prepare tools from gathered wood and real crop reserves.
The primary save has now reached **game time253965**: Clover owns an earned
**iron pickaxe**, crafted from physically mined iron, native furnace smelting
and the actual workbench recipe. All179humanoid UUIDs from the observation
baseline remain; all33saved managed villagers are at full health. The grassland
cohort was actively simulated; this does not prove sustained care of every region.
The original authored save is unchanged. Earlier runs recorded three starvation
deaths among the initial eight; that history is retained. Sustained faction
growth, diamond equipment, Nether/End progression and game completion remain
unfinished.

Villagers can place an owned dirt step to reach remaining natural tree trunks.
Quarry work follows native paths with bounded drops and protects actual buildings
and their footing. Copied camp tests recovered and placed two real nursery beds;
captive meal delivery and camp growth are still being verified, with camp features
disabled in the primary save.
[Primary iron-pickaxe evidence](docs/evidence/2026-11-10/native-earned-iron-pickaxe/PRIMARY-EARNED-IRON-PICKAXE.json),
[Primary stone-pickaxe evidence](docs/evidence/2026-11-10/native-stone-progression/PRIMARY-STONE-PICKAXE.json),
[current work and limitations](docs/HANDOFF.md), and
[boat return evidence](docs/evidence/2026-11-10/native-boats-and-cells/README.md).

**Four Biomes War:** run `./tools/play-war.sh` to open the separate64×64-chunk
world, split into Grassland, Forest, Snowy Mountains and Desert. Each biome has
one settlement: two villager towns and two pillager towns. Persistent creature
minds now also drive illagers, Endermen and wandering traders. Villager and
illager soldiers march, fight with native weapons and retreat; casualties persist.
See [scenario details and controls](docs/design/four-biomes-war.md) and
[native/visible acceptance evidence](docs/evidence/2026-11-09/four-biomes-war/README.md).
Existing trial worlds are separate and preserved.

A Fabric research mod for persistent villagers whose needs, temperament, and
experience influence their choices. The starting point is Creatures' artificial
life: learning while living, grounded reactions, and individual histories. The
goal is an interesting population that survives Minecraft and develops over time.

The current source provides opt-in AI replacement, five actions, bounded online
learning, named identities, remembered beds, player-style hunger, heritable traits,
language, gossip, mood, and a separate disk folder for every villager in every world. It is an experimental foundation; survival improvements,
long-term autonomy, and population capacity still need controlled gameplay tests.

Current development status, audit findings, priorities, and continuation instructions:
[Project Lead handoff](docs/HANDOFF.md) and [codebase audit](docs/audit-2026-11-07.md).
The source has advanced beyond the published 0.3.0 artifact; release test counts
below describe their historical artifacts.

## Project home

Canonical source and development runtime:
`/home/niko/.internal/source/Villagers`.

The T3 workspace at `/home/cmkrist/.t3/projects/villagers` is a symlink to that
directory. Source, research, generated laboratory worlds, and villager records
therefore stay on the internal drive.

## Build and play

The pinned environment is **Minecraft 26.3**, **Fabric Loader 0.19.5**,
**Fabric API 0.162.0+26.3**, **JDK 25**, and Modrinth's
[Vanilla Perfected 1.0.0+26.3](https://modrinth.com/modpack/vanilla-perfected/version/IFYAmjXO).

From the project root:

```bash
python3 tools/prepare-client.py
./tools/test.sh
./tools/run-client.sh
```

Preparation validates official mod hashes and copies pack assets into an isolated
`run/`; it never copies the user's saves or launcher account/session files.
The scripts use JDK 25 from `VILLAGERS_JAVA_HOME`, `JAVA_HOME`, or `java` on
`PATH`, in that order, accepting only a real Java 25. The launcher opens a visible client and creates or resumes the disposable
**VillageTrial** world (the older flat **VillagerLab** remains available). See [testing instructions](docs/testing.md) for refresh
behavior, commands, screenshots, evidence, and restart checks.

Outside the automatic development lab, adoption is explicit. With commands
enabled, use `/villagers adopt` near a villager to enable the prototype,
`/villagers status` to inspect it, and `/villagers release` to restore vanilla AI.
These commands target the nearest villager within 24 blocks. The tag persists
across saves; released villagers retain their learned identity files.
Adopted villagers show a `name · activity` label unless a player has given them a
name tag, which the mod never overwrites or clears.

Sneak-right-click an adopted villager with an empty hand to hear what it remembers.
In survival you only understand memories you shared, because you were within 16
blocks with line of sight (`memoryWitnessRadius` in `config/villagers.json`); the
rest read `???`. Creative and spectator players see everything. Operators can use
`/villagers memories`, which follows the same rule.

Lab, trial, probe and `report` commands are development tools. They are only
registered when the JVM runs with `-Dvillagers.lab=true`, which the Gradle dev
runs set; a normal install of the mod does not include them.

## King Size Bed

Craft a **King Size Bed** with two beds above two planks in a 2×2 crafting grid,
or use `/give @s villagers:king_size_bed`. It places a 2×2 red bed with two
independent sleeping positions. Leave two blocks of headroom and provide a roof,
a nearby wooden door, and a separate spare bed for the baby.

Consenting, healthy, well-fed adult managed villagers can walk to a nearby King
Size Bed, lie down for a five-second courtship pause with hearts and ordinary
villager murmurs, then have a baby and release 1–7 XP (with mob drops enabled).
This uses native sleeping poses; it has no explicit animation or moving blanket.
Danger, interrupted sleep, lost readiness, a broken bed, or a timeout cancels the
visit without charging food or awarding XP. Existing ordinary-bed births remain
available. Each successful birth still consumes real food, reserves the nursery,
records both parents, inherits traits, and applies the normal cooldown.

## What is implemented

- Hunger, fatigue, and social drives; UUID-seeded names. A heritable genome with
  ten bold traits (coward, brave, gossip, loner, glutton, frugal, lazy, diligent,
  greedy, generous) and five tendencies changes how fast needs grow and what villagers
  choose to do. See `/villagers status`.
- Foraging for finite available food, resting, socializing, exploring, and an
  authored threat/fire escape response. Learning changes preferences among
  permitted actions; the escape response is a separate survival rule.
- A bounded contextual action-value learner and 48 recent event records.
- A remembered in-game bed, plus `<world>/villagers/<UUID>/mind.json` containing
  identity, drives, temperament, learned values, and history.
- World-scoped storage, atomic snapshots, corrupt-file preservation, and
  protection against overwriting unsupported future schemas.
- Pure-Java unit tests (`./tools/test.sh`) for meaningful preference changes, action masks,
  bounded state, and persistence behavior. Rendered acceptance evidence is tracked
  separately; unit tests do not establish gameplay survival.

Replacing `Brain.tick` also stops vanilla profession routines, restocking,
breeding, and brain-driven door interactions for adopted villagers. Navigation,
collision, and trading interaction remain available, but the prototype has not
recreated the complete vanilla village economy. Current source implements lethal starvation, bounded individual opinions, and
learned language; these additions still need visible gameplay validation. Current source includes narrow paid farming, bread/tool/table crafting, food
sharing and ordinary two-parent breeding. General building, autonomous resource
discovery, sustained population growth, demonstrated genetic evolution and LLM
inference remain future work. Nothing
here establishes personhood or a proven many-agent capacity.

Current language development adds persistent biome-based cultural factions and
founding dialects. Children begin with ten core concepts, including basic sentence words; local conversations,
visible objects and real meals supply evidence for learning more words. Villagers
can understand foreign synonyms while retaining their native pronunciation, and
travel does not change faction identity. `/villagers language` reports origin,
vocabulary and nearby listeners' comprehension. Existing saved words are preserved.
[Language mechanics and validation limits](docs/design/living-villages.md).

Current source adds compositional subject/verb/object sentences, with distinct
founding word orders for plains, desert, forest and snowy cultures. Listeners
interpret spoken sounds through their own learned vocabulary and grammar; they
can be confidently wrong. Observable encounters can change word meanings and
grammar preferences, and those changes survive saves and pass to children.
Care, trade and family signals use decoded requests. Generic gossip no longer
unlocks an unnamed person's exact memory or reputation record. No LLM is used.
See [verification goal and limits](docs/language-grammar-goal.txt).

The next language milestone adds attributed, expiring beliefs, spoken clarification
and grounded word repair, interpreted invitations and promises that guide actual
companionship, and learned constructions that generalize to new sentences.
These features use bounded local rules and preserve the no-LLM interpretation
boundary. [Current priorities, verification and limits](docs/language-life-priorities.txt).

Grounded reference development adds learned spoken aliases for encountered people
and jointly observed places. Unfamiliar or ambiguous names remain unresolved;
listeners can also retain mistaken bindings. Named reports use the listener's
own reference memory, with no spoken UUIDs/coordinates or copied private episodes.
[Reference progress and remaining goal](docs/grounded-references-progress.txt).

## Research and design

- [Living villages: agreed roadmap](docs/design/living-villages.md) (genome,
  language, opinions, mood, breeding, trading, chunk residency)
- [Villager economy research](docs/research/economy.md)

- [Creatures mechanisms, limitations, and evidence](docs/research/creatures.md)
- [Modern approaches and falsifiable experiments](docs/research/modern-solutions.md)
- [Implemented architecture and roadmap](docs/architecture.md)
- [Visible-client testing procedure](docs/testing.md)

- [Movement regression and restart evidence](docs/evidence/2026-11-06/movement-fix/README.md)
- [Current resource coordination](docs/coordination.md)

Historical flat-lab survival trial: five confirmed zombie-caused deaths; three of the original eight remained alive. The survival objective is not yet met. See the [failed trial evidence](docs/evidence/2026-11-06/survival-trial/README.md).

## Current village and adventure iteration

The default visible test is now a normal generated **VillageTrial**, preserving
its natural villagers, houses, farms and iron golem. The failed flat lab and its
three survivors are retained separately. See [village/adventure design](docs/design/adventure-village.md)
and [testing](docs/testing.md).

Villagers can use wooden doors, accept real swords/hoes and planting items from a
sneaking player, defend with possessed swords, and harvest/replant ripe field
crops by hand or with a possessed hoe, reserving a real seed/root from owned stock
or native drops to pay for replanting. Hoe use wears the actual tool. General block breaking and
placement stay disabled. Dusk/home-range/refuge logic and seeking nearby vanilla
golem protection are authored safety capabilities. Golems retain vanilla AI in
this iteration; a distinct persistent guardian agent type is a later extension.

Managed villagers also collect nearby dropped tools and weapons, including items
thrown by players, when their inventory has space. Normal pickup delays and the
mob-griefing gamerule apply; seeking equipment yields to food and safety needs.

That iteration's build passed its unit tests. Live generated-village evidence and unresolved
acceptance checks are recorded separately; this is not a multi-day survival pass
or a complete player gameplay implementation.

- [Generated-village iteration: evidence and pending checks](docs/evidence/2026-11-07/generated-village/README.md)


## Player impressions in 0.3.0

[Install the 0.3.0 social update](releases/0.3.0/README.md). Villagers begin with
high awe and little evidence about unfamiliar players. Each villager remembers
individual players and revises trust, fear and awe after useful gifts, actual
harm and witnessed protection. Brief attention, cautious approach and avoidance
remain subordinate to shelter, danger and physical needs. Inspect a resident's
view of you with `/villagers impression`.

The update preserves existing identities, household beds and learned policies
while upgrading saved minds to schema 2. Back up worlds before upgrading;
0.2.0 cannot read the new schema. [Design and limits](docs/design/player-impressions.md).
All 44 unit tests pass; [visible gift, harm and persistence evidence](docs/evidence/2026-11-07/player-impressions/README.md)
records the tested behavior and remaining checks.

## Earlier 0.2.0 and accelerated trials

[Download/install the 0.2.0 build](releases/0.2.0/README.md). The canonical JAR is
`releases/0.2.0/villagers-0.2.0.jar`; its 30 unit tests passed. `./tools/play.sh`
launches the current source in a private normal-speed copy.

Two independent full-pack visible clients ran adaptive/frozen policies from
the same saved checkpoint, with measured 80 TPS after a 40 TPS staging interval.
Their bounded 48000-tick sessions completed and returned to 20 TPS. The original historical trial
is preserved. [Evidence and experimental limits](docs/evidence/2026-11-07/accelerated-pair/README.md)
separate simulation throughput from actual learning/survival quality.


## Bread crafting in current source

Hungry adopted villagers carrying at least three wheat can seek a nearby existing
crafting table and use the server's current wheat-row bread recipe. The operation
consumes actual wheat and requires output capacity; blocked/missing stations are
visible in `/villagers status`. Sneak-right-click wheat gifts transfer real items.
This is a narrow crafting capability, not arbitrary recipe discovery or a complete
village economy. General block breaking/placement remain disabled. The current
source is ahead of the released 0.3.0 JAR.


## Families in current source

The first ordinary-breeding slice is implemented, with real parental food costs,
a spare sheltered bed, inherited genes/language and persistent two-parent lineage
in schema7. A labelled supplied fixture now has a native two-parent birth with
exact food charges, inheritance and successful save/reopen; see
[family evidence](docs/evidence/2026-11-07/family-company/README.md). Full parent care,
bed construction and naturally sustainable population growth remain unfinished.
[Rules, save compatibility and remaining checks](docs/design/families.md).

## Protected farm expansion in unreleased0.4.1-dev

Villagers can till and plant explicitly approved bare ground using real planting
items and hoe durability. An operator standing on the intended soil grants a small
site with `/villagers site farm <radius>` (0..4). Protected areas take precedence:
`/villagers site protect x1 y1 z1 x2 y2 z2`. List/revoke grants or protection with
`/villagers site list` and `/villagers site remove <UUID>`.

Outside standard block writes retire affected permissions; completed plots retire
their grant. Unknown structures are not demolition resources. Generic breaking,
general building placement remains unfinished. Native checks cover
protection, a paid plot and restart without duplicate costs.
[Rules and limits](docs/design/terrain-permission.md),
[evidence](docs/evidence/2026-11-07/construction-sites/README.md).

For arid farms, `/villagers site soil sx sy sz tx ty tz` approves one exact exposed
dirt source and one bare sand destination near water. A fit villager can collect
and carry the real dirt, pay it to replace sand, retain the displaced sand, then
till and plant with an owned seed and hoe. Protection at either endpoint blocks
the whole job. Ordinary home bounds stay unchanged; only the assigned source trip
gets a finite corridor. Native gathering, paid placement, planting and reopening
passed. Autonomous land discovery and general construction remain future work.
[Soil rules](docs/design/soil-transfer.md),
[native evidence](docs/evidence/2026-11-07/soil-transfer/README.md).

Farmers also remember crop patches they actually see, including immature crops,
and can return to them instead of relying solely on their assigned composter.
Up to eight patches persist in mind schema8; sight, ordinary home bounds, complete
loaded paths and all material/protection rules still apply. Native observation,
two return trips, one paid harvest/replant and reopening passed.
[Farm memory](docs/design/farm-memory.md),
[evidence](docs/evidence/2026-11-07/farm-memory/README.md).

## Wooden-hoe production in unreleased0.4.1-dev

An adult carrying planting material can replace a missing hoe using real held wood
and a reachable existing crafting table. Villagers use the loaded plank, stick and
wooden-hoe recipes, retain spare materials and equip the result. Sneak-right-click
gifts accept supported wood ingredients and sticks. The native supplied-log check
and restart passed; autonomous forestry remains unfinished. Station construction
is implemented below with an explicit site grant.
[Rules and limits](docs/design/tool-production.md),
[evidence](docs/evidence/2026-11-07/tool-production/README.md).

## Crafting-station construction in unreleased0.4.1-dev

Villagers can craft a table from real held wood and build it at an explicitly
approved clear site. Stand in the intended cell and use `/villagers site table`;
protected areas override the grant. Outside edits revoke its watched footprint,
and completion retires permission. Native checks verified protection,paid table
construction,subsequent hoe production and restart without duplicate work.
Autonomous wood gathering remains unfinished.
[Rules and limits](docs/design/station-production.md),
[evidence](docs/evidence/2026-11-07/station-production/README.md).

## Rest recovery in current source

A valid but unreachable or occupied bed no longer prevents resting in place.
Villagers need a real quiet interval on safe ground; travel or interruption earns
no rest credit. Native checks showed Juniper recover naturally and retain that
recovery across restart. A subsequent clipped-start recovery restored Juniper's
movement toward home; reaching the actual bed remains unverified.
[Rules](docs/design/rest-recovery.md),
[evidence](docs/evidence/2026-11-07/rest-recovery/README.md).

Residents whose body is already clipped under terrain can attempt a guarded walk
to the centre of their current floor cell. Native movement and gravity handle the
step; no terrain edits or teleport. Juniper escaped the original slab/stair trap
and continued moving after restart.
[Rules](docs/design/floor-centering.md),
[evidence](docs/evidence/2026-11-07/home-route/README.md).


## Finite grain bootstrap in current source

`hayForaging` defaults to `false`, preserving the crop-only terrain boundary.
Explicitly enabling it permits adopted villagers to gather nearby visible,
exposed existing hay, unpack one bale into nine wheat using the native recipe,
and use an existing crafting table for bread. Actual items and output capacity
are required. It neither supplies tools nor makes a renewable economy.
[Limits and live checks](docs/design/food-bootstrap.md).


## Pinned-pack client shutdown

Current source gracefully closes Animatica0.6.2+26.3's otherwise unclosed static
animation executor at client exit. A rendered title-only reproduction failed
before the fix and exited normally afterward; other versions/executors are left
alone. World save/reopen with the fix is still pending.
[Evidence and limits](docs/evidence/2026-11-07/shutdown-pool/README.md).


## Dedicated server and trader recruitment (0.4.0)

Install `build/libs/villagers-0.4.0.jar` in a Minecraft 26.3 Fabric dedicated
server's `mods/` folder with Fabric Loader 0.19.5+, Fabric API 0.162.0+ and JDK25,
then restart the server. Clients do not need this mod for recruitment.

Sleep in your own bed to set your home, then sneak-right-click a wandering trader
with an empty main hand. Each trader has one 20% willingness roll, saved with the
entity; repeated clicks cannot reroll a refusal. A willing trader offers to join.
Repeat that interaction to accept. Normal right-click still opens trading.
`traderRecruitmentChancePercent` in `config/villagers.json` accepts0..100 (default20).
The home bed must be loaded and in the trader's dimension.

The accepted trader becomes a persistent smart villager at its current location,
with your bed's position as its fixed household anchor. It walks home using the
existing survival navigation and seeks a separate spare bed within16blocks.
Without a spare bed it can shelter near home but cannot sleep in your bed. Provide
food and a safe route: recruits retain the mod's hunger and survival needs.
Recruitment and household anchors survive restarts. Your original home bed and
subsequent observed player spawn beds remain protected even when you log out;
`<world>/villagers/player-beds.json` stores these reservations. Managed villagers
cannot claim or sleep in protected beds or reserve them for children. Old
reservations are deliberately retained when a player's spawn changes.

Dedicated-server acceptance (separate disposable world, two launches):

```bash
source tools/java-env.sh
villagers_select_java
python3 tools/recruitment-server-test.py --server-home /path/to/prepared-fabric-server
```

The test mod stays out of the release JAR. It checks consent, no-home rejection,
ordinary trading, refusal persistence across clicks, conversion, spare-bed POI
claiming, both protected bed halves, and recruitment/bed protection after restart.

## Finite forestry prototype in unreleased0.4.1-dev

The terrain policy is limited autonomous building with real materials and
protection for player structures. Explicit mangrove jobs reserve a real propagule,
allow only selected logs/leaves and require paid planting. Native trials verified
wild seed collection, three approved leaf cuts, persistence and player-write
revocation. The selected logs still have no safe visible standing position;
native log income, planting and natural regrowth remain unverified.
[Rules](docs/design/forestry.md),
[evidence and remaining work](docs/evidence/2026-11-07/forestry/README.md).

## Food help and survival priority in unreleased0.4.1-dev

Hungry villagers can ask visible nearby adults for food using their actual words.
Sharing depends on understanding and willingness and moves one real food item.
Native validation showed one bread donated and eaten, with nutrition and history
preserved across restart. Urgent hunger suspends optional forestry while keeping
its real planting reserve and finite permission. Renewable food supply remains
open. [Rules](docs/design/food-help.md),
[evidence](docs/evidence/2026-11-07/food-help/README.md).

## First native forestry income and paid planting

A villager now completed wild seed collection, three approved leaf cuts, one
separately approved dry root cut, two real log harvests and paid replanting.
The logs and planted propagule survived restart without repeated costs or income.
Existing leaf permissions gain no root authority; direct root edits still revoke
the job. Natural tree regrowth and another harvest cycle remain unverified.
[Evidence](docs/evidence/2026-11-07/forestry-access/README.md).

## Earned wood becomes an actual workstation

A fed villager can fulfill an explicit table job using its own wood without
carrying farming supplies. Native testing followed a harvested log through four
planks,one actual table item and paid placement. Protection blocked all spending;
the completed table and remaining materials survived restart without replay.
[Evidence](docs/evidence/2026-11-07/earned-station/README.md).

## First crop income with harvest-funded replanting

Villagers can reserve a matching seed from actual native harvest drops when
they have none in stock. Native testing produced two beetroots and a seed bank,
replanted both crops and retained the yield and hoe wear across restart without
new supplies. Repeated farm output and sustainable food adequacy remain open.
[Rules](docs/design/harvest-replant.md),
[evidence](docs/evidence/2026-11-07/harvest-bootstrap/README.md).

The next natural observation followed both earned beetroots into actual meals
and one wheat crop to maturity. It also exposed an overnight zombie death;
sustained food output and defense remain open.
[Continued evidence](docs/evidence/2026-11-07/farm-regrowth/README.md).

When a bed route fails, villagers can now try a complete native path to covered
standing space beside their existing bed. Native checks recorded actual arrival
and sleep; failed searches back off to reduce repeated work for trapped residents.
This does not yet establish general village defense or survival improvement.
[Rules](docs/design/refuge-standing.md),
[evidence](docs/evidence/2026-11-07/refuge-floor/README.md).

Refuge timing now follows the native rest schedule, so ordinary rain does not
keep villagers asleep through daylight. A continued native world verified waking,
another paid crop cycle, bread crafting/eating and sunset return. Both observed
deaths remain preserved; sustained food adequacy remains open.
[Rules](docs/design/weather-refuge.md),
[evidence](docs/evidence/2026-11-07/dawn-food/README.md).

## Autonomous company and the bounded food loop

Current unreleased source adds deliberate social visits shaped by loneliness,
expressed temperament, curiosity and existing relationships. Villagers walk to a
stable visible companion, require willing contact, remember the encounter and
use the existing conversation system. Recent-peer limits encourage different
encounters; food, family and safety retain priority. Activity labels show who is
being visited. [Behavior and limits](docs/design/social-company.md),
[native walking, conversation and reopen evidence](docs/evidence/2026-11-07/social-company/README.md).

A separate peaceful supplied farm trial reached tick96001 with both adults alive,
repeat crop harvests, bread made from harvested wheat, and two recorded earned
bread handoffs followed by meals. Exact saved stocks and nutrition passed load
checks. The long trial used its archived food build; a shorter food continuation
also passed with the initial social build. This is bounded fixture acceptance,
with initial infrastructure, seed/tools and rations, rather than natural-village
or indefinite survival. [Food results](docs/evidence/2026-11-07/mixed-farm-completed/results.json).


## Civilization stability in current source

Unreleased0.4.1-dev now passes a prepared four-resident peaceful village through
72,681loaded ticks (over three days) and clean restart, with no damage or zero-food
interval. Two farmers produce food and both dependents consume actual produced
meals. Crops regrow and are harvested again; memories and actual inventories
persist. The overnight minimum food was5, so the safety margin remains narrow.
Breeding was enabled but no birth occurred in this run.

This iteration fixes a rejected-family-route server crash, repeated requests to
unhelpful nearest neighbours, forgotten suppliers and production work being
preempted by pantry replenishment.393Java tests and10Python checks pass, alongside
separate paid economy, food commission, charity and renewable-food native checks.
[Evidence and limits](docs/evidence/2026-11-07/civilization-stability/README.md),
[reproduce the checks](docs/testing.md#civilization-stability-acceptance).

Prepared peaceful survival does not establish indefinite tool/wood renewal,
hostile natural-village survival, child-rearing, autonomous construction or a
population capacity. Current source remains ahead of the installed/released build.
