# Consented birth: first exact-copy attempt

The birth path had not checked its existing courtship agreement: ordinary social
contact with food/needs/housing could bypass the spoken agreement. Current source
requires both actors to retain the same live mutual partner/agreedAt/expiry before
the nursery search, at birth entry and again after spawn callbacks. One-sided,
expired, changed-partner or mismatched-round agreements cannot authorize birth.
Callbacks changing stock/consent or killing a parent discard the uncharged baby
and release its nursery POI. Other costs and native safety requirements remain.

Build57847 exited0:217Java+10Python tests passed, zero failures/errors/skips.
Two added tests cover both sides, time boundaries, different partners and rounds.
[Build](build.log). This is source acceptance, not proof that a birth occurred.

Native attempt13170 exited0 after2400loaded ticks,33105→35505, final35508. It
used an exact168-file copy of the stopped original world, with no initial mind,
inventory, health, position, age, terrain or time/weather setter. Parent selection
used actual readiness: Fern healthy, Dara injured but rested; both share the
four courtship words. Juniper is exhausted with repeated failed rest attempts at
a distant home; Nell is stranded. Neither was treated as ready by assumption.

Explicit fixture inputs:4bread to Fern and8 to Dara through the existing actual
observer handoff, with reach/sight and native inventory capacity. Only the
observer was teleported. No additional shelter/bed was built; the watch records
the existing spare home(-229,71,760), not a reservation or forced birth target.
[Setup](setup.json), [commands](commands.txt), [native family records](family),
[source hashes](source-before.json), [launch](launch.log).

Dara consumed one real bread, healed12→20 through ordinary metabolism, retained
7bread and became HAPPY. Fern retained6bread (original2+supplied4), health20,
HAPPY. Both retained funded readiness in the final snapshot, but moved apart:
Dara near(-235.49,70,727.32), Fern(-231.55,72,762.72). No mutual agreement, birth,
inventory birth charge or newborn occurred.4alive/7dead/0missing unchanged.
Do not claim consent or reproduction success from this run. Its failure motivates
the following visible-company work, not an increase to reproduction probability.

Original world full file manifest remains unchanged; source was unchanged during
the run. Worlds saved and idle Animatica workers drained normally. Observed
wrapper786220/start12994061, daemon786322/start12994204,
game786483/start12994715 all absent; original six exact identities unchanged.
[Results/custody](results.json), [native snapshot](budget-native.json),
[final census](census.json). No active new client/build/timer or coordination hold.
Mean native tick3.850973ms/four actors is not a population benchmark. Pack warnings
remain. Preserve this failed fixture and never merge its history into the original.

The later family-company experiment is separate evidence with different source.
