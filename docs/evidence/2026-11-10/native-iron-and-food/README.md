# Native mineral progression and camp food limits

The dragon objective is incomplete. No iron ingots, iron pickaxe, Nether entry,
End entry or dragon victory has been verified here.

## Clay and natural rock

`runs/native-clay-prospect-v1` crossed the actual clay layer in the saved
adventurer's quarry. Clover retained four clay balls produced by native drops.
The quarry then stopped at andesite. The copy was saved and stopped explicitly
at6310ticks to test that newly identified handoff barrier separately; exit0 and
the wrapper's primary hash comparison passed.

Quarry starts still require observed dry soil. Clay is accepted only inside the
existing finite staircase. Native drop capacity is checked before removal;
making bag space uses an existing owned-table placement or actual item drop.
Andesite, diorite, granite and tuff now join stone/deepslate as supported mining
substrate. Reach, support, entity, fluid, construction and tool checks remain.
Masonry is excluded from the new substrate whitelist.

`runs/native-rock-prospect-v2` ran24000ticks from202345 to226345 using the current
primary as its source. All eight tracked grassland villagers ended20HP. Clover
reachedY55, mined actual andesite with native drops and pickaxe durability,
and retained23 exit floors. Clover ended20food and zero iron ingots. The saved
copy and primary are distinct; the wrapper verified primary hashes unchanged.
The tested production JAR hash matched the current build before the guarded
primary continuation was launched.

Later trace samples repeatedly traverse open passage steps. Investigate avoiding
already surveyed supported floors when selecting a fresh mining direction;
that is a hypothesis for further progress, not a verified fix.

## Camp food

Delivery queues now temporarily defer a blocked caller after a bounded attempt.
Repeated speech refreshes request validity without resetting the attempt's
deadline. Other actually heard callers can be served during that delay.

`runs/native-camp-meal-fairness-v7` ended at17680ticks with Oak-6432 at the4HP
guard. It is NOT a successful sustained-care result. Both founders and their
existing copied child remained alive, but food replenishment still failed to
keep all captives fed. Camp passage/meal flags remain off in the primary.

One next diagnostic: remembered hay locations refer to previously harvested
blocks. The current revisit check requires a block hit on the remembered cell;
an empty previously harvested cell may never satisfy that sight test. Verify
that hypothesis with actual navigation and sight evidence before changing it.

## Checks and bindings

478Java tests and14Python checks passed, followed by a successful build.
The initial clay test omitted Minecraft registry bootstrap; that test setup was
corrected before the successful full suite. Native runtime artifact hashes are
in each run's `artifact-pins.json`; exits are in `terminal.json`.

Concise retained data: [clay, rock and meal checks](clay-rock-and-meal-retry-checks.json).
Primary continuation evidence is in `runs/native-primary-rock-prospect-v1`.
It ended normally after3000 native20TPS ticks at205346. The saved-body audit
retained all179 original humanoid identities, with all33 managed villagers20HP;
the original authored save hashes remained unchanged. Clover retains4clay balls,
33dirt and the actual stone pickaxe at30damage, with a native andesite receipt.
No iron ingots or iron pickaxe were acquired. See
[primary saved result and native receipt](PRIMARY-CLAY-AND-ANDESITE.json).
