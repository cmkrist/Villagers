# Ordinary-family visible check — November 7, 2026

Ordinary birth acceptance is **pending**. The supplied adults did not produce a
child during this bounded visible run. No birth, food charge, nursery reservation,
parentage, child inheritance or positive birth cooldown is claimed.

## Build and isolated fixture

Added development-only `VillageFamilyProbe.java` and its one registration line in
`Villagers.java`. Commands: `villagers familyprobe status`, `feed <uuid> <1..12>`,
and `nursery`. The probe rejects non-play/non-VillageTrial worlds. It supplies at
most two existing adults, requires empty observer hand/native reach/sight, and
uses ordinary item handoff. A nursery requires an entirely clear grounded 5x5x4
volume with no actors, no farmland and no terrain removal; one oak shelter, real
door and matching white bed are supplied. Native POI registration is untouched.
The probe never calls family consideration, sets a mind/age/mood/health/brain,
requests a social action/path, relocates a resident or spawns a resident/child.
A bounded 12000-tick watcher samples only the two supplied adults and would record
adjacent-tick inventories when a real native birth deadline changed. It generated
no birth-transition record in this run.

The first launch attempt (handle30570) failed compile with the fixture's obsolete
`Blocks.WHITE_BED` constant; no game opened. Corrected it from the installed26.3
source to `Blocks.BED.white()`. The normal retry compiled the current parent
family provisioning/navigation changes and this probe successfully. Source hashes
are `source-sha256.json`; loaded runtime hashes are
`loaded-artifact-sha256.json`, unchanged through closure. Parent's156 passing unit
tests preceded this fixture build; the fixture itself received visible compilation
and execution, without a repeated full unit suite. No release was produced.

Before schema7 load the **entire stopped schema6 world** was backed up to
`runs/archives/ordinary-birth-schema6-20261107T080155Z/VillageTrial` (`backup.txt`).
Runtime remained `/home/niko/.internal/source/Villagers/runs/play`, VillageTrial
seed846206, normal difficulty, adaptive20TPS. This inherits earlier social, harvest
and crafting interventions; it is not an untouched survival comparison.

## What happened

Visible world observation ran approximately08:05–08:13UTC, from reported game
16239 to25669 (9430 ticks, approximately7m52s). The only time intervention was
`time set day` at initial setup. Camera teleports moved only the creative observer.

Selected existing adults:

- Fern-5ad3: `5ad3aad5-3e4b-4eb6-8158-87731868dc03`.
- Nell-e358: `e358e923-f9d2-4d5b-a04b-b2531c8f02d9`.

Each received2 real operator bread initially and6 more as reserves, through four
successful handoff batches. Several preceding out-of-sight/range attempts refused
before creating supplies. The recorded fixture cost is16 supplied bread total.
Fern naturally ate food and shared food with hungry Juniper; the stock decrease
is not a reproduction charge. Final stocks were3 bread for Fern and7 for Nell,
with food20/HAPPY/native age0 for both. Native inventories, episodes and receipts
are retained in `family-reports`, `report-final.json` and `commands.jsonl`.

Before supplies/bed, physical snapshots showed insufficient breeding food and
no spare native HOME POIs near the selected adults. No qualifying ready encounter
was captured during that control phase. Thus no-food/no-bed **behavioral rejection
controls remain unvalidated**, despite the genuine initial scarcity.

An initial nursery search from an unsuitable elevated observer origin refused
without changing blocks. The second search placed the labelled shelter with bed
head **-242,72,759**, foot -242,72,760, north-facing. The first same-tick snapshot
had no available POI yet; later snapshots recorded the native HOME POI available.
The final white bed remained complete/unoccupied and its POI was still available.
No fixture code reserved a ticket or assigned a home. Path reachability has not
been tested by an ordinary birth attempt.

Both selected adults had adequate real reserve food, health, mood and fatigue
for much of the run, but their autonomous movement frequently separated them.
Fern repeatedly chose high-regard, hungry Juniper instead. Some other unsupplied
pairs reached `not ready for a child`, establishing the family hook executed.
Fern eventually recorded that same rejection; Nell retained `no recent family
encounter`. Their supplied mutual pair did not reach an observed eligibility or
nursery search. The observer was moved overhead to avoid Nell's inherited prior
harm/avoidance history affecting the intended later contact; that earlier history
was preserved rather than reset. No fertility/path failure is inferred from the
absence of a birth.

Three actual screenshots were inspected with view_image:
`family-nursery-visible` shows naturally moving adults (captured the earlier view,
so its label is not proof of the nursery), `family-nursery-overview` shows the new
oak shelter amid the natural village, and `family-final-bed` shows the real white
bed inside its roofed room. PNG metadata and native reports carry the conditions;
a screenshot cannot establish a birth or inventory transaction.

## Definite results and limits

All11 original residents remained alive in final ground-truth census. All11 minds
migrated schema6→7, retained UUID/name/home/personality, and saved nondecreasing
learning counts. All saved birth deadlines remained0; no ordinary-child lineage
or birth-transition record appeared. This establishes schema7 save continuity
from schema6, **not a schema7 reopen/child persistence test**. No reopen was done
solely to repeat the unproven birth; any later launch requires fresh resource
coordination.

The native tick query at08:11:40UTC reported20TPS, average3.0ms,
P95=4.0ms/P99=5.7ms over100ticks. This is a short observation, not a population
scaling result. The known resourcepack-symlink limitations remain; no full pack
parity claim is added.

Remaining acceptance: a qualifying willing mutual pair, genuine no-food/no-bed
encounter controls, nursery path/ticket and native child HOME, exact three-bread
(or12-unit) deductions per parent at birth, one ordinary baby, exact two-parent
lineage/genes/words, native and persisted positive parent cooldowns, and clean
child save/reopen continuity. Supplied materials do not demonstrate autonomous
food reserves, bed construction, childcare or natural population growth.

Next bounded source task: grounded partner intent/coordination from actual
encounters, without private-mind omniscience or weakened safety/scarcity checks.
Then revisit this already-supplied isolated save and test ordinary birth rather
than adding forced encounter/birth calls to the fixture.

## Process closure and resource readiness

Retry exec handle96153 ended exit0 after consumed `quit` and world/chunk saving.
Shell699596→wrapper699607→daemon699718→game699878 are all absent; no queued command
remains and the play launcher lock is free. `owned-launch-2-supplement.json` has
UID/startTicks/exe/cwd/cgroup; `terminal-custody.json` records terminal state.
All eight Java processes observed before launch retained exact start ticks,
executable and working directory, including original Modrinth516527, historical
six adaptive/frozen processes and Buddy replacement693634. Older Buddy679447 was
already absent at preflight; no custody/exception was transferred to its replacement.

Parent acknowledged changed-readiness before each launch and received exact
live/terminal tuples. No new worker, model/release job or automated timer was
created. No additional JVM/build/reopen is needed for this check. Future mutually
agreed no-new-job intervals can coexist with the preserved original clients;
this checkpoint does not itself authorize or activate any interval/hash operation.
