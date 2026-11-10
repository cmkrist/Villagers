# Food requests and optional work priority

Unreleased0.4.1-dev gives a hungry villager a way to ask for help without waiting
for an incidental conversation. Owned meals and actual food provisioning still
run first. If no usable ration is held, FoodHelp may select a visible managed
adult within16blocks and the requester's existing home range. Selection reads
only the requester's need and observable peer state, not peer stock or
private hunger. A candidate is not assumed willing or able to help.

Requests retain the existing FoodRequest hunger threshold (.5,or .35 for a
GLUTTON),600tick per-session request budget and actual WANT FOOD phrase. The
neighbor must decode both words. Sharing retains its existing donor rules:
not GREEDY,not low mood,own hunger below.4,and generosity/happy mood or a favorable
opinion. Refusal affects relations only after an actual understood NO. Missing
stock, receiving capacity or language is not fabricated social refusal.

Actual handoff still requires both villagers alive,managed,awake,not trading,
in the same level,within3blocks and configured hearing range,with mutual native
line of sight. One real edible item moves from giver inventory to receiver;
farmer planting roots remain reserved by FoodInventory. Successful transfers add
session-local given/received counters and bounded mind memories, saved through
the existing coalesced writer. There is no direct nutrition increase: the
receiver must eat the actual ration under the unchanged MealPolicy/metabolism.
There is no global inventory/mind/chunk crash-atomicity claim.

Approach uses the shared2planning batches/dimension/second budget,one native path
per batch,no more than128nodes,40tick replan interval,80tick physical stall,
400tick total trip and400tick failed-trip backoff. Searches run at most every100
ticks,with at most16candidate peers tested. Paths must fully reach and remain in
existing home territory. Children may ask adults; no reproduction/readiness gate
is introduced. Danger,sleep,trading,water,insufficient air,night,fatigue or a
superseding food task interrupt the trip. Frozen research mode does not run this
new goal. No terrain edits,teleports,global donor discovery,external timer or new
thread. Authored request/travel clears elective learning credit.

Fair path admission keeps at most128waiting actors per dimension. A journey now
separates waiting for its first admitted native moveTo from physical travel:
the queue wait is bounded to2560ticks, then the existing400tick travel deadline
and80tick movement stall begin only after a permitted path is accepted. Replanning
cannot extend either physical bound. The same distinction applies to remembered
farm, bed and company trips. Interrupted/stale dispatches cannot restart a trip.
Deterministic full-queue checks cover128staggered requesting actors; these are
scheduler-model checks, not a Minecraft population-capacity benchmark.

Optional forestry now requires food above the existing urgent-meal/sprint
threshold6 at work and commit. It keeps the real seed and finite permission while
waiting. FoodSupply.mayWork remains unchanged,so hungry/injured adults and adults
in birth cooldown can still prepare food. Tool/station work used to obtain food
has not been globally disabled by this optional-work rule. Hunger rates,meal
sizes,health regeneration,consent and breeding costs are unchanged.

Native validation produced one authored request,one real bread handoff and one
owned meal: Juniper's food6→10 after metabolism,Dara's bread3→2. Nutrition,stock
and transfer memories persisted across restart. A separate hungry forestry
fixture retained seed1 and its exact outstanding quota2 with zero forestry path
batches. The approach path before that successful contact has not independently
been proven; neither have native foreign-language/refusal branches or food aid
for an isolated peer with no visible in-home neighbor. Native evidence:
[evidence/2026-11-07/food-help](../evidence/2026-11-07/food-help/README.md).

The donated bread was real stock from an existing controlled fixture with earlier
operator inputs. This demonstrates social distribution and survival benefit;
renewable food production,adequate farm yield,parental provisioning and broader
food logistics still need work.

## Production after an unmet understood request

An awake local donor that understands WANT FOOD and passes the existing
willingness rules now remembers one unmet request when it has no transferable
food. A failed transfer caused by receiver capacity does not create production
demand while the donor still has usable stock. The signal wakes existing food
provisioning and raises the producer's edible reserve target from15 to20nutrition.
It grants no recipe, material, terrain permission or nutrition, and preserves the
last planting root and the existing9wheat batch cap. The normal survival and
body-work checks still take precedence.

One requester can occupy this signal at a time. Another actual request from that
same peer can refresh its12000loaded-world-tick lifetime; duplicate signals do not
stack targets. A successful actual handoff to that peer clears it, and replaying
that fulfilled request cannot create another goal. Expiry changes the target
only, never the inventory. The signal is session-local and is dropped on unload;
it is a short-lived observation, not a persisted promise, debt or delivery order.
Ordinary food and identity persistence stay unchanged. No private peer inventory
or remote need is consulted to plan production.

