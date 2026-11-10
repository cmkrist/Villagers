# Complete native routes to covered standing space beside a bed

Unreleased0.4.1-dev now preserves a working bed route and falls back to a real
covered standing goal when the bed itself cannot be reached. Native acceptance
recorded complete paths into Juniper's house followed by actual shelter and sleep.
Final source, including retry backoff, passed the positive branch.286Java+10Python
checks pass;61recorded-data/source assertions pass. Deployed0.4.0 is unchanged.

## Diagnosis and exact branch boundaries

[Diagnostic-only evidence](../refuge-diagnosis/README.md) captured a collision-free
Juniper whose native bed route targetedY76above his savedY75bed and ended outside
the north wall after15nodes,canReach=false. The same diagnostic-only cohort later
entered the house and slept. We therefore do not claim a causal survival-rate
increase, or that no old route could ever reach shelter. The concrete improvement
is an additional complete path to usable covered floor when the bed route fails.

Both comparisons independently copied exactly278stopped files from
harvest-bootstrap/play57237. They never replace or merge into farm-regrowth/play,
which retains Juniper's zombie death66151 and four living actors. That separate
world's286files remain unchanged. Same UUIDs in separate tests are not a shared
history. No food, seed, tool, growth, terrain, clock, actor/body, health, fatigue,
action or RNG setter was used. Historical supplied fixture resources remain
background; these are not generated-baseline survival experiments.

## Source, actual paths and limits

RefugeRoutes checks at most75cells near the same permitted home bed and uses at
most three home-path queries per plan. Real standing collision clearance, safe dry
support, roof cover, native sight, loaded terrain and a complete exact-goal path
are required. A valid cached goal is rechecked and reused. Existing native movement,
door use, stamina, threat-crossing checks, progress timeouts and home-return bounds
remain. SurvivalPolicy and TraderRecruitment are unchanged. Partial resource or
home paths gain no new permission; no construction, material or bed claim is added.

Initial fixed cohort: Juniper received four floor-route assignments. The retained
last receipt at60839 records a complete34-node route to(-270,75,734) from the south
of the house. He actually reached coverage/sight/proximity and slept, with no route
stall or partial return. This is independent evidence beyond a path-planning receipt.

Final source repeats the positive branch: four floor routes,9homequeries over5plans,
225candidate inspections. Last receipt60645 is a complete6-node route to
(-271,75,734); end actual sleeping position(-268.5,75.5625,733.5)at the real bed.
No route stall or partial home route. A covered standing place is not a guarantee
against every monster; normal danger handling remains active.

The first fixed cohort was reopened for4800natural night ticks. Juniper remained
alive,19HP/food3,and sleeping after66844, beyond the old separate-branch death tick.
All five actors survived this observed segment. This is not a full-night, repeated
population or randomized survival study. Nell remains canyon-stranded with a one-node
native home path; a better destination does not repair an unusable path start.

## Bounded failed-search cost

The first night segment exposed repeated useless floor searches for Nell. Final
source backs off a failed floor search200simulation ticks in the same block cell.
Moving to another cell or changing home/cover permits an immediate search. Cached
valid goals and ordinary bed/threat/guardian/escape checks continue normally.

At the identical stuck position, the earlier segment used384queries/128homeplans
and9600candidate inspections. The final400tickcase used14queries/10plans and150
inspections. This demonstrates lower repeated work per plan, not a measured tick-time
speedup. Snapshot means below are not comparable controlled latency benchmarks.
The final positive cohort separately confirms that this backoff retains useful
floor-route selection and arrival.

## Runs and custody

| Phase | Handle/exit | Native cutoff/final save | Mean tick ms |
| --- | --- | --- | --- |
| Diagnostic-only baseline |48332/0|62038/62041|2.343257|
| Initial standing goals |64985/0|62038/62043|3.248798|
| Same-world night/reopen |63551/0|66844/66847|3.506383|
| Final retry backoff |36106/0|67248/67252|5.244180|
| Final-source positive comparison |12867/0|62038/62043|3.034036|

Every exit0 was directly observed on its original handle. No run was restarted
because an observation expired. No new worker, automated build/release timer,
service change, deployment, model/hash/inference grant or resource hold.

Captured exact tuples, all terminal:
974567/15518475,974699/15518532,974866/15519033;
980593/15568378,980698/15568437,980883/15568956;
984484/15599701,984586/15599768,984708/15600029;
989551/15649677,989695/15649734,989884/15650239.

The short36106run ended before its census attempt. Its exact wrapper/daemon/game
tuples were not captured; custody-gap.json records that limitation. Empty
ownedTerminal is not treated as an exact census or inherited exception. We retained
the original terminal0 and stopped-runtime evidence without inventing IDs or
relaunching solely to repair metadata. Future admission requires fresh exact data.
Original adaptive three and frozen wrapper stay absent; frozen601990/10936325 and
602372/10937054 remain exact in the latest final capture. No stop/signal by Lead,
cause/replacement inferred, or original user client/world/service changed.

## Source bindings and continuation

source-main.zip binds the initial positive and night cohorts. source-backoff.zip
binds the final backoff and positive cohorts and matches current main source;
SHA219b75ea5f248673b4527280d84dd19db0414b241670385f7fe2d4ce03b942cc.
DevJAR SHA1e35d0a4564b57c6cd1ee47f2b5be8791c55bae29fb72b386bbe3f54ad2fee8c.
Old diagnostic source remains separately archived; do not rerun old source-pinned
verifiers against newer main code. Source/deployed0.4.0 retains its prior SHA.

Next: continue the retained-death world toward dawn, actual repeated food income
and reserve refill. Obtain an idle native collision/path inspection for Nell before
choosing a rescue or movement correction. No teleport, restored deaths, free
materials, widened terrain authority or blind partial-route relaxation. General
defense, sustainable production, parent care, construction, adaptation and scale
performance remain work toward the complete society goal.
