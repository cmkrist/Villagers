# Own food work after parenthood

## Source behavior

FoodSupply now establishes a finite own-food goal independently of reproduction.
A positive native age is an adult's birth cooldown, so it no longer prevents that
adult preparing food. Disabling breeding also does not disable self-supply. Babies,
dead/nonfinite bodies, fatigue>=.75 and unsafe/night/sleeping/wet/trading conditions
cannot start this adult work. Immediate danger/shelter/player priorities remain
above it. An injured or hungry adult can try to obtain its own food; low mood alone
does not forbid self-supply. No food/hunger/growth/healing rate was changed.

The target is15 held nutrition (three ordinary bread), not15 items, reproductive
units or a promised day's food. It uses actual FOOD components, excludes raw wheat
and retains a farmer's planting root. Counting scans the small native inventory
without snapshots, mutation or overflow. Reproductive stock/courtship/cooldowns
remain separate; neither food intent creates consent nor pays a birth cost.

Shared stocking takes the active self/family goals' required raw inputs, capped
at9wheat per batch. Actual native recipes/items still determine the result. Stock
work stops once its active deficits are satisfied; saturation is not guessed into
the reserve. Existing bread/crop transactions and root/wheat target caches remain.
Food route status no longer overwrites the transient family narrative status.

The known-farm capability still requires the actor's actual native JOB_SITE,
loaded composter, own hoe/seed, permitted home range and native route. Existing
400trip/80stall/400backoff and shared<=2path batches/dimension/second/<=128nodes
remain. Unsupported/empty sites back off100ticks, avoiding a food-entity search
every thinker on an actor unable to work. Existing bounded world searches are
reused; eligible population can now be larger, so scale remains to benchmark.
No thread, timer, disk write, forced chunk load or peer-private-state lookup added.

Development source is **0.4.1-dev**, unreleased. It was not installed on the server.
The deployed0.4.0 artifact remains pinned to
4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0 and unchanged.
Versioned build outputs preserve the prior artifact and its deployment receipt.

## Checks and actual native scope

Build3582 and final29633 exited0:225Java+10Python pass, zero failures/errors/skips.
[Final build](build-final.log). Six added regressions cover persisted/native birth
cooldown, body safety, actual component nutrition/raw-grain exclusion, seed retention,
finite raw targets/overflow, and goal reconstruction/completion without altering
consent or stock. Existing third-party JOML Unsafe runtime warnings remain.

Native71518 exited0 in a new independent `runs/household-food/play`. It copied the
five-resident family fixture exactly (187files), then changed **only copied Fern's
hunger state** tofood15/sat0/exhaustion0/timer0 and redundant hunger.25. The queued
`time set day` is an explicit clock fixture input; game time, native age and the
60379birth deadline were not reset. No food, tool, seed, terrain or ripe crop was
supplied. Earlier supplied4/8bread and hoe/station remain labelled background.
[Exact setup](setup.json), [commands](commands.txt), [source hashes](source-before.json).

Fern used one existing bread, leaving2bread, nutrition deficit5/raw target3. At the
native boundary: age4058 (>0), nextBreedingAt60379 (>current38322), familyFoodGoal=false,
selfFoodGoal=true. New notes record planning and arrival at its real composter
(-226,72,752). Native position(-224.70,72,753.19), doors opened3/closed2, seed12
retained, health20/food20. This verifies self-food intent and actual farm access
during birth cooldown, not a completed refill. [Verification](verification.json),
[native snapshot](budget-native.json), [results/custody](results.json).

Harvests0/crafts0: the five observed wheat plots were unripe before and after the
run, and Fern lacked beetroot seed for the two mature beetroots. Raw selected chunks
and normalized reports in [before](farm-before.json)/[after](farm-after.json).
These are bounded local observations, not whole-world crop totals. Crop maturation,
seed/tool acquisition, sufficient field capacity, complete reserve refill and a
sustainable household economy remain open. No crop state was advanced to get a pass.

Observer1200ticks37122→38322, final save38326.5live (4original+1child),7historical
deaths,0missing original UUIDs. Source family world remains byte-identical to its
187-file manifest; this copy's state must not merge into it. The default original
four-survivor world was not written. All running original clients remain untouched.
The screenshot was viewed and shows the village overview, not direct harvest proof:
[native view](native-village.png). [Launch](launch.log), [saved census](census.json).

Mean native tick3.626681ms at20TPS/five actors is a small snapshot, not population
or tail-latency capacity evidence. Worlds saved and exact idle Animatica executor
drained normally. Observed wrapper810814/start13304631, daemon810921/start13304780,
game811065/start13305329 are all absent; original six exact identities unchanged.
No active new client/build/worker, automated timer, hold, model/hash/grant or inherited
exception. Pack warnings remain. Full metadata is retained in custody/results.

Next: implement the accepted protected-site/real-material construction boundary
before limited farm expansion or station placement. The current player-bed registry
protects beds; it is not ownership proof for every existing structure. Also retain
parental-care, unreachable-home rest, real production/refill and scale acceptance.