Unit checks cover bounded demand, expiry, replay, body eligibility, planting
reserves and paid bread production without duplicate output. An isolated native
server fixture now exercises an actual understood request, raises the target,
transfers18declared operator wheat through real gifts, and lets the controller
craft five bread from15wheat. The producer eats one bread and gives one to the
requester, who eats it; three bread and three wheat remain. A second launch
preserves both identities, homes, nutrition and real inventories with no repeated
recipe costs, output or transfer. The test mod stays outside the production JAR.

This is a new supplied chamber with selected generous/frugal and glutton founder
traits, existing fixture beds/table,80TPS and configured400base exhaustion/day.
Hunger develops through actual loaded-time metabolism. There are no hunger,
health, clock, growth, age, learning or actor-position setters after initial
setup. The declared wheat arrives only after the actual unmet request. This
establishes the native communication/production/distribution integration, not
autonomous farming, normal-speed survival or population capacity. Lead's primary
world and all its deaths are untouched. Exact artifact bindings, both terminal
process identities and resource accounting are recorded in
[food-demand-native/results.json](../evidence/2026-11-07/food-demand-native/results.json).

## Delivery for a remembered request

A willing healthy adult can now bring actual owned food to the neighbour whose
WANT FOOD request it previously understood. The signal remains bounded to one
peer and expires after12000ticks; the longer window accommodates the observed
delay between crop work and a meal becoming available. It is still session-local
and grants no persistent debt, materials, terrain permission or remote knowledge.
The signal now records charitable versus paid intent. Only the charitable intent
can authorize this delivery or meal deferral; an understood paid food proposal
prepares the same bounded reserve and waits for a negotiated purchase. The
same customer's later understood request can change intent, without adding
another production target. See the separate paid/gift boundary evidence in
[food-intent-native](../evidence/2026-11-07/food-intent-native/README.md).

Selection uses only that identity and an actually visible loaded body within16
blocks and the donor's normal home range. The receiver's handler decides, using
its own need and usable rations, whether it still wants food. A declined stale
request is withdrawn without a gift, social credit or refusal penalty. A real
handoff still needs both actors alive, managed, awake, not trading, within3blocks,
configured hearing range and mutual native sight. Exactly one actual food item
moves through the existing inventory transfer.

The donor keeps at most one transferable item for the task, never the farmer's
last planting root. Injury, own food at12or below, danger, rest scheduling and
body restrictions release meal deferral; urgent self-care stays available.
Delivery uses the existing fair path budget, complete loaded/home-bounded paths,
40tick replans and a400tick total reservation/trip bound, with400tick failed-trip
backoff. No chunk loads, teleport or remote stock query is added. An immediate
nearby handoff changes no navigation and does not wait for a repeat request.

The native supplied-wheat fixture handed over the first real crafted bread11ticks
after the request, with only one request. It then rebuilt the donor's own reserve:
18declared wheat funded five recipes, leaving three wheat, three bread, one
donation and one producer meal. The recipient ate its donated bread. A second
launch preserved nutrition and inventories with no repeated recipe or transfer.
This validates nearby delivery only; native delivery travel and renewable food
adequacy remain open. Source/artifact bindings, exact terminal identities and
receipts are recorded in
[food-delivery-native/results.json](../evidence/2026-11-07/food-delivery-native/results.json).

The separately
archived scheduler observation in
[infinity-runtime/results.json](../evidence/2026-11-07/infinity-runtime/results.json)
uses an earlier pinned source snapshot and does not validate this new feature.

## Selecting seeds for newly granted plots

New farm plots can prefer a real carried planting item that avoids the native
same-crop crowding penalty. At planting time, the villager inspects at most eight
loaded neighbouring crop cells with bounded eye distance and native collider
sight. Same-kind diagonal neighbours, or same-kind neighbours along both axes,
count as crowding. Unseen cells contribute no assumed knowledge. Stable inventory
order breaks ties, and the old choice remains available when there is no
spendable alternative. Existing crops are never removed or swapped by this rule.

Only seed selection changes. Every ground grant, protection, body, tool, reach,
real planting payment, retained replanting seed and atomic commit check remains
required. Native crop growth, random ticks, maturity and loot are unchanged.
Unit checks cover paid alternative selection, stable rows, no duplicate spend
and no spending the last seed. An isolated native two-plot fixture now validates
actual seed selection and payment; any survival/yield benefit remains unverified.
The earlier fixed planted-farm fixtures do not exercise new-plot selection.

## Preparing approved capacity while stocked

