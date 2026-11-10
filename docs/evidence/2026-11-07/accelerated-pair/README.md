# Accelerated paired visible trials — November7,2026

Version0.2.0 builds with30 passing tests. The same saved generated desert-village
checkpoint was forked into independent adaptive/frozen worlds, configs, minds,
evidence, queues and Gradle build caches. The earlier damaged live trial was
saved and retained separately; no dead resident was restored inside that trial.
The common start is an explicitly named earlier checkpoint with11 original IDs.

Both full Vanilla Perfected clients run visibly. They began at40TPS and promoted
to80 only after two healthy measured windows. Native mean tick costs and actual
wall-clock throughput are in the captured role status JSON files. The maximum
accelerated window is48000 additional game ticks; the controller returns to20TPS
on completion, confirmed cohort extinction, a play request or sustained overload.
This is bounded acceleration, not an unbounded sprint, randomTickSpeed change or
headless server. The native `frozen` field in status means simulation freeze;
`learning_mode` and learning_statistics.mode identify the fixed-policy control.

The adaptive policy counters increased while the frozen files retained their
exact checkpoint values, visits and learningUpdates. Both copied the same starting
world/UUIDs. Bodies, safety, needs and memory continue in the frozen control;
frozen does not mean stationary or zeroed. Adaptive learning is already active
during the20TPS loading warmup; the measured accelerated window starts only after
fixture staging. Compare elapsed game ticks from the documented fork/warmup and
run boundaries, not wall time or unlabelled snapshots. A small single-seed cohort
is exploratory evidence and does not establish a general learning benefit.

Both clients loaded byte-for-byte the same compiled mod classes as the released
JAR; only role/learning/TPS/runtime properties differ. The root build's30 tests
cover checkpoint-frozen semantics, timeouts, pacing, mode validation and existing
cognition/tool/safety/persistence behavior. Pure tests do not certify long-run
survival or provide tool exposure when no gear is present.

`manifest.json` records the measured throughput, current cohort and policy checks.
The two roles use separate native JVMs and save locks, not two windows onto one
simulation. Shared verified assets are read-only copies; mutable state is copied
independently. Current project-owned Java process tuples are in owned-jobs.json;
old Niko hash-only exceptions do not transfer to these new jobs.

## Play now

The playable artifact and checksum are in releases/0.2.0/. `tools/play.sh` opens a
separate adaptive20TPS play copy from the starting checkpoint; it does not silently
import the subsequently learned trial state. To interact with the *currently
training* adaptive villagers and keep their newest minds, use `/villagers play`
in the adaptive window. That ends its accelerated comparison window and returns
it to normal20TPS. The frozen window stays a fixed-policy control unless stopped.

No survival improvement is claimed from speed or update counts alone. Crop/tool
opportunities, route failures, damage, deaths and original-UUID survival remain
acceptance criteria. The trials stay visible and continue to their bounded end.
