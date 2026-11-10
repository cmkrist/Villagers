# First autonomous crop income without a supplied seed

Fern harvested both existing mature beetroot crops, paid for both replants and
retained two real beetroots plus four seeds. The first replant used actual harvest
loot; the second used earned stock. The crops, bag and hoe wear survived restart.
286 Java and 10 Python checks pass. Source 0.4.1-dev remains unreleased.

## Inputs and corrected diagnosis

`runs/harvest-bootstrap/play` copied exactly 270 stopped files from
`runs/earned-station/play`, saved at 54428. That history already contains supplied
fixture crops/tools/food, the ordinary child, native donated meal, earned logs,
paid propagule and table. These are not newly generated baseline resources.
No new seed, food, tool, crop, growth, actor, age, clock, health, fatigue, action
or RNG setter was used. Only read-only watch/status commands were issued. The
parent world stayed unchanged; independent copies' histories were never merged.

The prior grain-only diagnosis was wrong: default harvest search already includes
beetroot. The actual missing input was beetroot seeds, required by the old search
and harvest despite ripe crops being available. The controller is unchanged.
The new payment receipt can reserve a real seed from the actual native loot.
Early observation also confirms Fern's actual jobsite at(-226,72,752).

## Native production

Handle 15542 exited 0: 2400 ticks, 54429→56829, final save 56832. Early observation
at 54525 shows Fern with 12 wheat seeds, one bread, one wheat, zero beetroot/seeds,
food 20 and no harvests. Final bag adds two beetroots and four beetroot seeds.
Counters record two harvests, one harvest-funded and one stock-funded replant.
The existing iron hoe incurred exactly two wear, damage 8→10/250.

The single retained last receipt is the second harvest at 54594,(-230,72,752):
native drops one beetroot and four seeds; prior bag one beetroot/one seed; final
bag two beetroots/four seeds; zero overflow; hoe 9→10; native crop age zero.
The first receipt was overwritten by design and its individual drop list was not
retained. Baseline zero seeds, successful-source counters and the later paid
receipt establish the bootstrap without inventing the missing first event detail.

Raw saved chunks show both previously mature beetroot crops,(-228,72,751) and
(-230,72,752), replanted. Minecraft omits default properties in these saved block
states, so `{"Name":"minecraft:beetroots"}` encodes default age zero; the native
receipt independently records age=0. All eight farmland cells remain. Six wheat
crops stayed immature; their natural growth is permitted, not forced.

Fern's held-nutrition deficit fell 10→8 while food remained 20. This proves two
new edible reserve units, not that he ate the harvested beets. Dara ate one
previously held bread and reached food 20; that is not farm-derived nutrition.
No new birth occurred. `newSelectedParentChildren` is a legacy collector label:
its one child is the existing Fern-589b, born at 36379, not a child of this run.

## Reopen and observation limits

Same-world handle 1537: 400 ticks,56833→57233, final save57237. Fern retained
two beetroots/four seeds, hoe damage10, both default-age replants, food20 and
deficit8. Load-local counters reset to zero and last receipt is empty, with no
extra harvest or repeated payment. One wheat naturally grew age1→2.

The terminal tool response for 1537 was truncated and is no longer recoverable;
the collector's `exitCode:0` is a supplied field, not independent process evidence.
`persistence/launch.log` records normal world shutdown and BUILD SUCCESSFUL;
all three exact owned process identities were terminal at collection. No client
was relaunched to recover the lost response. The primary 15542 exit0 was directly
observed. Evidence verification treats the persistence terminal basis accordingly.

Native five-actor mean tick samples were 2.780779 and 3.876113 ms. These are snapshot
means, not population capacity or tail-latency acceptance. Both cohorts retain
four original survivors plus the existing child, seven old deaths, zero missing.
The nearby original table(-233,72,746) and earned table(-281,75,745) remain.

## Custody and continuation

Exact owned tuples, all terminal:
951632/15254361,951740/15254418,951886/15254918;
955586/15300159,955689/15300217,955802/15300471.
No pending command, owned automated build/release timer, new worker or service
change. Original frozen wrapper601857/10936249 was already absent at both captures;
frozen daemon601990/10936325 and game602372/10937054 remained exact. Historical
adaptive three stayed absent. No lead stop/signal or cause/replacement inferred.
All running user clients/services/worlds are preserved. No resource hold, hash
operation, model grant or inference activation; historical005/006/007 untouched.

`source-main.zip` binds both native cohorts; `verify-results.py` checks recorded
conservation, crops, restart, source pins, parent preservation and deployed0.4.0.
Build logs retain the 286 Java checks and prior 10 Python checks. Full society
goal remains active. Next: natural rematurity→another paid harvest→actual meal/
reserve refill, adequate farm area/irrigation and conserved expansion. Native
wheat/carrot/potato acceptance, sustainable yield, tree regrowth, transport,
general buildings, parent care, defense and population performance remain open.