A fit adult with food above12and a real personal edible reserve of15nutrition
may work existing explicit farm grants before that reserve runs short. Ordinary
food provisioning, delivery, danger and player reactions still precede this
optional work. Body age, health, fatigue, daylight/rest schedule, real hoe and
seed stock, local loaded paths, home bounds, grants, protection and all planting
commit checks remain required. Raw wheat and retained planting roots cannot
pretend to be an edible reserve. This adds no land discovery or edit authority.

Native validation used a supplied worker with six bread and two protected bare
plots. While protected, seeds and hoe wear stayed unchanged and both crop cells
stayed AIR. After a real operator command removed protection, the ordinary
controller planted a beetroot alternative beside visible wheat and one wheat
plot, paying exactly one seed of each type and two hoe uses while personal stock
remained full. Both finite grants retired. A second server launch preserved the
plots, seed banks and costs without replay. The native fixture is recorded in
[farm-investment-native/results.json](../evidence/2026-11-07/farm-investment-native/results.json).

## Renewable supply is not yet adequate

Two new isolated age-zero wheat fixtures used normal80base exhaustion/day and
native3random tick speed at80TPS, with declared farm infrastructure, founder
traits, seeds, hoe and marked starting rations. No grain or food was supplied
after setup. Six starting bread per adult left the producer at its personal
reserve through the48000tick window, so that case did not exercise production.

With two starting bread each, the producer completed three native wheat harvests,
paying a seed and one hoe wear per replant, and crafted one bread from that grain.
The requester starved at43668. No earned bread handoff or second harvest of the
same replanted cell was established. The failure and death are retained; no
resources or identities were restored. This is a supplied isolated fixture, not
the Lead primary history, and demonstrates neither adequate crop capacity nor
sustainable distribution. Exact receipts, pinned artifacts and terminal process
identities are in
[renewable-food-native/results.json](../evidence/2026-11-07/renewable-food-native/results.json).

The native field run predates the new seed-layout preference. Next validation
must cover actual paid new-plot selection, enough explicitly declared startup
stock to span native first-crop latency, and an observation window covering real
regrowth. More plots or mixed planting must earn their real costs and retain
protection; crop growth, hunger and deaths cannot be bypassed to pass the test.


## Meal selection and traceable handoffs

A one-item gift now prefers the supported food with the highest actual component
nutrition that fits the recipient's inventory. Selection is read-only and retains
the last planting carrot/potato. If the largest meal cannot fit, an available
smaller ration can still be offered. The delivery meal reservation uses the same
selection rule. Existing consent, self-care, distance and line-of-sight checks
still govern contact.

Successful server-thread handoffs keep one bounded session receipt on each actor:
item, nutrition, custom-name marker, game tick, actor IDs and before/after item
counts. These are diagnostic observations, not persisted commands or claims on
another villager's inventory. Native probes check exactly one donor item spent
and one recipient item gained, and distinguish marked starting bread from
unmarked bread produced using harvested grain.

## Continuing exhausted farm observations

`tools/food-economy-server-test.py --continue-from` copies a stopped isolated
renewable fixture into a fresh runtime. It accepts only an observation-budget
failure, refuses a held server world lock, and retains prior events and the
original failed report. It does not reopen deaths or reset resident state.
The test observer is restored to enable ordinary player-nearby random ticking;
resident food, inventory, health, terrain and simulation time are not edited.
Current mod/probe artifacts are pinned separately from the original run.

`--minimum-ticks` keeps observing after the first food loop, and
`--verify-restart` checks exact saved body bread/wheat and saved mind nutrition at entity
load, before resumed gameplay. Later legitimate recipes or handoffs are allowed. New-session counters start at zero; prior paid harvest receipts remain
part of the evidence chain. A successful isolated peaceful fixture demonstrates
a bounded food pipeline, not indefinite survival, full threats, an unprepared
world, trade prices, or a complete local economy.


The completed mixed-farm chain reached tick96001 with both adults alive:
32 paid harvest/replants, six cells harvested repeatedly, four bread recipes
across sessions, and two receipted earned-bread handoffs during the extra day.
Both meals were consumed. Setup supplied four marked bread per adult; no food
or grain was added after setup. The original72000tick observation failure is
retained. A corrected entity-load check verified saved stocks and nutrition from
the96001tick world, and a short food continuation plus restart also passed with
the combined social build. Earlier160tick restart checks failed because live
villagers could already harvest/share; those failed checks are preserved.
See [mixed-farm-completed/results.json](../evidence/2026-11-07/mixed-farm-completed/results.json).
This is bounded peaceful production and cooperation evidence; natural-village
survival, tool replacement over long horizons, delivery travel and actual trade
remain separate acceptance work.
